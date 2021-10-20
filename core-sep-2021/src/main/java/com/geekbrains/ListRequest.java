package com.geekbrains;

import java.nio.file.Path;


public class ListRequest extends Command {

    private String dir ;// тут объект path до васи включая

    public ListRequest(String dir) {
        this.dir = dir;
    }

    public String getDir() {
        return dir;
    }

    @Override
    public CommandType getType() {
        return CommandType.LIST_REQUEST;
    }
}
