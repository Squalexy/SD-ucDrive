import java.net.*;
import java.io.*;

public class FileSyncReceiver extends Thread{

    private DatagramSocket ds;
    private int portToListen;

    public FileSyncReceiver(int port){
        System.out.println("Before start");
        this.portToListen = port;
        this.start();
    }
    
    @Override
    public void run(){
        try{
            System.out.println("After try");
            ds = new DatagramSocket(this.portToListen);

            while(true){
                System.out.println("After while true");
                byte[] buf = new byte[1024];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                System.out.println("[RECEIVING] from port" + dp.getPort());
                System.out.println("before receive");
                ds.receive(dp);
                System.out.println("after receive");
    
                ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, dp.getLength());
                DataInputStream dis = new DataInputStream(bais);
                String filename = dis.readUTF();
                System.out.println("[dir filename]: " + filename);

                String dir = new File("").getAbsolutePath() + "/" + filename;
                File incoming = new File(dir);
                InetAddress transferAddr = dp.getAddress();
                int transferPort = dp.getPort();
                System.out.println("[before filereceiverudp] dpgetport = " + transferPort);
                System.out.println("[before filereceiverudp] transferAddr = " + transferAddr);
                new FileReceiverUDP(transferAddr, transferPort, incoming);
                System.out.println("after filereceiverudp");
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
