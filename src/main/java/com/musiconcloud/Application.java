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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * @author sanjay.rajput on 04 Aug 2017 1:13 AM
 */
public class Application {

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
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
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
        // Build a new authorized API client service.
        service = getDriveService();
        String allSongs = args[0];

        cacheAllMusicFiles();
        if (allSongs != null && !allSongs.isEmpty()) {
            List<String> songInfoList = Arrays.asList(allSongs.split(ROW_DELIMITER));
            songInfoList.stream().forEach(song -> {
                System.out.println(songInfoList.indexOf(song) + " >. SONG INFO: " + song);
                List<String> songFields = Arrays.asList(song.split(FIELD_DELIMITER));
                String fileName = songFields.get(0);
                String filePath = songFields.get(1);
                String size = songFields.get(9);
                try {
                    if (!isFileExist(fileName)) {
                        System.out.println("Local File Size: " + Long.parseLong(size) * 1.0/1024/1024);
                        upload(fileName, filePath, Long.parseLong(size),  "audio/mpeg");
                    }
                } catch (IOException e) {
                    System.out.println(e);
                }
            });
        }
//        printFiles();
    }

    public static void upload(String fileName, String filePath, long size, String contentType) throws IOException {
        System.out.println("uploading " + fileName + ", at " + filePath);
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        fileMetadata.setParents(Collections.singletonList(myMusicFolderID));
        java.io.File file = new java.io.File(filePath);
        FileContent mediaContent = new FileContent(contentType, file);
        long tstart = new Date().getTime();
        File gFile = service.files().create(fileMetadata, mediaContent)
                .setFields("id, parents")
                .execute();
        long tend = new Date().getTime();
        allMusicFiles.add(fileName);
        long duration = (tend - tstart)/1000;
        System.out.println("File ID: " + gFile.getId() + ", Time Taken: " + duration + " sec, Speed: " + (size*1.0/1024/1024/duration) + " Mbps");
    }

    public static boolean isFileExist(String fileToSearch) throws IOException {
        System.out.println("checking for file: " + fileToSearch);
        if (!allMusicFiles.isEmpty()) {
            System.out.println("Total File Count: " + allMusicFiles.size());
            for (String file : allMusicFiles) {
                if (file != null && fileToSearch != null) {
                    if (file.equalsIgnoreCase(fileToSearch)) {
                        System.out.println("file already exist");
                        return true;
                    }
                } else {
                    System.out.println("file: " + file + ", fileToSearch: " + fileToSearch);
                }
            }
        }
        return false;
    }

    public static void cacheAllMusicFiles() throws IOException {
        String pageToken = null;
        do {
            FileList result = service.files().list()
                    .setSpaces("drive")
                    .setPageSize(1000)
                    .setPageToken(pageToken)
                    .setQ("'" + myMusicFolderID + "' in parents")
                    .setFields("nextPageToken, files(id, name)")
                    .execute();
            System.out.println("Result Size: " + result.size());
            List<File> files = result.getFiles();
            if (files != null && files.size() > 0) {
                files.forEach(x -> allMusicFiles.add(x.getName()));
            }
            pageToken = result.getNextPageToken();
            System.out.println("Token: " + pageToken);
        } while (pageToken != null);
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
