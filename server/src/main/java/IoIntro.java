import java.io.*;

public class IoIntro {
    private static byte[] buffer = new byte[1024];
    private static final String APP_NAME = "server/";
    private static final String ROOT_DIR = "server/root/";

    private String readAsString (String resourceName) throws IOException {
        InputStream inStream = getClass().getResourceAsStream(resourceName);
        int readed = inStream.read(buffer);
        return new String(buffer,0,readed);

    }
    private void createServerDir(String dirName){
        File dir = new File(APP_NAME + dirName);
        if(!dir.exists()){
            dir.mkdir();
        }
    }
    private void transfer (File srs, File dst){
        try(FileInputStream is = new FileInputStream(srs); FileOutputStream os = new FileOutputStream(dst)){
            int readed = 0;
            while ((readed =is.read(buffer)) != -1){

                os.write(buffer,0,readed);
            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws IOException {
        IoIntro io = new IoIntro();
        System.out.println(io.readAsString("hello.txt"));
        io.createServerDir("root");
        io.transfer(new File(APP_NAME + "src/main/resources/hello.txt"), new File(ROOT_DIR + "copy_hello.txt"));

    }
}
