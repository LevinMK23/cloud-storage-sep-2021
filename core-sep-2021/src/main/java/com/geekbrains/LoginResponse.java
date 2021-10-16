package com.geekbrains;

public class LoginResponse extends Command {
    private boolean response;
    private String userName;


    public String getUserName() {
        return userName;
    }

    public LoginResponse(boolean response, String userName) {
        this.response = response;
        this.userName = userName;
    }

    public boolean isValid() {
        return response;
    }

    @Override
    public CommandType getType() {
        return CommandType.LOGIN_RESPONSE;
    }
}
