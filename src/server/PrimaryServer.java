package server;

import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class PrimaryServer {

    public static ArrayList<Connection> connections = new ArrayList<>();

    public static void main(String args[]) {
        int numero = 0;
        try {
            int serverPort = 7000;
            System.out.println("A Escuta no Porto 7000");
            ServerSocket listenSocket = new ServerSocket(serverPort);
            System.out.println("LISTEN SOCKET=" + listenSocket);
            while (true) {
                Socket clientSocket = listenSocket.accept(); // BLOQUEANTE
                System.out.println("CLIENT_SOCKET (created at accept())=" + clientSocket);
                numero++;
                new Connection(clientSocket, numero, connections);
            }
        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
        }
    }



    /* LEITURA DE FICHEIRO
    FileOutputStream fos = new FileOutputStream (new File(filename));
    int nread;
    byte [] buf = new byte[buffsize];
    do {
        nread = fis.read(buf);
        if (nread > 0) dos.write(buf, 0, nread);
    }
    while (nread-> -1);
    fis.close()
    ss.close();
    */





}

class Connection extends Thread {
    DataInputStream in;
    DataOutputStream out;
    ArrayList<Connection> connections;
    Socket clientSocket;
    int thread_number;

    public Connection (Socket aClientSocket, int numero, ArrayList<Connection> connections) {
        thread_number = numero;
        try{
            clientSocket = aClientSocket;
            this.connections=connections;
            synchronized (this){
                this.connections.add(this);
            }
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            this.start();
        }catch(IOException e){System.out.println("Connection:" + e.getMessage());}
    }
    //=============================
    @Override
    public void run(){
        String resposta;
        try{
            while(true){
                //an echo server
                String data = in.readUTF();
                System.out.println("T["+thread_number + "] Recebeu: "+data);
                resposta=data.toUpperCase();
                for( Connection c : connections){
                    c.out.writeUTF(resposta);
                    c.out.flush();
                }
            }
        }catch(EOFException e){
            System.out.println("EOF:" + e);
            this.connections.remove(this);
        }catch(IOException e){System.out.println("IO:" + e);
            this.connections.remove(this);
        }
    }
}
