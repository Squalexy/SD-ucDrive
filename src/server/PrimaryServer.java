package server;


import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class PrimaryServer {

    public static ArrayList<Connection> connections = new ArrayList<>();
    private static final int serverPort = 7000;

    public static void main(String args[]) {

        ServerSocket listenSocket = null;
        Socket clientSocket = null;

        int numero = 0;
        try {
            
            System.out.println("A Escuta no Porto 7000");
            listenSocket = new ServerSocket(serverPort);
            System.out.println("LISTEN SOCKET=" + listenSocket);
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



    // LEITURA DE FICHEIRO
    // FileOutputStream fos = new FileOutputStream (new File(filename));
    // int nread;
    // byte [] buf = new byte[buffsize];
    // do {
    //     nread = fis.read(buf);
    //     if (nread > 0) dos.write(buf, 0, nread);
    // }
    // while (nread-> -1);
    // fis.close()
    // ss.close();


}

class Connection extends Thread {
    DataInputStream in;
    DataOutputStream out;
    ArrayList<Connection> connections;
    Socket clientSocket;
    int thread_number;
    File currentDir;
    File usersFile;
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
    private void parseCommand(String data) throws IOException {

        String [] command = data.split(" ");

        // --------------------------------------------------- CLIENT AUTHENTICATION
        if (command[0].toUpperCase().equals("AU")){
            if (this.username.isEmpty()){
                if(find_user(command[1], command[2], usersFile)){
                    this.username = command[1];
                    this.password = command[2];
                    System.out.println("User" + this.username + "authenticated!");
                }
                else System.out.println("Wrong username or password!");
            }
            else if (this.username.equals(command[0])) System.out.println("User already registered!");
        }

        // --------------------------------------------------- MODIFY PASSWORD
        else if(command[0].toUpperCase().equals("PW")){
            if (!this.username.isEmpty()){
                modify_user_info(this.username, this.password, this.directory, 1, command[1]);
                // TODO: tem que desconectar utilizador
            }
            else System.out.println("User not registered!");
        }

        // --------------------------------------------------- LIST SERVER DIRECTORY
        else if (command[0].toUpperCase().equals("LSS")){
            if (!this.username.isEmpty()){
                listServerDirectory();
            }
            else System.out.println("User not registered!");
        }

        // --------------------------------------------------- CHANGE SERVER DIRECTORY
        else if (command[0].toUpperCase().equals("CDS")){
            if (!this.username.isEmpty()){
                changeDirectoryServer(command[1]);
                modify_user_info(this.username, this.password, this.directory, 2, command[1]);
            }
            else System.out.println("User not registered!");
        }

        // --------------------------------------------------- DOWNLOAD FROM SERVER DIRECTORY
        else if (command[0].toUpperCase().equals("DN")){
            if (!this.username.isEmpty()){
                downloadFile(command[1]);
            }
            else System.out.println("User not registered!");
        }
        
        // --------------------------------------------------- UPLOAD TO SERVER DIRECTORY
        else if (command[0].toUpperCase().equals("UP")){
            if (!this.username.isEmpty()){
                uploadFile(command[1]);
            }
            else System.out.println("User not registered!");
        }
        
        else System.out.println("Wrong command!");
    }

    // checks if user exists "in database"
    private boolean find_user(String username, String password, File user_file) throws FileNotFoundException {
        try (Scanner scan = new Scanner(user_file)) {
            while (scan.hasNextLine()) {
                String[] line = scan.nextLine().split(",");
                if (line[0].equals(username) && line[1].equals(password)) return true;
            }
        }
        return false;
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
        String newLine = "";

        // change password
        if (option == 1) {
            this.directory = System.getProperty("user.dir") + "/home";
            newLine = this.username + "," + command + "," + this.directory;
            System.out.println("\n----------\nPassword changed!\n----------\n");
            System.out.println();
        }
        
        // change directory
        else if (option == 2){
            this.directory = this.directory + "/" + command;
            newLine = this.username + "," + this.password + "," + this.directory;
            System.out.println("\n----------\nNew directory: " + this.directory + "----------\n");
        }

        // overwrite user info in users.txt
        fileContent = fileContent.replaceAll(oldLine, newLine);
        try (FileWriter writer = new FileWriter(user_file_path)) {
            writer.append(fileContent);
            writer.flush();
        }
  
    }


    // returns list of files in current directory (aka $ls)
    private void listServerDirectory() {
        if (currentDir.isDirectory()) {
            // TODO: SYNCHRONISE!!! and expand
            File[] paths = currentDir.listFiles();
            System.out.println("\n-- " + currentDir);
            if (!paths.toString().isEmpty()){
                for (File path : paths) {
                    System.out.println("---- " + path);
                }
            }
            else System.out.println("empty");
        }
        else System.out.println("Current dir isn't a valid directory!");
    }

    // handles remote directory navigation (aka $cd)
    private void changeDirectoryServer(String directory){
        if (currentDir.isDirectory()) {
            File[] paths = currentDir.listFiles();
            // if path isn't empty, you can do CD
            if (!paths.toString().isEmpty()) {
                // checks all available folders
                for (File path : paths) {
                    if (path.toString().equals(directory) && path.isDirectory()){
                        this.directory += "path";
                    }
                }
            }
            else System.out.println("\nFolder is empty!\n");
        }
        else System.out.println("Current dir isn't a valid directory!");
    }

    // function that handles file download
    private void downloadFile(String filename){
        // ainda faltam muitas verificacoes e tratamento de excepcoes aqui
        try (ServerSocket listenSocket = new ServerSocket(0)){
            int port = listenSocket.getLocalPort();
            out.writeInt(port);

            Socket downloadSocket = listenSocket.accept(); // BLOQUEANTE
            FileInputStream fis = new FileInputStream(new File(filename));
            new FileDownload(downloadSocket, fis);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    // function that handles file upload
    private void uploadFile(String filename){
        try (ServerSocket listenSocket = new ServerSocket(0)) {
            int port = listenSocket.getLocalPort();
            out.writeInt(port);

            Socket uploadSocket = listenSocket.accept(); // BLOQUEANTE  
            FileOutputStream fos = new FileOutputStream(new File(filename));
            new FileUpload(uploadSocket, fos);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

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

class FileDownload extends Thread {
    // private DataInputStream in;
    private DataOutputStream out;
    private Socket clientSocket;
    private FileInputStream fis;

    public FileDownload(Socket clientSocket, FileInputStream fis) {
        try{
            this.clientSocket = clientSocket;
            // this.in = new DataInputStream(clientSocket.getInputStream());
            this.out = new DataOutputStream(clientSocket.getOutputStream());
            this.fis = fis;
            this.start();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run(){
        int nread;
        int bufsize = 4096;
        byte[] buf = new byte[bufsize];
        try{
            do{
            nread = fis.read(buf);
            if (nread > 0) out.write(buf, 0, nread);
            }
            while (nread > -1);
            fis.close();
            clientSocket.close();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }        
    }
}

class FileUpload extends Thread {
    private DataInputStream in;
    // private DataOutputStream out;
    private Socket clientSocket;
    private FileOutputStream fos;

    public FileUpload(Socket clientSocket, FileOutputStream fos) {
        try{
            this.clientSocket = clientSocket;
            this.in = new DataInputStream(clientSocket.getInputStream());
            // this.out = new DataOutputStream(clientSocket.getOutputStream());
            this.fos = fos;
            this.start();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void run(){
        int nread;
        int offset = 0;
        int bufsize = 4096;
        byte[] buf = new byte[bufsize];
        try {
			do{
			    nread = in.read(buf);
			    if (nread > 0){
			        fos.write(buf, offset, nread);
			        offset += nread;
			    }
			}
			while (nread > -1);
            fos.close();
            clientSocket.close();
		} catch (IOException e) {
            System.out.println(e.getMessage());
			e.printStackTrace();
		}
    }
}



// heartbeat
// trg (datagramsocket ds) = new d« datagramdoscket(4000)
//     criar byte array input streams

//     byte buf [] = new byte[4096];
//     DatagramPacket dp = new DatagramPacket(buf, buf.length);
//     ds.receive(dp);
//     ByteArrayInputStream bais = new ByteArrayInputStream(buf, 0, dp, getLength());
//     DataInputStream dis = new DataInputStream(bais);

//     ByteArrayOutputStream baos = new ByteArrayOutputStream();
//     DataOutputStream dos = new DataOutputStream(baos)
    
// cliente envia heartbeat
// servidor responde