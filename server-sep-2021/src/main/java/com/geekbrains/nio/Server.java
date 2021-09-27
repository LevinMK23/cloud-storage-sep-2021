package com.geekbrains.nio;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class Server {

    private ServerSocketChannel serverChannel;
    private Selector selector;
    private ByteBuffer buffer;
    private static Path ROOT = Paths.get("server-sep-2021","root");

    public Server() throws IOException {

        buffer = ByteBuffer.allocate(256);
        serverChannel = ServerSocketChannel.open();
        selector = Selector.open();
        serverChannel.bind(new InetSocketAddress(8189));
        log.debug("server started");
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (serverChannel.isOpen()) {

            selector.select();

            Set<SelectionKey> keys = selector.selectedKeys();

            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key);
                }
                if (key.isReadable()) {
                    handleRead(key);
                }
                iterator.remove();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new Server();
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();

        buffer.clear();
        int read = 0;
        StringBuilder msg = new StringBuilder();
        while (true) {
            if (read == -1) {
                channel.close();
                return;
            }
            read = channel.read(buffer);
            if (read == 0) {
                break;
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                msg.append((char) buffer.get());
            }
            buffer.clear();
        }
        String message = msg.toString().trim();
        log.debug("received  {}",msg);
        if(message.startsWith("ls")){
            channel.write(ByteBuffer.wrap(getFilesList().getBytes(StandardCharsets.UTF_8)));
        }else if(message.startsWith("cat")){
            String[] splitMessage =message.split(" ");
            if(splitMessage.length<2){
                log.debug("wrong command");
                channel.write(ByteBuffer.wrap("\n wrong command pls enter fileName ".getBytes(StandardCharsets.UTF_8)));

            }else {
                String fileName = splitMessage[1];
                channel.write(ByteBuffer.wrap(getFdataAsString(fileName).getBytes(StandardCharsets.UTF_8)));
            }

        }else {
            channel.write(ByteBuffer.wrap(("\n wrong command pls choos cat or ls").getBytes(StandardCharsets.UTF_8)));
            log.debug("wrong command");
        }


        channel.write(ByteBuffer.wrap(("[" + LocalDateTime.now() + "] " + message + "\n").getBytes(StandardCharsets.UTF_8)));
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ);
        log.debug("client accepted");
    }
    private String getFilesList() throws IOException {
        return Files.list(ROOT)
                .map(this::fileOrDir)
                .collect(Collectors.joining("\n")).toString() + "\n";
    }
    private String fileOrDir(Path path){
        if(Files.isDirectory(path)){
            return String.format("%3s%s" ,"[] " ,path.getFileName().toString() );

        }else {
            return String.format("%3s%s" ,"<> " ,path.getFileName().toString() );
        }

    }
    private String getFdataAsString (String fileName) throws IOException {
        if(Files.isDirectory(ROOT.resolve(fileName))){
            return " {Error} cannot cat applied to " + fileName;
        }else {

            byte[] bytes = Files.readAllBytes(ROOT.resolve(fileName));
            return new String(bytes);

        }
    }
}
