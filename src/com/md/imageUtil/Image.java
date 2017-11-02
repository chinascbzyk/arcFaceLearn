package com.md.imageUtil;


import com.md.common.FaceInfo;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Image extends JFrame {
    private FaceInfo[] list;
    private String path;
    MyPanel mp = null;

    public void start() {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mp = new MyPanel();
        mp.setList(list);
        mp.setPath(path);
        this.setSize(image.getWidth(), image.getHeight());
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setVisible(true);
        this.add(mp);
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setList(FaceInfo[] list) {
        this.list = list;
    }
}


class MyPanel extends JPanel {
    private String path;
    private FaceInfo[] list;
    private int x, y, w, h;
    BufferedImage image = null;

    public void paint(Graphics g) {
        try {
            image = ImageIO.read(new File(path));
            g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
            g.setColor(Color.GREEN);  //设置画笔颜色
            if (list.length != 0) {
                for (FaceInfo faceInfo : list) {
                    x = faceInfo.left;
                    y = faceInfo.top;
                    w = faceInfo.right - faceInfo.left;
                    h = faceInfo.bottom - faceInfo.top;
//                    System.out.println(x);
//                    System.out.println(y);
//                    System.out.println(w);
//                    System.out.println(h);
//                    System.out.println();
                    g.drawRect(x, y, w, h);
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setList(FaceInfo[] list) {
        this.list = list;
    }
}
