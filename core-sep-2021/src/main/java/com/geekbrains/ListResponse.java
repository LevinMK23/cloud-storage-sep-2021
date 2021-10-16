package com.geekbrains;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ListResponse extends Command{
    private final List<String> names;
    String currentDir;

    public ListResponse(Path path,String dir) throws IOException {

        names = Files.list(path).map(p->p.getFileName().toString()).collect(Collectors.toList());
        // тут добавить префикс - файл - директория
        for (int i = 0; i< names.size(); i++) {
            if(Files.isDirectory(path.resolve(names.get(i)))){

                names.set(i,"[D]:"+names.get(i));
            }else names.set(i,"<f>:"+names.get(i));

        }
        currentDir = dir;

    }

    public String getCurrentDir() {
        return currentDir;
    }

    public List<String> getFilesList(){
        return names;
    }

    @Override
    public CommandType getType() {
        return CommandType.LIST_RESPONSE;
    }
}
