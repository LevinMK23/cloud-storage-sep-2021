package com.geekbrains;

public class FileResponse extends Command{
    private boolean response;
    private String message;


    public String getMessage() {
        return message;
    }

    public boolean isDone() {
        return response;
    }

    public FileResponse(boolean response, String message) {
        this.response = response;
        this.message = message;
    }



    @Override
    public CommandType getType() {
        return CommandType.FILE_RESPONSE;
    }
}
