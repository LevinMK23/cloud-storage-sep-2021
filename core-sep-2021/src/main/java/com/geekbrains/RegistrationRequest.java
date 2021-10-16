package com.geekbrains;

public class RegistrationRequest extends Command{
    private String userName;
    private String pass;

    public RegistrationRequest(String userName,String pass) {
        this.userName = userName;
        this.pass = pass;

    }

    public String getUserName() {
        return userName;
    }

    public String getPass() {
        return pass;
    }

    @Override
    public CommandType getType() {
        return CommandType.REGISTRATION_REQUEST;
    }
}
