package com.geekbrains;

import java.util.List;


public class ListRequest extends Command {

    private String dir ;

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
