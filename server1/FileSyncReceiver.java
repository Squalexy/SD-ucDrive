import java.net.*;
import java.io.*;

public class FileSyncReceiver extends Thread{

    private DatagramSocket ds;
    private int portToListen;

    public FileSyncReceiver(int port){
        this.portToListen = port;
        this.start();
    }
    
    @Override
    public void run(){
        try{
            ds = new DatagramSocket(this.portToListen);

            while(true){
                byte[] buf = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                ds.receive(dp);
    
                ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, dp.getLength());
                DataInputStream dis = new DataInputStream(bais);
                String filename = dis.readUTF();
                System.out.println("[dir filename]: " + filename);

                String dir = new File("").getAbsolutePath() + "/" + filename;
                File incoming = new File(dir);
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

        finally{
            ds.close();
        }
    }
}
