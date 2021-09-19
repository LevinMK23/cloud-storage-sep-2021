package com.geekbrains.io;

import java.io.File;

public class CreateFolderService {

    private static final String APP_NAME = "/GB cloud storage/Lesson_1/cloud-storage-sep-2021/server-sep-2021/";

    public void createServerDir(String dirName) {
        File dir = new File(APP_NAME + dirName);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }
}

