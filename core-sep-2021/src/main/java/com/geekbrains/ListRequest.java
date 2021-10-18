package com.geekbrains;

import java.nio.file.Path;


public class ListRequest extends Command {

    private String dir ;

    public ListRequest(Path dir) {
        this.dir = dir.toString();
    }

    public String getDir() {
        return dir;
    }

    @Override
    public CommandType getType() {
        return CommandType.LIST_REQUEST;
    }
}
