import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class PrimaryServer {

    private static final int serverPort = 7000;
    public static ArrayList<Connection> connections = new ArrayList<>();

    public static void main(String args[]) throws FileNotFoundException {

        String [] state = new String[2];
        File serverState = new File("server.txt");
        Scanner scan = new Scanner(serverState);
        state[0] = scan.nextLine();
        state[1] = scan.nextLine();
        scan.close();
        
        while (true) {

            if (state[0].equals("true") && state[1].equals("false")){

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

            else if (state[0].equals("false") && state[1].equals("true")) {

                try {

                    HeartbeatSender hb = new HeartbeatSender();
                    hb.join();

                    FileWriter newServerState = new FileWriter("server.txt");
                    newServerState.write("true\nfalse");
                    newServerState.close();
                    state[0] = "true";
                    state[1] = "false";

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}