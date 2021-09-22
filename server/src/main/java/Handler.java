import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Handler implements Runnable{
    private final Socket soket;

    public Handler(Socket soket) {
        this.soket = soket;
    }

    public Socket getSoket() {
        return soket;
    }
    private final String SINCRONIZED_DIR = "server/root/syncronyzed/";


    @Override
    public void run() {
        try (DataInputStream is = new DataInputStream(soket.getInputStream()); DataOutputStream os = new DataOutputStream(soket.getOutputStream())){
            while (true){
                String s = is.readUTF();
                if(s.startsWith("{F_N}")){
                    System.out.println("Recived { "+s+" }"  );
                    s = s.substring(5,s.length());
                    createOrUpdateFiles(s,is);
                }


                os.writeUTF(s + " сохранён в облаке ");
                os.flush();

            }

        }catch (Exception e){
            e.printStackTrace();
            System.out.println("connection lost");
        }
    }
    public void createOrUpdateFiles(String filename, DataInputStream is){
            File dir = new File(SINCRONIZED_DIR );
            if(!dir.exists()) dir.mkdir();
            File file = new File( SINCRONIZED_DIR + filename);
        try(FileOutputStream fileOutputStream = new FileOutputStream(file)){
            if(file.exists()){
                file.delete();
            }
            file.createNewFile();
            String msg ="";
            while (!msg.equals("{END}")){
                int avaliable = is.readInt();
                byte[] buffer = new byte[avaliable];

                is.readFully(buffer);
                msg = is.readUTF();
                fileOutputStream.write(buffer,0,avaliable);
                fileOutputStream.flush();
            }


            System.out.println("файл сохранён");
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
