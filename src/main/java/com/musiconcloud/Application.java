package com.musiconcloud;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.musiconcloud.models.SongMeta;
import com.musiconcloud.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * @author sanjay.rajput on 04 Aug 2017 1:13 AM
 */
public class Application {

    private static final Logger logger = Logger.getLogger("Application");

    private static final String FIELD_DELIMITER = "&FIELD_DELIMETER&";
    private static final String ROW_DELIMITER = "&ROW_DELIMETER&";
    private static final String myMusicFolderID = "0B9mh_-WKGdSYMjloQVBlMUVmM00";

    private static Drive service;
    private static List<String> allMusicFiles = new ArrayList<>();

    /** Application name. */
    private static final String APPLICATION_NAME = "Drive API Java Quickstart";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".credentials/google-drive-java-full");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/drive-java-quickstart
     */
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = Application.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("mac-flipkart");
//        System.out.println("Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Drive client service.
     * @return an authorized Drive client service
     * @throws IOException
     */
    public static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            logger.log(Level.SEVERE, "NOT ENOUGH PARAMETERS");
            System.exit(-1);
        }
        service = getDriveService();
        int totalCloudFiles = cacheAllMusicFiles();

        String songDetailFilePath = args[0];
        final long[] count = {Long.parseLong(args[1]), 0, 0, Long.parseLong(args[2]), totalCloudFiles}; //localSongsCount, uploaded, failed, ignored, cloudSongsCount

        if (songDetailFilePath != null && !songDetailFilePath.isEmpty()) {
            List<SongMeta> songInfoList = readSongsFromFile(songDetailFilePath);
            songInfoList.forEach(song -> {
                if (song.isValid()) {
                    try {
                        if (!isFileExist(song.getFileName())) {
//                            System.out.println("fileName: " + song.getFileName() + " , path: " + song.getFilePath() + " , size: " + song.getSize());
                            upload(song, "audio/mpeg");
                            count[1]++;
                            count[4]++;
                        }
                    } catch (IOException e) {
                        logger.log(Level.INFO, "failed to upload\nSong Info: " + song, e);
//                    System.out.println(e.getStackTrace());
                        count[2]++;
                    }
                } else {
                    System.out.println("IGNORED << " + song.toString());
                    count[2]++;
                }
            });
        }
        printSummary(count);
//        printFiles();
    }

    private static void printSummary(long[] count) {
        System.out.println("\t\t========================= EXPORT SUMMARY ========================");
        System.out.println("\t\t\t\t\t total    : " + count[0]);
        System.out.println("\t\t\t\t\t uploaded : " + count[1]);
        System.out.println("\t\t\t\t\t failed   : " + count[2]);
        System.out.println("\t\t\t\t\t ignored  : " + count[3]);
        System.out.println("\t\t\t\t cloud songs count(gDrive): " + count[4]);
        System.out.println("\t\t=================================================================");
    }

    public static void upload(SongMeta song, String contentType) throws IOException {
        System.out.println(song.toString());
        File fileMetadata = new File();
        fileMetadata.setName(song.getFileName());
        fileMetadata.setParents(Collections.singletonList(myMusicFolderID));
        java.io.File file = new java.io.File(song.getFilePath());
        if (!file.exists()) {
            logger.log(Level.INFO, "file does not exist");
        }
        FileContent mediaContent = new FileContent(contentType, file);
        long tStart = new Date().getTime();
        File gFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id, parents")
                .execute();
        long tEnd = new Date().getTime();
        allMusicFiles.add(song.getFileName());
        long duration = (tEnd - tStart)/1000;
        System.out.println("uploaded " + gFile.getId() + " in " + duration + " sec at " + Utils.df.format(song.getSizeInLong() * 1.0/1024/1024/duration) + " mbps");
    }

    public static boolean isFileExist(String fileToSearch) throws IOException {
//        System.out.println("checking for file: " + fileToSearch);
        if (!allMusicFiles.isEmpty()) {
//            System.out.println("Total File Count: " + allMusicFiles.size());
            for (String file : allMusicFiles) {
                if (file != null && fileToSearch != null) {
                    if (file.equalsIgnoreCase(fileToSearch)) {
//                        System.out.println("file already exist");
                        return true;
                    }
                } else {
                    System.out.println("file: " + file + ", fileToSearch: " + fileToSearch);
                }
            }
        }
        return false;
    }

    public static int cacheAllMusicFiles() throws IOException {
        String pageToken = null;
        List<File> files = new ArrayList<>();
        do {
            FileList result = service.files().list()
                    .setSpaces("drive")
                    .setPageSize(1000)
                    .setPageToken(pageToken)
                    .setQ("'" + myMusicFolderID + "' in parents")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            files = result.getFiles();
            if (files != null && files.size() > 0) {
                System.out.println("total files in cloud: " + files.size());
                files.forEach(x -> allMusicFiles.add(x.getName()));
            }
            pageToken = result.getNextPageToken();
//            System.out.println("Token: " + pageToken);
        } while (pageToken != null);
        return files.size();
    }

    public static List<SongMeta> readSongsFromFile(String filePath) throws IOException {
        List<SongMeta> songs = new ArrayList<>();
        try (Stream<String> stream = Files.lines(Paths.get(filePath))) {
            stream.forEach(song -> {
                try {
                    List<String> songFields = Arrays.asList(song.split(FIELD_DELIMITER));
                    int fields = songFields.size();
                    String fileName = Utils.smartTrim(songFields.get(0));
                    String fileLocation = Utils.smartTrim(songFields.get(1));
                    String name = Utils.smartTrim(songFields.get(2));
                    String artist = Utils.smartTrim(songFields.get(3));
                    String album = Utils.smartTrim(songFields.get(4));
                    String time = Utils.smartTrim(songFields.get(5));
                    String rating = Utils.smartTrim(songFields.get(6));
                    String genre = Utils.smartTrim(songFields.get(7));
                    String year = Utils.smartTrim(songFields.get(8));
                    String size = Utils.smartTrim(songFields.get(9));
                    String kind = null;
                    if (fields >= 11)
                        kind = Utils.smartTrim(songFields.get(10));
                    String extension = null;
                    if (fileName.contains("."))
                        extension = fileName.substring(fileName.lastIndexOf("."));

                    songs.add(new SongMeta(name, artist, album, size, fileName, fileLocation, time, rating, genre, year, kind, extension));
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "failed to parse song meta\n" + song + "\n", e);
                }
            });
        }
        return songs;
    }

    public static void printFiles() throws IOException {
        // Print the names and IDs for up to 10 files.
        FileList result = service.files().list()
                .setPageSize(10)
                .setQ("'" + myMusicFolderID +"' in parents")
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        if (files == null || files.size() == 0) {
            System.out.println("No files found.");
        } else {
            System.out.println("Music Files:");
            for (File file : files) {
                System.out.println(file.getName() + " " + file.getSize() + " " + file.getKind() + " " + file.getCreatedTime() + " " + file.getPermissions());
            }
        }
    }
}
