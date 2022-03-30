import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class SecondaryServer {
    
    private static final int serverPort = 6000;
    public static ArrayList<Connection> connections = new ArrayList<>();

    public static void main(String[] args) {

        try {

            HeartbeatSender hb = new HeartbeatSender();
            hb.join();

        }
        catch (Exception e){
            e.printStackTrace();
        }

        // SECONDARY SERVERS BECOMES PRIMARY SERVER
        ServerSocket listenSocket = null;
        Socket clientSocket = null;
        int numero = 0;

        try {
            System.out.println("A Escuta no Porto 6000");
            listenSocket = new ServerSocket(serverPort);
            System.out.println("LISTEN SOCKET=" + listenSocket);

            while (true) {
                clientSocket = listenSocket.accept(); // BLOQUEANTE
                System.out.println("CLIENT_SOCKET (created at accept())=" + clientSocket);
                numero++;

                new HeartbeatReceiver();
                new Connection(clientSocket, numero, connections);
            }
        } catch (IOException e) {
            System.out.println("Listen:" + e.getMessage());
        } finally {
            if (listenSocket != null)
                try {
                    listenSocket.close();
                } catch (IOException e) {
                    System.out.println("close:" + e.getMessage());
                }
            if (clientSocket != null)
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.out.println("close:" + e.getMessage());
                }
        }
    
    }

}
