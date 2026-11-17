package com.dinikos.file_cloud_easy.common;

public class Command extends AbstractMessage {
    private String cmd;
    private String filename;

    public String getCommand() {
        return cmd;
    }

    public Command(String cmd, String filename) {
        this.cmd = cmd;
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}

