package com.geekbrains;

import java.util.List;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

//Класс-команда для передачи списка файлов и папок в текущей директории сервера

public class ListResponse extends Command {

    private final List<String> list;

    public ListResponse(Path path) throws IOException {
        list = Files.list(path)
                .map(p->p.getFileName().toString())
                .collect(Collectors.toList());

    }
    public List<String> getList() {
        return list;
    }
    @Override
    public CommandType getType() {
        return CommandType.LIST_RESPONSE;
    }
}
