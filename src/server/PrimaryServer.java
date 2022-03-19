package server;


import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.sound.midi.Soundbank;

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
    File currentDir, usersFile;
    String username = "";
    String password;
    String directory;

    public Connection(Socket aClientSocket, int numero, ArrayList<Connection> connections) {
        thread_number = numero;
        try {
            clientSocket = aClientSocket;
            this.connections = connections;
            synchronized (this) {
                this.connections.add(this);
            }
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());
            this.start();
        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
        }
    }

    //=============================
    @Override
    public void run() {
        
        // SETUP
        usersFile = new File("users.txt");

        // LOOP
        String resposta;
        try {
            while (true) {
                //an echo server
                String data = in.readUTF();
                System.out.println("T[" + thread_number + "] Recebeu: " + data);
                resposta = data.toUpperCase();

                // servidor trata do pedido do cliente aqui
                parseCommand(data);
                

                // alterar estas próximas linhas depois, nao é necessário o "c"
                for (Connection c : connections) {
                    c.out.writeUTF(resposta);
                    c.out.flush();
                }
            }
        } catch (EOFException e) {
            System.out.println("EOF:" + e);
            this.connections.remove(this);
        } catch (IOException e) {
            System.out.println("IO:" + e);
            this.connections.remove(this);
        }
    }

    // TODO: command parsing function
    private void parseCommand(String data) {
        String [] command = data.split(" ");

        // CLIENT AUTHENTICATION ----------------------------------- //
        if (command[0].toUpperCase().equals("AU")){
            if (this.username.isEmpty()){
                if(find_user(command[1], command[2], usersFile)){
                    this.username = command[1];
                    this.password = command[2];
                }
                else System.out.println("Wrong username or password!");
            }
            else if (this.username.equals(command[0])) System.out.println("User already registered!");
            else System.out.println("Error command AU");
        }

        // MODIFY PASSWORD ----------------------------------- //
        else if(command[0].toUpperCase().equals("PW")){
            if (!this.username.isEmpty()){
                modify_user_info(this.username, this.password, this.directory, 1, command[1]);
                
                // TODO: tem que desconectar utilizador
                
                
            }
            else System.out.println("User not registered!");
        }

        // LIST SERVER DIRECTORY  ----------------------------------- //
        else if (command[0].toUpperCase().equals("LSS")){
            listServerDirectory();
        }

        else if (command[0].toUpperCase().equals("CDS")){
            changeServerDirectory(command[1]);
            modify_user_info(this.username, this.password, this.directory, 2, command[1]);
        }

        else if command[0].toUpperCase().equals("DN"){
            
        }
        
        else if command[0].toUpperCase().equals("UP"){
            
        }
        return;
    }

    // TODO: check if user exists "in database"
    private boolean find_user(String username, String password, File user_file) throws FileNotFoundException {
        Scanner scan = new Scanner(user_file);
        while (scan.hasNextLine()) {
            String[] line = scan.nextLine().split(",");
            if (line[0].equals(username) && line[1].equals(password)) return true;
        }
        return false;
    }


    Scanner scan = new Scanner(user_file);
    StringBuffer buffer = new StringBuffer();
        while(scan.hasNextLine())

    {
        buffer.append
    }


        while(scan.hasNextLine())

    {
        String[] line = scan.nextLine().split(",");
        if (line[0].equals(username)) return true;
    }

}

    // TCP

    // TODO: function that handles client password update--> must disconnect user after calling this function and authenticate him again
    private void modify_user_info(String username, String password, String user_file_path, int option, String command) throws IOException {

        // command = password if option = 1
        // command = new dir if option = 2

        Scanner sc = new Scanner(new File(user_file_path));
        StringBuffer buffer = new StringBuffer();
        while (sc.hasNextLine()) buffer.append(sc.nextLine() + System.lineSeparator());
        String fileContent = buffer.toString();
        sc.close();

        String oldLine = username + "," + password + "," + user_file_path;

        // change password
        if (option == 1) {
            this.directory = System.getProperty("user.dir") + "/home";
            String newLine = username + "," + command + "," + directory;
        }
        
        // change directory
        else if (option == 2){
            this.directory = this.directory + "/" + command;
            String newLine = username + "," + command + "," + directory;
        }

        // overwrite user info in users.txt

  
    }


    // TODO: function that returns list of files in current directory (aka $ls)
    private void listServerDirectory() {
        if (currentDir.isDirectory()) {
            // TODO: SYNCHRONISE!!! and expand
            File[] paths = currentDir.listFiles();
            for (File path : paths) {
                System.out.println(path);
            }
        }
    }

    // TODO: function that handles remote directory navigation (aka $cd)
    private void changeServerDirectory(String directory){
        if (currentDir.isDirectory()) {
            File[] paths = currentDir.listFiles();
            for (File path : paths) {
                if (path.toString().equals(directory) && path.isDirectory()){
                    
                }
            }
        }
    }

    // TODO: function that handles file download

    // TODO: function that handles file upload

    // UDP
    // TODO: heartbeat function to other server

    // TODO?: data replication
}

class FileAccess {
    private String filename;
    private int readers;
    private int writers;

    public FileAccess(String filename){
        this.filename = filename;
        this.readers = 0;
        this.writers = 0;
    }

    public String getFilename(){
        return filename;
    }
    public int getReaders(){
        return readers;
    }
    public int getWriters(){
        return writers;
    }

    public void addReader(){
        readers++;
    }
    public void removeReader(){
        readers--;
    }
    public void addWriter(){
        writers++;
    }
    public void removeWriter(){
        writers--;
    }
}

class FileSystemManager {
    // NOTE: allow concurrent file reading but not if that file is being written on be someone else
    private ArrayList<FileAccess> accessList;

    public FileSystemManager() {
        accessList = new ArrayList<FileAccess>();
    }

    private synchronized FileAccess accessFile(String filename){
        for (FileAccess f : accessList){
            if (f.getFilename().equals(filename)) return f;
        }
        
        FileAccess f = new FileAccess(filename);
        accessList.add(f);
        return f;
    }

    public void writeFileStart(String filename){
        FileAccess f = accessFile(filename);
        synchronized(f){
            while (f.getReaders() > 0 || f.getWriters() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            f.addWriter();
        }
    }

    public void writeFileEnd(String filename){
        FileAccess f = accessFile(filename);
        synchronized(f){
            f.removeWriter();
            this.notifyAll();
        }
    }

    public void readFileStart(String filename){
        FileAccess f = accessFile(filename);
        synchronized(f){
            while (f.getWriters() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            f.addReader();
        }
    }

    public void readFileEnd(String filename){
        FileAccess f = accessFile(filename);
        synchronized(f){
            f.removeReader();
            this.notifyAll();
        }
    }
}




// heartbeat
trg (datagramsocket ds) = new d« datagramdoscket(4000)
    criar byte array input streams

    byte buf [] = new byte[4096];
    DatagramPacket dp = new DatagramPacket(buf, buf.length);
    ds.receive(dp);
    ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, dp, getLength());
    DataInputStream dis = new DataInputStream(bais);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(baos)
    
cliente envia heartbeat
servidor responde