import java.net.*;
import java.io.*;

public class FileSyncReceiver extends Thread{
    public FileSyncReceiver(){
        this.start();
    }
    
    @Override
    public void run(){
        try{
            DatagramSocket ds = new DatagramSocket();

            while(true){
                byte[] buf = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                ds.receive(dp);
    
                ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, dp.getLength());
                DataInputStream dis = new DataInputStream(bais);
                String filename = dis.readUTF();
    
                File incoming = new File(filename);
                InetAddress transferAddr = dp.getAddress();
                int transferPort = dp.getPort();
                new FileReceiverUDP(transferAddr, transferPort, incoming);
            }
        }
        catch(SocketException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch (IOException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
