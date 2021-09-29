package com.geekbrains;

import java.util.List;
import java.io.File;

//Класс-команда для передачи списка файлов и папок в текущей директории сервера

public class ListResponse extends Command {

    private final List<File> list;

    public ListResponse(List<File> list) {
        this.list = list;

    }
    public List<File> getList() {
        return list;
    }
    @Override
    public CommandType getType() {
        return CommandType.LIST_RESPONSE;
    }
}
