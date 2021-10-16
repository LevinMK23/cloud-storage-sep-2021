package com.geekbrains;

public class FileRequest extends Command{



    private String fileName;

    public FileRequest(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE_REQUEST;
    }
}
