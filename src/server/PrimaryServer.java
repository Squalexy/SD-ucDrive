import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class PrimaryServer {

    public static ArrayList<Connection> connections = new ArrayList<>();
    private static final int serverPort = 7000;
    private static final int secServerPort = 6000;

    public static void main(String args[]) {

        ServerSocket listenSocket = null;
        Socket clientSocket = null;

        int numero = 0;
        try {

            System.out.println("A Escuta no Porto 7000");
            listenSocket = new ServerSocket(serverPort);
            System.out.println("LISTEN SOCKET=" + listenSocket);
            new HeartbeatReceiver();

            while (true) {
                clientSocket = listenSocket.accept(); // BLOQUEANTE
                System.out.println("CLIENT_SOCKET (created at accept())=" + clientSocket);
                numero++;
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