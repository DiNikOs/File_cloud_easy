package com.dinikos.file_cloud_easy.common;

import javafx.scene.control.ListView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class FileList extends AbstractMessage {

    private String filename;
    //private ListView<String> filesList;
    private List<String> filesList;
    private byte[] data;

//    public ListView<String> getFileList() {
//        return filesList;
//    }

    public List<String> getFileList() {
        return filesList;
    }

    public String getFilename() {
        return filename;
    }

    public String get(int i) {
        return filesList.get(i);
    }

    public byte[] getData() {
        return data;
    }

    public FileList(List<String> filesList) throws IOException {
        this.filesList = filesList;
    }

//    public FileList(ListView<String> filesList) throws IOException {
//        this.filesList = filesList;
//    }
}
