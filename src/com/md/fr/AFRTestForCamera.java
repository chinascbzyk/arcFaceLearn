package com.md.fr;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core;
import com.md.common.*;
import com.md.imageUtil.Image;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;
import com.sun.jna.ptr.PointerByReference;

public class AFRTestForCamera {
    public static final String APPID = "wVv9yo7uxziSN5itsNVeLZpySZAiAihAPTuAYA4tTLt";
    public static final String FD_SDKKEY = "2suUcZ8NcNKt354cPvwR5ivBYWJE1Apd5kkJaFaGJQjd";
    public static final String FR_SDKKEY = "2suUcZ8NcNKt354cPvwR5ivZ2i5jx9tas8PqnKM6ZGFz";

    public static final int FD_WORKBUF_SIZE = 200 * 1024 * 1024;
    public static final int FR_WORKBUF_SIZE = 400 * 1024 * 1024;
    public static final int MAX_FACE_NUM = 50;

    private static AFR_FSDK_FACEMODEL faceFeatureSource;

    public static FaceInfo[] faceReference(BufferedImage bufferedImage, Pointer hFDEngine, Pointer hFREngine) {
        // load Image Data
        ASVLOFFSCREEN inputImg;
        inputImg = loadImage(bufferedImage);
        return compareFaceSimilarity(hFDEngine, hFREngine, inputImg);
    }

    public static FaceInfo[] doFaceDetection(Pointer hFDEngine, ASVLOFFSCREEN inputImg) {
        FaceInfo[] faceInfo = new FaceInfo[0];

        PointerByReference ppFaceRes = new PointerByReference();
        NativeLong ret = AFD_FSDKLibrary.INSTANCE.AFD_FSDK_StillImageFaceDetection(hFDEngine, inputImg, ppFaceRes);
        if (ret.longValue() != 0) {
            System.out.println(String.format("AFD_FSDK_StillImageFaceDetection ret 0x%x", ret.longValue()));
            return faceInfo;
        }

        AFD_FSDK_FACERES faceRes = new AFD_FSDK_FACERES(ppFaceRes.getValue());
        if (faceRes.nFace > 0) {
            faceInfo = new FaceInfo[faceRes.nFace];
            for (int i = 0; i < faceRes.nFace; i++) {
                MRECT rect = new MRECT(new Pointer(Pointer.nativeValue(faceRes.rcFace.getPointer()) + faceRes.rcFace.size() * i));
                int orient = faceRes.lfaceOrient.getPointer().getInt(i * 4);
                faceInfo[i] = new FaceInfo();

                faceInfo[i].left = rect.left;
                faceInfo[i].top = rect.top;
                faceInfo[i].right = rect.right;
                faceInfo[i].bottom = rect.bottom;
                faceInfo[i].orient = orient;

//                System.out.println(String.format("%d (%d %d %d %d) orient %d", i, rect.left, rect.top, rect.right, rect.bottom, orient));
            }
        }
        return faceInfo;
    }

