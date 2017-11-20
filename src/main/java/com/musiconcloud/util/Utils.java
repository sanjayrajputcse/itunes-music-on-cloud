package com.musiconcloud.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

/**
 * Created by sanjay.rajput on 02/10/17.
 */
public class Utils {

    private static final List<String> mp3Formats = Arrays.asList(".mp3", ".wav", ".m4a");

    public static final DecimalFormat df = new DecimalFormat(".##");

    public static String getFileSize(long length) {
        StringBuilder size = new StringBuilder();
        if (length > 1024) {    //kb
            double s = length / 1024.0;
            if (s > 1024) {     //mb
                s = s / 1024.0;
                if (s > 1024) { //gb
                    s = s / 1024.0;
                    size.append(df.format(s)).append(" GB");
                } else {
                    size.append(df.format(s)).append(" MB");
                }
            } else {
                size.append(df.format(s)).append(" KB");
            }
        }
        return size.toString();
    }

    public static String smartTrim(String str) {
        str = str.trim();
        if (str.startsWith(","))
            str = str.replaceFirst("," ,"").trim();
        if (str.endsWith(","))
            str = str.substring(0, str.length() - 1).trim();
        return str;
    }

    public static boolean isAudioFile(String fileExtension) {
        return fileExtension != null && mp3Formats.contains(fileExtension.toLowerCase());
    }

    public static boolean isFileExist(String filePath) {
        return new File(filePath).exists();
    }

    public static String executeCommand(String[] command) {
        System.out.println("executing command: " + Arrays.asList(command));
        StringBuffer output = new StringBuffer();
        StringBuffer error = new StringBuffer();
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader readerOut = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader readerError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String line = "";
            while ((line = readerOut.readLine())!= null) {
                output.append(line + "\n");
            }
            line = "";
            while ((line = readerError.readLine())!= null) {
                error.append(line + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("output: " + output.toString());
        System.out.println("error: " + error.toString());
        return output.toString();

    }
}
