package com.geekbrains;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ListResponse extends Command{
    private final List<String> names;
    private final List<Boolean> fileFolder;
    String root;


//переделать так чтобы летали простые классы

    boolean isFirstHandle;

    public ListResponse(Path path,boolean isFirstHandle,Path root) throws IOException {
        this.isFirstHandle = isFirstHandle;

// необходимо передавать полные пути
        names = Files.list(path).map(p->p.subpath(2,p.getNameCount()).toString()).collect(Collectors.toList());//заменил на полный путь
        Path rootPath = path.subpath(0,2);
        System.out.println(rootPath);

        fileFolder = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            fileFolder.add(Files.isDirectory(rootPath.resolve(names.get(i))));//менял тут
        }
        System.out.println(root.toString());
        System.out.println(path.toString());
        System.out.println(fileFolder.toString());
        System.out.println(names.toString());

        // где получить корень списка
        this.root = root.toString();


    }


    public String getCurrentDir() {
        return root;
    }

    public List<String> getFilesList(){
        return names;
    }

    public boolean isFirstHandle() {
        return isFirstHandle;
    }

    public List<Boolean> getFileFolder() {
        return fileFolder;
    }

    @Override
    public CommandType getType() {
        return CommandType.LIST_RESPONSE;
    }
}