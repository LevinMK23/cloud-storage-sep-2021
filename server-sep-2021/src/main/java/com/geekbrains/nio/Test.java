package com.geekbrains.nio;

import java.util.List;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Test {
    public static void main(String[] args) throws IOException {
        String message = "cat copy.txt";
        String[] words = message.split(" ");
        if (message.equals("ls")) {
            Path path = Paths.get("server-sep-2021", "root");
            try (DirectoryStream<Path> files = Files.newDirectoryStream(path)) {
                for (Path paths : files)
                    System.out.println(paths);
            }
        } else if (words[0].equals("cat")) {
            Path path = Paths.get("server-sep-2021", "root", words[1]);
            List<String> list = Files.readAllLines(path);
            for (String str : list) {
                System.out.println(str);
            }
        } else {
            System.out.println(message);
        }
    }
}

