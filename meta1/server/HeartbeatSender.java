import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class HeartbeatSender extends Thread{
    
    private static final int maxfailedrounds = 5;
    private static final int timeout = 1000;
    private static final int bufsize = 4096;
    private int port;
    private static final int period = 1000;
    private String otherServerAddress;    

    public HeartbeatSender(String otherServerAddress, int heartbeatPort) throws UnknownHostException{
        this.otherServerAddress = otherServerAddress;
        this.port = heartbeatPort;
        this.start();
    }

    @Override
    public void run(){

        int count = 1;
        InetAddress ia;

        try (DatagramSocket ds = new DatagramSocket()) {
            ia = InetAddress.getByName(this.otherServerAddress);
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
                    /*
                    ByteArrayInputStream bais = new ByteArrayInputStream(rbuf, 0, dr.getLength());
                    DataInputStream dis = new DataInputStream(bais);
                    int n = dis.readInt();
                    System.out.println("Got: " + n + ".");
                    */
                }
                catch (SocketTimeoutException ste) {
                    failedheartbeats++;
                    System.out.println("Failed heartbeats: " + failedheartbeats);
                    if (failedheartbeats > 4) return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Thread.sleep(period);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
        }
    }
}
