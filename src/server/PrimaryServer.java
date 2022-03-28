import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.file.Paths;

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
}

class Connection extends Thread {
    DataInputStream in;
    DataOutputStream out;
    ArrayList<Connection> connections;
    Socket clientSocket;
    int thread_number;
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


    public String getDirectory() {
        return this.directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }


    //=============================
    @Override
    public void run() {

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

                // DEBUGGING ------------------------------------------------------
                // System.out.println("Username: " + this.username);
                // System.out.println("Password: " + this.password);
                //System.out.println("Directory: " + this.directory);
                // DEBUGGING ------------------------------------------------------
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

        usersFile = new File("users.txt");

        // --------------------------------------------------- CLIENT AUTHENTICATION
        if (command[0].equalsIgnoreCase("AU")){
            if (command.length != 3) {
                out.writeUTF("\n[ERROR] AU <username> <password>\n");
                return;
            }

            if (this.username.isEmpty()){
                if(find_user(command[1], command[2], usersFile)){
                    this.username = command[1];
                    this.password = command[2];
                    out.writeUTF("\n\n-------\nWelcome " + this.username + ", you are now authenticated!\n-------\n");
                }
                else out.writeUTF("\n[ERROR] Wrong username or password!\n");
            }
            else if (this.username.equals(command[0])) out.writeUTF("\n[ERROR] You're already registered!\n");
        }

        // --------------------------------------------------- MODIFY PASSWORD
        else if(command[0].equalsIgnoreCase("PW")){
            if (command.length != 3) {
                out.writeUTF("\n[ERROR] PW <password> <new password>\n");
                return;
            }

            if (!this.username.isEmpty()){
                modify_user_info(this.username, this.password, this.directory, 1, command[2], out);
                // TODO: must disconnect user after calling this function
            }
            else out.writeUTF("\n[ERROR] You're not registered!\n");
        }

        // --------------------------------------------------- LIST SERVER DIRECTORY
        else if (command[0].equalsIgnoreCase("LSS")){
            if (command.length != 1) {
                out.writeUTF("\n[ERROR] LSS\n");
                return;
            }

            if (!this.username.isEmpty()){
                listServerDirectory(out);
            }
            else out.writeUTF("\n[ERROR] You're not registered!\n");
        }

        // --------------------------------------------------- CHANGE SERVER DIRECTORY
        else if (command[0].equalsIgnoreCase("CDS")){
            if (command.length != 2) {
                out.writeUTF("\n[ERROR] CDS <new directory>\n");
                return;
            }

            if (!this.username.isEmpty()){
                String oldDir = getDirectory();
                setDirectory(changeServerDirectory(oldDir, command[1], out));
                modify_user_info(this.username, this.password, oldDir, 2, command[1], out);
            }
            else out.writeUTF("\n[ERROR] You're not registered!\n");
        }

        // --------------------------------------------------- DOWNLOAD FROM SERVER DIRECTORY
        else if (command[0].equalsIgnoreCase("DN")){
            if (command.length != 2) {
                out.writeUTF("\n[ERROR] DN <filename>\n");
                return;
            }

            if (!this.username.isEmpty()){
                downloadFile(command[1]);
            }
            else out.writeUTF("\n[ERROR] You're registered!\n");
        }
        
        // --------------------------------------------------- UPLOAD TO SERVER DIRECTORY
        else if (command[0].equalsIgnoreCase("UP")){
            if (command.length != 2) {
                out.writeUTF("\n[ERROR] UP <filename>\n");
                return;
            }

            if (!this.username.isEmpty()){
                uploadFile(command[1]);
            }
            else out.writeUTF("\n[ERROR] You're not registered!\n");
        }

        // --------------------------------------------------- LS and CD from CLIENT: do nothing
        else if (command[0].equalsIgnoreCase("LSC") || command[0].equalsIgnoreCase("CDC")) {
            if (command.length != 1) {
                out.writeUTF("\n[ERROR] LSC/CDC\n");
            }
        }

        else out.writeUTF("\n[ERROR] Wrong command!\n");
    }

    // checks if user exists "in database"
    private boolean find_user(String username, String password, File user_file) throws FileNotFoundException {
        try (Scanner scan = new Scanner(user_file)) {
            while (scan.hasNextLine()) {
                String[] line = scan.nextLine().split(",");
                if (line[0].equals(username) && line[1].equals(password)) {
                    System.out.println(line[2]);
                    setDirectory(line [2]);
                    return true;
                }
            }
        }
        return false;
    }


    // TCP

    // TODO: function that handles client password update--> must disconnect user after calling this function and authenticate him again
    private void modify_user_info(String username, String password, String userFilePath, int option, String command, DataOutputStream out) throws IOException {

        // command = password if option = 1
        // command = new dir if option = 2

        String dir = new File("").getAbsolutePath() + "/users.txt";
        Scanner sc = new Scanner(new File(dir));

        StringBuffer buffer = new StringBuffer();
        while (sc.hasNextLine()) buffer.append(sc.nextLine() + System.lineSeparator());
        String fileContent = buffer.toString();
        sc.close();

        String oldLine = username + "," + password + "," + userFilePath;
        String newLine = "";

        // change password
        if (option == 1) {
            setDirectory("clients/" + this.username + "/home");
            newLine = this.username + "," + command + "," + getDirectory();
            out.writeUTF("\n\n-------\nPassword changed!\n-------\n");
        }
        
        // change directory
        else if (option == 2){
            newLine = this.username + "," + this.password + "," + getDirectory();
        }

        // overwrite user info in users.txt
        fileContent = fileContent.replaceAll(oldLine, newLine);
        try (FileWriter writer = new FileWriter(dir)) {
            writer.append(fileContent);
            writer.flush();
        }
    }


    // returns list of files in current directory (aka $ls)
    private void listServerDirectory(DataOutputStream out) throws IOException {

        String dir = new File("").getAbsolutePath()+ "/" + getDirectory();
        File curDir = new File(dir);
    
		File[] filesList = curDir.listFiles();
        

        if (!curDir.isDirectory()) {
            out.writeUTF("\n[ERROR] Invalid directory!\n");
            return;
        }

        out.writeUTF("\n[server dir]: " + getDirectory());
        if (curDir.list().length == 0) out.writeUTF("Empty.");
		for (File f: filesList){
			if (f.isDirectory()) out.writeUTF("---- " + f.getName() + " (FOLDER)");
			if (f.isFile()) out.writeUTF("---- " +f.getName());
		}
		out.writeUTF("\n");
    }

    // handles remote directory navigation (aka $cd)
    private String changeServerDirectory(String curDir, String nextDir, DataOutputStream out) throws IOException{

        if (nextDir.equals("..")){
            File curDirFolder = new File(curDir);
            String previousFolder = curDirFolder.getParent();
            String[] folders = previousFolder.split("/");

            if (!folders[folders.length - 1].equals(this.username)){
                out.writeUTF("\n----------------\nNew [server] directory: " + previousFolder + "\n----------------\n");
                return previousFolder;
            }
            else {
                out.writeUTF("[ERROR] Already on home page!");
                return curDir;
            }
        }

        String dir = new File("").getAbsolutePath() + "/" + getDirectory();
        String newPath = dir + "/" + nextDir;
        File newPathDir = new File(newPath);

		if (!newPathDir.isDirectory()) out.writeUTF("\n[ERROR] Directory not found!\n");
		else {
            out.writeUTF("\n----------------\nNew [server] directory: " + getDirectory() + "/" + nextDir + "\n----------------\n");
			return getDirectory() + "/" + nextDir;
		}
		return curDir;
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