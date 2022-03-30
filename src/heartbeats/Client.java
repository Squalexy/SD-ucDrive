

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class Client {
    private static final int maxfailedrounds = 5;
    private static final int timeout = 5000;
    private static final int bufsize = 4096;
    private static final int port = 7000;
    private static final int period = 10000;

    public static void main(String[] args) throws IOException, InterruptedException {
        int count = 1;
        
        InetAddress ia = InetAddress.getByName("localhost");
        try (DatagramSocket ds = new DatagramSocket()) {
            ds.setSoTimeout(timeout);
            int failedheartbeats = 0;
            while (failedheartbeats < maxfailedrounds) {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeInt(count++);
                    byte [] buf = baos.toByteArray();
                    
                    DatagramPacket dp = new DatagramPacket(buf, buf.length, ia, port);                
                    ds.send(dp);
                    
                    byte [] rbuf = new byte[bufsize];
                    DatagramPacket dr = new DatagramPacket(rbuf, rbuf.length);

                    ds.receive(dr);
                    failedheartbeats = 0;
                    ByteArrayInputStream bais = new ByteArrayInputStream(rbuf, 0, dr.getLength());
                    DataInputStream dis = new DataInputStream(bais);
                    int n = dis.readInt();
                    System.out.println("Got: " + n + ".");
                }
                catch (SocketTimeoutException ste) {
                    failedheartbeats++;
                    System.out.println("Failed heartbeats: " + failedheartbeats);
                }
                Thread.sleep(period);
            }
        }
    }
}
