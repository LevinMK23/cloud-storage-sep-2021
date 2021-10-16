package com.geekbrains.netty;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;


@Slf4j
public class SQLHandler {

    private static final Path ROOT = Paths.get("server-sep-2021", "root");



    private static Connection connection = null;
    public static Connection getConnection() {
        return connection;
    }
    public static Connection createConnection() throws ClassNotFoundException, SQLException {

        String con = "jdbc:mysql://localhost:3306/users";

        Class.forName("com.mysql.jdbc.Driver");

        connection = DriverManager.getConnection(con,"root","36963aqz");
        return connection;
    }
    public static boolean createNewUser(String userName, String pass) throws IOException, SQLException {// добавить проверку на уникальность имени в базе
        Path userPath = ROOT.resolve(userName);
        String nickValidation = "SELECT id FROM users_table WHERE user_name= '"+userName+"';";
        String createNewUser = "INSERT INTO users_table (user_name,pass) VALUES('"+userName+"', '"+pass +"');";

        ResultSet result = null;
        try {
            PreparedStatement pst = getConnection().prepareStatement(nickValidation);

            result = pst.executeQuery();



        } catch (Exception e) {
            log.error(" EROR DB ", e);
        }


        if(!result.isBeforeFirst()) {

            try {
                PreparedStatement pst = getConnection().prepareStatement(createNewUser);
                pst.executeUpdate();
            } catch (Exception e) {
                log.error("cant create new user", e);
            }
            Files.createDirectory(userPath);
            return true;
        }else return false;

       //ввести проверку на уникальность имени


    }
    public static ResultSet getUserFromDb(String userName, String pass){
        String requestToDb  = "SELECT * FROM users_table WHERE user_name= '"+userName+"' AND pass = '"+pass+"';";
        ResultSet result = null;
        try {
            PreparedStatement pst = getConnection().prepareStatement(requestToDb);
            result = pst.executeQuery();


        } catch (Exception e) {
            log.error(" EROR DB ", e);
        }
        return result;
    }




}
