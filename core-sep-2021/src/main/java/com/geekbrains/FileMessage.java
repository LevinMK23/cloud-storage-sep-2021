package com.geekbrains;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMessage extends Command {

    private final String name;

    private long size;

    private final byte[] bytes;



    public FileMessage(Path path) throws IOException {
        name = path.getFileName().toString();
        bytes= Files.readAllBytes(path);
        size = Files.size(path);
    }
    public FileMessage(String name, byte[] bytes,long size) throws IOException {
        this.name = name;
        this.bytes= bytes;
    }
    public byte[] getBytes(){
        return bytes;
    }


    public String getName() {
        return name;
    }



    public long size(){
        return size;
    }

    @Override
    public CommandType getType() {
        return CommandType.FILE_MESSAGE;
    }
}
