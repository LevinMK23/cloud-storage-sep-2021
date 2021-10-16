package com.geekbrains;



public class LoginRequest extends Command  {
    private String login;
    private String pass;

    public LoginRequest(String login, String pass) {
        this.login = login;
        this.pass = pass;
        System.out.println(login + ":" + pass);
    }

    public String getLogin() {
        return login;
    }

    public String getPass() {
        return pass;
    }

    @Override
    public CommandType getType() {
        return CommandType.LOGIN_REQUEST;
    }
}
