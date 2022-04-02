import java.net.*;
import java.io.*;

public class FileDownload extends Thread {
    // private DataInputStream in;
    private DataOutputStream out;
    private Socket clientSocket;
    private FileInputStream fis;

    public FileDownload(Socket clientSocket, FileInputStream fis) {
        try {
            this.clientSocket = clientSocket;
            // this.in = new DataInputStream(clientSocket.getInputStream());
            this.out = new DataOutputStream(clientSocket.getOutputStream());
            this.fis = fis;
            this.start();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run() {
        int nread;
        int bufsize = 4096;
        byte[] buf = new byte[bufsize];
        try {
            do {
                nread = fis.read(buf);
                if (nread > 0)
                    out.write(buf, 0, nread);
            } while (nread > -1);
            fis.close();
            clientSocket.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
