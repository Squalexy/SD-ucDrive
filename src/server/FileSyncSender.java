import java.net.*;
import java.io.*;

public class FileSyncSender extends Thread{
    private String secondaryAddr;
    private int secondaryPort;
    private DatagramSocket ds;
    String path;

    public FileSyncSender(String secondaryAddr, int port, String path){
        this.secondaryAddr = secondaryAddr;
        this.secondaryPort = port;
        this.path = path;
    }
    
    @Override
    public void run(){
        try{
            // create new UDP socket dedicated to file transfer
            InetAddress ia = InetAddress.getByName(secondaryAddr);
            ds = new DatagramSocket();
            
            while (true){
                // create byte array containing the name of the file to be transfered
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeUTF(path);
                byte [] buf = baos.toByteArray();
                
                // send filename through udp socket
                DatagramPacket dp = new DatagramPacket(buf, buf.length, ia, secondaryPort);
                ds.send(dp);

                // receive server answer
                byte [] rbuf = new byte[1024];
                DatagramPacket dr = new DatagramPacket(rbuf, rbuf.length);
                ds.receive(dr);

                // extract answer from datagram
                ByteArrayInputStream bais = new ByteArrayInputStream(rbuf, 0, dr.getLength());
                DataInputStream dis = new DataInputStream(bais);
                String response = dis.readUTF();

                // if the request was not acknowledge, try again
                if (!response.equals("ACK")) continue;

                int transferPort = dr.getPort();

                // send filesize
                File outgoing = new File(path);
                FileInputStream fis = new FileInputStream(outgoing);

                baos = new ByteArrayOutputStream();
                dos = new DataOutputStream(baos);
                dos.writeLong(outgoing.length());
                buf = baos.toByteArray();
                
                // send filename through udp socket
                dp = new DatagramPacket(buf, buf.length, ia, transferPort);
                ds.send(dp);

                response = receiveUTF();

                // if the request was not acknowledge, try again
                if (!response.equals("ACK")){
                    fis.close();
                    continue;
                }

                int nread, sequence = 0;
                byte[] fileBuf = new byte[1024];
                do{
                    nread = fis.read(buf);
                    if (nread > 0){
                        String ack = "";
                        while(!ack.equals("ACK" + sequence)){
                            baos = new ByteArrayOutputStream();
                            dos = new DataOutputStream(baos);
                            dos.write(fileBuf);
                            byte [] outBuf = baos.toByteArray();
                            
                            // send filename through udp socket
                            dp = new DatagramPacket(outBuf, outBuf.length, ia, transferPort);
                            ds.send(dp);

                            ack = receiveUTF();
                        }
                        sequence++;
                    }
                } while (nread > -1);

                fis.close();

                sendUTF("ACK", ia, transferPort);
                if (receiveUTF().equals("ACK")) break;
            }
        }
        catch (UnknownHostException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch(SocketException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch(IOException e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        finally{
            ds.close();
        }
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

    private void sendUTF(String msg, InetAddress ia, int port) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeUTF(msg);
        byte [] buf = baos.toByteArray();

        DatagramPacket dp = new DatagramPacket(buf, buf.length, ia, port);
        ds.send(dp);
    }
}
