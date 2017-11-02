package com.md.test;

import java.io.FileOutputStream;
import java.io.IOException;

public class ImageUtilsExample {
    public static void main(String[] args) {
        try {
            ImageLoader.BufferInfo bufferInfo = ImageLoader.getI420FromFile("222.png");
            if (bufferInfo.base != null) {
                FileOutputStream fos = new FileOutputStream(String.format("%dx%d_I420_222.yuv", bufferInfo.width, bufferInfo.height));
                fos.write(bufferInfo.base);
                fos.close();
            }

            bufferInfo = ImageLoader.getI420FromFile("xxx.bmp");
            if (bufferInfo.base != null) {
                FileOutputStream fos = new FileOutputStream(String.format("%dx%d_I420_xxx.yuv", bufferInfo.width, bufferInfo.height));
                fos.write(bufferInfo.base);
                fos.close();
            }
            bufferInfo = ImageLoader.getI420FromFile("003.jpg");
            if (bufferInfo.base != null) {
                FileOutputStream fos = new FileOutputStream(String.format("%dx%d_I420_003.yuv", bufferInfo.width, bufferInfo.height));
                fos.write(bufferInfo.base);
                fos.close();
            }
            bufferInfo = ImageLoader.getI420FromFile("_R43.png");
            if (bufferInfo.base != null) {
                FileOutputStream fos = new FileOutputStream(String.format("%dx%d_I420_R43.yuv", bufferInfo.width, bufferInfo.height));
                fos.write(bufferInfo.base);
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
