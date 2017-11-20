package com.musiconcloud.models;

import com.musiconcloud.util.Utils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author sanjay.rajput on 20 Nov 2017 7:50 PM
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SongMeta {

    private String name;
    private String artist;
    private String album;
    private String size;
    private String fileName;
    private String filePath;
    private String time;
    private String rating;
    private String genre;
    private String year;
    private String kind;
    private String fileExtension;

    @Override
    public String toString() {
        return "name='" + name + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", size='" + Utils.getFileSize(Long.parseLong(size)) + '\'' +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", time='" + time + '\'' +
                ", rating='" + rating + '\'' +
                ", genre='" + genre + '\'' +
                ", year='" + year + '\'' +
                ", kind='" + kind + '\'';
    }

    public boolean isValid() {
        if (filePath.contains("missing value")){
            return false;
        }
        return isAudioFile();
    }

    public boolean isAudioFile() {
        return Utils.isAudioFile(fileExtension);
    }

    public long getSizeInLong() {
        return Long.parseLong(size);
    }
}
