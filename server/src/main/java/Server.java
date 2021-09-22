import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
        try(ServerSocket serverSocket = new ServerSocket(8189)){
            System.out.println("server started");// прикрутить логгер
            while (true){
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                new Thread(handler).start();

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
