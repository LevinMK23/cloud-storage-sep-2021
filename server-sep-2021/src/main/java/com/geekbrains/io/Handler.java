package com.geekbrains.io;

import java.net.Socket;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Handler implements Runnable {

    private final Socket socket;
    private static final String ROOT_DIR = "/GB cloud storage/Lesson_1/cloud-storage-sep-2021/server-sep-2021/root";


    public Handler(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    @SneakyThrows
    @Override
    public void run() {
        InputStream in = socket.getInputStream();

        // Читаем размер имени
        int nameSize;
        while ((nameSize = in.read()) != -1) {

            // Читаем само имя
            byte[] name = new byte[nameSize + 1];
            in.read(name, 0, nameSize);

            // Преобразовываем обратно в строку
            String fileName = new String(name, "utf-8").trim();
            System.out.println(fileName);

            File file = new File(ROOT_DIR + "/" + fileName);
            try (FileOutputStream out = new FileOutputStream(file)) {

                // Читаем размер файл
                byte[] fileSizeBuf = new byte[8];
                in.read(fileSizeBuf, 0, 8);

                // Преобразовываем в long
                ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
                buf.put(fileSizeBuf);
                buf.flip();
                long fileSize = buf.getLong();

                // Читаем содержимое файла блоками по килобайту и пишем в файл
                int read = 0;
                byte[] data = new byte[1024];
                while (read < fileSize) {
                    read += in.read(data);
                    out.write(data);
                }
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }
    }
}

