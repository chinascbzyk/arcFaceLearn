package com.md.fd;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber;
import com.googlecode.javacv.OpenCVFrameGrabber;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_highgui;
import com.md.common.*;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_highgui.*;

public class AFDTestForFile {
    public static final String APPID = "wVv9yo7uxziSN5itsNVeLZpySZAiAihAPTuAYA4tTLt";
    public static final String FD_SDKKEY = "2suUcZ8NcNKt354cPvwR5ivBYWJE1Apd5kkJaFaGJQjd";

    public static final int FD_WORKBUF_SIZE = 20 * 1024 * 1024;
    public static final int MAX_FACE_NUM = 50;

    public static FaceInfo[] faceDetection(BufferedImage bufferedImage, Pointer hFDEngine) {
        // load Image Data
        ASVLOFFSCREEN inputImg = loadImage(bufferedImage);
        // do Face Detect
        FaceInfo[] faceInfos = doFaceDetection(hFDEngine, inputImg);
        return faceInfos;
    }

    public static FaceInfo[] doFaceDetection(Pointer hFDEngine, ASVLOFFSCREEN inputImg) {
        FaceInfo[] faceInfo = new FaceInfo[0];

        PointerByReference ppFaceRes = new PointerByReference();
        NativeLong ret = AFD_FSDKLibrary.INSTANCE.AFD_FSDK_StillImageFaceDetection(hFDEngine, inputImg, ppFaceRes);
        if (ret.longValue() != 0) {
//            System.out.println(String.format("AFD_FSDK_StillImageFaceDetection ret 0x%x", ret.longValue()));
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
            }
        }
        return faceInfo;
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

    public static void main(String[] args) throws FrameGrabber.Exception, IOException {
        initialPortrait();
    }

    public static void initialPortrait() throws IOException {
        // init Engine
        Pointer pFDWorkMem = CLibrary.INSTANCE.malloc(FD_WORKBUF_SIZE);

        PointerByReference phFDEngine = new PointerByReference();
        NativeLong ret = AFD_FSDKLibrary.INSTANCE.AFD_FSDK_InitialFaceEngine(APPID, FD_SDKKEY, pFDWorkMem, FD_WORKBUF_SIZE, phFDEngine, _AFD_FSDK_OrientPriority.AFD_FSDK_OPF_0_HIGHER_EXT, 32, MAX_FACE_NUM);
        if (ret.longValue() != 0) {
            CLibrary.INSTANCE.free(pFDWorkMem);
            System.exit(0);
        }

        // print FDEngine version
        Pointer hFDEngine = phFDEngine.getValue();

        opencv_highgui.CvCapture cvCapture = cvCreateFileCapture("/home/yk/my_video-3.mkv");
        //创建视频输出帧的图像大小、位深度、通道数
        opencv_core.IplImage bgrImage;
        int x, y, w, h;
        int captureWidth = 600;
        int captureHeight = 800;

        Graphics2D bGraphics;
        CanvasFrame canvas = new CanvasFrame("Camera", 0);
        canvas.setCanvasSize(captureWidth, captureHeight);
        canvas.setBounds(0, 0, captureWidth, captureHeight);
        //视频界面持续显示画面
        while (canvas.isVisible() && (bgrImage = cvQueryFrame(cvCapture)) != null) {
            BufferedImage bImage = bgrImage.getBufferedImage();
            bGraphics = bImage.createGraphics();
            FaceInfo[] faceInfos = faceDetection(bImage, hFDEngine);
            bGraphics.drawImage(bImage, 0, 0, bImage.getWidth(), bImage.getHeight(), null);
            if (faceInfos.length != 0) {
                ImageIO.write(bImage,"jpg",new File("/home/yk/" + System.currentTimeMillis() + ".jpg"));
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
        // release Engine
        AFD_FSDKLibrary.INSTANCE.AFD_FSDK_UninitialFaceEngine(hFDEngine);
        CLibrary.INSTANCE.free(pFDWorkMem);
    }

}
