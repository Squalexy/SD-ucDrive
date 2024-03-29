import java.net.*;
import java.io.*;

public class FileUpload extends Thread {
    private DataInputStream in;
    // private DataOutputStream out;
    private Socket clientSocket;
    private FileOutputStream fos;
    private String root;
    private String otherServerAddress;
    private int fileSyncPort;

    public FileUpload(Socket clientSocket, FileOutputStream fos, String root, String otherServerAddress, int fileSyncPort) {
        try {
            this.clientSocket = clientSocket;
            this.root = root;
            this.in = new DataInputStream(clientSocket.getInputStream());
            this.otherServerAddress = otherServerAddress;
            this.fileSyncPort = fileSyncPort;
            // this.out = new DataOutputStream(clientSocket.getOutputStream());
            this.fos = fos;
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
                nread = in.read(buf);
                if (nread > 0) {
                    fos.write(buf, 0, nread);
                }
            } while (nread > -1);
            fos.close();
            clientSocket.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        new FileSyncSender(fileSyncPort, root, otherServerAddress);
    }
}