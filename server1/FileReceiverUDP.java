import java.io.*;
import java.net.*;

public class FileReceiverUDP extends Thread{
    private DatagramSocket ds;
    private InetAddress otherAddr;
    private int otherPort;
    private File f;

    public FileReceiverUDP(InetAddress addr, int port, File f){
        otherAddr = addr;
        otherPort = port;
        this.f  = f;
        System.out.println("before start");
        this.start();
        System.out.println("after start");
    }

    @Override
    public void run(){
        while(true){
            try {
                ds = new DatagramSocket();
                
                // send ACK
                sendUTF("ACK");

                // receive file size
                long filesize = receiveLong();

                // send ACK
                sendUTF("ACK");
                
                // create buffer for file
                int nread = 1;
                int seq=0;
                long received = 0;
                byte[] rbuf = new byte[1024];
                byte[] buf = new byte[1024];
                FileOutputStream fos = new FileOutputStream(f);

                do{
                    System.out.println("[filesize] " + filesize);
                    System.out.println("[received] " + received);
                    if (received == filesize) break;
                    System.out.println("entrou no do");
                    DatagramPacket dp = new DatagramPacket(rbuf, rbuf.length);
                    ds.receive(dp);
        
                    ByteArrayInputStream bais = new ByteArrayInputStream(rbuf, 0, dp.getLength());
                    DataInputStream dis = new DataInputStream(bais);
                    nread = dis.read(buf);
                    
                    System.out.println("before nread > 0");
                    if (nread > 0){
                        received += nread;
                        fos.write(buf, 0, nread);
                        System.out.println("before send utf ack " + seq);
                        sendUTF("ACK" + seq);
                        seq++;
                        fos.flush();
                    }
                    
                } while (nread > 0);

                System.out.println("saiu do do while");

                String response = receiveUTF();

                System.out.println("after receive utf");

                // check if total file has received, else retry
                if (received == filesize && response.equals("ACK")){
                    System.out.println("equals ack");
                    sendUTF("ACK");
                    fos.close();
                    break;
                }
                else{
                    fos.close();
                    f.delete();
                }
            }
            catch(SocketException e){
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
            catch(IOException e){
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void sendUTF(String msg) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF(msg);
        byte [] buf = baos.toByteArray();

        DatagramPacket dp = new DatagramPacket(buf, buf.length, otherAddr, otherPort);
        ds.send(dp);
    }

    private long receiveLong() throws IOException{
        byte[] buf = new byte[8];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        ds.receive(dp);

        ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, dp.getLength());
        DataInputStream dis = new DataInputStream(bais);
        long value = dis.readLong();

        return value;
    }
    private String receiveUTF() throws IOException{
        byte [] rbuf = new byte[1024];
        DatagramPacket dr = new DatagramPacket(rbuf, rbuf.length);
        ds.receive(dr);

        // extract answer from datagram
        ByteArrayInputStream bais = new ByteArrayInputStream(rbuf, 0, dr.getLength());
        DataInputStream dis = new DataInputStream(bais);
        String response = dis.readUTF();
        return response;
    }

}
