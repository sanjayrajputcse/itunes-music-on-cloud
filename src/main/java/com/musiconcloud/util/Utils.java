package com.musiconcloud.util;

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
}
