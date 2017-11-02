package com.md.imageUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class yuvUtil {
    public static void main(String[] args) {
        String path = "/home/yk/Pictures";
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        File[] files = null;
        if (file.isFile()) {
            files = new File[]{file};
        } else {
            files = file.listFiles();
        }

//        String filePath = file.getParent();
        for (File fil : files) {
            if (fil.getName().endsWith(".png")) {
                String cmd = "ffmpeg -i %s -s 640x480 -pix_fmt yuv420p %s.yuv";
                cmd = String.format(cmd, fil, path + File.separator + fil.getName().substring(0, fil.getName().lastIndexOf(".")));
                System.out.println(cmd);
                exeCMD(cmd);
            }

        }

        return;
    }

    /**
     * 本地执行 sh 命令
     *
     * @param cmd shell命令
     * @return 终端内容
     */
    public static StringBuffer exeCMD(String cmd) {
        if (cmd == null || cmd.equals("")) return null;
        BufferedReader br = null;
        InputStreamReader inputStreamReader = null;
        InputStream inputStream = null;
        Process p = null;
        try {
            Runtime runtime = Runtime.getRuntime();

            String[] cmds = {"/bin/bash", "-c", cmd};
            p = runtime.exec(cmds);
            inputStream = p.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            br = new BufferedReader(inputStreamReader);

            StringBuffer buffer = new StringBuffer();
            String readLine;
            while ((readLine = br.readLine()) != null) {
//				System.out.println(readLine);
                buffer.append(readLine + "\n");
            }
            return buffer;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) br.close();
                if (inputStreamReader != null) inputStreamReader.close();
                if (inputStream != null) inputStream.close();
                if (p != null) p.destroy();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
