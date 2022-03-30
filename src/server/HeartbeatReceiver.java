import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;

public class HeartbeatReceiver extends Thread {

    private static final int port = 7000;
    private static final int bufsize = 4096;

    public HeartbeatReceiver() throws UnknownHostException {
        this.start();
    }

    @Override
    public void run() {

        try (DatagramSocket ds = new DatagramSocket(port)) {
            while (true) {
                byte buf[] = new byte[bufsize];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                ds.receive(dp);
                ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, dp.getLength());
                DataInputStream dis = new DataInputStream(bais);
                int count = dis.readInt();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(count);
                byte resp[] = baos.toByteArray();
                DatagramPacket dpresp = new DatagramPacket(resp, resp.length, dp.getAddress(), dp.getPort());
                ds.send(dpresp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
