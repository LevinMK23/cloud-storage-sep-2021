package com.geekbrains;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

        names = Files.list(path).map(p->p.getFileName().toString()).collect(Collectors.toList());
        fileFolder = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            fileFolder.add(Files.isDirectory(root.resolve(names.get(i))));
        }


        // где получить корень списка
        this.root = root.toString();
        System.out.println(root.getFileName() + " : "+ names.toString());

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
