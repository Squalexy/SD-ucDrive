import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {

    private static int thisServerPort;
    private static String otherServerAddress;
    private static int hearbeatPort;
    private static int fileSyncPortReceiver;
    private static int fileSyncPortSender;
    public static ArrayList<Connection> connections = new ArrayList<>();

    public static void main(String args[]) throws FileNotFoundException, UnknownHostException, InterruptedException {

        String[] addr = new String[5];
        Scanner sc = new Scanner(new File("addresses.txt"));
        for (int i = 0; i < 5; i++) {
            addr[i] = sc.nextLine().split(",")[1];
        }
        sc.close();

        otherServerAddress = addr[0];
        thisServerPort = Integer.parseInt(addr[1]);
        hearbeatPort = Integer.parseInt(addr[2]);
        fileSyncPortReceiver = Integer.parseInt(addr[3]);
        fileSyncPortSender = Integer.parseInt(addr[4]);

        FileSyncReceiver fsr = new FileSyncReceiver(fileSyncPortReceiver);
        HeartbeatSender hb = new HeartbeatSender(otherServerAddress, hearbeatPort);
        hb.join();
        fsr.interrupt();

        while (true) {

            ServerSocket listenSocket = null;
            Socket clientSocket = null;

            int numero = 0;
            try {

                System.out.println("Listening on port " + thisServerPort);
                listenSocket = new ServerSocket(thisServerPort);
                System.out.println("LISTEN SOCKET=" + listenSocket);
                new HeartbeatReceiver();

                while (true) {
                    clientSocket = listenSocket.accept(); // BLOQUEANTE
                    System.out.println("CLIENT_SOCKET (created at accept())=" + clientSocket);
                    numero++;
                    new Connection(clientSocket, numero, connections, otherServerAddress, fileSyncPortSender);
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
}