    public static AFR_FSDK_FACEMODEL extractFRFeature(Pointer hFREngine, ASVLOFFSCREEN inputImg, FaceInfo faceInfo) {

        AFR_FSDK_FACEINPUT faceinput = new AFR_FSDK_FACEINPUT();
        faceinput.lOrient = faceInfo.orient;
        faceinput.rcFace.left = faceInfo.left;
        faceinput.rcFace.top = faceInfo.top;
        faceinput.rcFace.right = faceInfo.right;
        faceinput.rcFace.bottom = faceInfo.bottom;

        AFR_FSDK_FACEMODEL faceFeature = new AFR_FSDK_FACEMODEL();
        NativeLong ret = AFR_FSDKLibrary.INSTANCE.AFR_FSDK_ExtractFRFeature(hFREngine, inputImg, faceinput, faceFeature);
        if (ret.longValue() != 0) {
            System.out.println(String.format("AFR_FSDK_ExtractFRFeature ret 0x%x", ret.longValue()));
            return null;
        }

        try {
            return faceFeature.deepCopy();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static FaceInfo[] compareFaceSimilarity(Pointer hFDEngine, Pointer hFREngine, ASVLOFFSCREEN inputImg) {
        // Do Face Detect
        FaceInfo[] faceInfosB = doFaceDetection(hFDEngine, inputImg);
        if (faceInfosB.length < 1) {
//            System.out.println("no face in Image B ");
            return null;
        }

        float rs = 0;
        float threshold = 0.5f;

        for (FaceInfo faceInfoB : faceInfosB) {
            AFR_FSDK_FACEMODEL faceFeatureB = extractFRFeature(hFREngine, inputImg, faceInfoB);
            if (faceFeatureB == null) {
                System.out.println("extract face feature in Image B failed");
//                faceFeatureSource.freeUnmanaged();
                return null;
            }

            // calc similarity between faceA and faceB
            FloatByReference fSimilScore = new FloatByReference(0.0f);
            NativeLong ret = AFR_FSDKLibrary.INSTANCE.AFR_FSDK_FacePairMatching(hFREngine, faceFeatureSource, faceFeatureB, fSimilScore);
//            faceFeatureSource.freeUnmanaged();
            faceFeatureB.freeUnmanaged();
            if (ret.longValue() != 0) {
                System.out.println(String.format("AFR_FSDK_FacePairMatching failed:ret 0x%x", ret.longValue()));
                return null;
            }
            rs = fSimilScore.getValue();
            if (rs >= threshold) {
                System.out.println(String.format("similarity between faceA and faceB is %f", rs));
                FaceInfo[] faceInfos = {faceInfoB};
                return faceInfos;
            }
        }
        return null;
    }

    public static ASVLOFFSCREEN loadImage(String filePath) {
        ASVLOFFSCREEN inputImg = new ASVLOFFSCREEN();
        BufferInfo bufferInfo = ImageLoader.getBGRFromFile(filePath);
        inputImg.u32PixelArrayFormat = ASVL_COLOR_FORMAT.ASVL_PAF_RGB24_B8G8R8;
        inputImg.i32Width = bufferInfo.width;
        inputImg.i32Height = bufferInfo.height;
        inputImg.pi32Pitch[0] = inputImg.i32Width * 3;
        inputImg.ppu8Plane[0] = new Memory(inputImg.pi32Pitch[0] * inputImg.i32Height);
        inputImg.ppu8Plane[0].write(0, bufferInfo.buffer, 0, inputImg.pi32Pitch[0] * inputImg.i32Height);
        inputImg.ppu8Plane[1] = Pointer.NULL;
        inputImg.ppu8Plane[2] = Pointer.NULL;
        inputImg.ppu8Plane[3] = Pointer.NULL;

        inputImg.setAutoRead(false);
        return inputImg;
    }

    public static ASVLOFFSCREEN loadImage(BufferedImage bufferedImage) {
        ASVLOFFSCREEN inputImg = new ASVLOFFSCREEN();

        BufferInfo bufferInfo = ImageLoader.getBGRFromFile(bufferedImage);
        inputImg.u32PixelArrayFormat = ASVL_COLOR_FORMAT.ASVL_PAF_RGB24_B8G8R8;
        inputImg.i32Width = bufferInfo.width;
        inputImg.i32Height = bufferInfo.height;
        inputImg.pi32Pitch[0] = inputImg.i32Width * 3;
        inputImg.ppu8Plane[0] = new Memory(inputImg.pi32Pitch[0] * inputImg.i32Height);
        inputImg.ppu8Plane[0].write(0, bufferInfo.buffer, 0, inputImg.pi32Pitch[0] * inputImg.i32Height);
        inputImg.ppu8Plane[1] = Pointer.NULL;
        inputImg.ppu8Plane[2] = Pointer.NULL;
        inputImg.ppu8Plane[3] = Pointer.NULL;

        inputImg.setAutoRead(false);
        return inputImg;
    }

    public static void main(String[] args) throws FrameGrabber.Exception {
        String sourceImgPath = "/home/yk/Pictures/ssz.jpg";
        initialPortrait(sourceImgPath);
    }

    public static void initialPortrait(String sourceImgPath) {
        // init Engine
        Pointer pFDWorkMem = CLibrary.INSTANCE.malloc(FD_WORKBUF_SIZE);
        Pointer pFRWorkMem = CLibrary.INSTANCE.malloc(FR_WORKBUF_SIZE);

        PointerByReference phFDEngine = new PointerByReference();
        NativeLong ret = AFD_FSDKLibrary.INSTANCE.AFD_FSDK_InitialFaceEngine(APPID, FD_SDKKEY, pFDWorkMem, FD_WORKBUF_SIZE, phFDEngine, _AFD_FSDK_OrientPriority.AFD_FSDK_OPF_0_HIGHER_EXT, 32, MAX_FACE_NUM);
        if (ret.longValue() != 0) {
            CLibrary.INSTANCE.free(pFDWorkMem);
            CLibrary.INSTANCE.free(pFRWorkMem);
            System.out.println(String.format("AFD_FSDK_InitialFaceEngine ret 0x%x", ret.longValue()));
            System.exit(0);
        }

        // print FDEngine version
        Pointer hFDEngine = phFDEngine.getValue();

        PointerByReference phFREngine = new PointerByReference();
        ret = AFR_FSDKLibrary.INSTANCE.AFR_FSDK_InitialEngine(APPID, FR_SDKKEY, pFRWorkMem, FR_WORKBUF_SIZE, phFREngine);
        if (ret.longValue() != 0) {
            AFD_FSDKLibrary.INSTANCE.AFD_FSDK_UninitialFaceEngine(hFDEngine);
            CLibrary.INSTANCE.free(pFDWorkMem);
            CLibrary.INSTANCE.free(pFRWorkMem);
            System.out.println(String.format("AFR_FSDK_InitialEngine ret 0x%x", ret.longValue()));
            System.exit(0);
        }

        // print FREngine version
        Pointer hFREngine = phFREngine.getValue();

        //init sourceImg
        ASVLOFFSCREEN inputSource = loadImage(sourceImgPath);

        // Do Face Detect
        FaceInfo[] faceInfosA = doFaceDetection(hFDEngine, inputSource);
        if (faceInfosA.length < 1) {
            System.out.println("no face in Image A ");
            return;
        }
        // Extract Face Feature
        faceFeatureSource = extractFRFeature(hFREngine, inputSource, faceInfosA[0]);

        int x, y, w, h;
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(0); //打开USB摄像头
        try {
            grabber.start();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        opencv_core.IplImage image = null; // 将所获取摄像头数据放入IplImage
        try {
            image = grabber.grab();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        int captureWidth = image.width();
        int captureHeight = image.height();
        grabber.setImageWidth(captureWidth);
        grabber.setImageHeight(captureHeight);
        BufferedImage bImage = image.getBufferedImage();

        Graphics2D bGraphics = bImage.createGraphics();
        CanvasFrame canvas = new CanvasFrame("Camera", 0);
        canvas.setCanvasSize(captureWidth, captureHeight);
        canvas.setBounds(0, 0, captureWidth, captureHeight);
        //视频界面持续显示画面
        try {
            int num = 0;
            long t1 = System.currentTimeMillis();
            System.out.println("start time:" + t1);
            while (canvas.isVisible() && (image = grabber.grab()) != null) {
                num++;
                bImage = image.getBufferedImage();
                bGraphics = bImage.createGraphics();
                FaceInfo[] faceInfos = faceReference(bImage, hFDEngine, hFREngine);
                bGraphics.drawImage(bImage, 0, 0, bImage.getWidth(), bImage.getHeight(), null);
                if (faceInfos != null && faceInfos.length != 0) {
                    bGraphics.setColor(Color.GREEN);  //设置画笔颜色
                    for (FaceInfo faceInfo : faceInfos) {
                        x = faceInfo.left;
                        y = faceInfo.top;
                        w = faceInfo.right - faceInfo.left;
                        h = faceInfo.bottom - faceInfo.top;
                        bGraphics.drawRect(x, y, w, h);
                    }
                    bGraphics.dispose();
                }
                canvas.showImage(bImage); //显示缓存图片
            }
            long t2 = System.currentTimeMillis();
            System.out.println("end time:" + t2);
            System.out.println("time:" + (t2 - t1) / 1000);
            System.out.println("num:" + num);
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        } finally {
            // release Engine
            AFD_FSDKLibrary.INSTANCE.AFD_FSDK_UninitialFaceEngine(hFDEngine);
            AFR_FSDKLibrary.INSTANCE.AFR_FSDK_UninitialEngine(hFREngine);

            CLibrary.INSTANCE.free(pFDWorkMem);
            CLibrary.INSTANCE.free(pFRWorkMem);
        }
    }

}
