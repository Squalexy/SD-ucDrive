import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Connection extends Thread {

    DataInputStream in;
    DataOutputStream out;
    ArrayList<Connection> connections;
    Socket clientSocket;
    int thread_number;
    File usersFile;
    String username = "";
    String password;
    String directory;
    String otherServerAddress;
    int fileSyncPort;

    public Connection(Socket aClientSocket, int numero, ArrayList<Connection> connections, String otherServerAddress, int fileSyncPort) {
        thread_number = numero;
        try {
            clientSocket = aClientSocket;
            this.connections = connections;
            this.otherServerAddress = otherServerAddress;
            this.fileSyncPort = fileSyncPort;
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

    // =============================
    @Override
    public void run() {

        // LOOP
        try {
            while (true) {
                // an echo server
                String data = in.readUTF();
                System.out.println("T[" + thread_number + "] Received: " + data);

                // servidor trata do pedido do cliente aqui
                parseCommand(data);

                // DEBUGGING ------------------------------------------------------
                // System.out.println("Username: " + this.username);
                // System.out.println("Password: " + this.password);
                // System.out.println("Directory: " + this.directory);
                // DEBUGGING ------------------------------------------------------
            }
        } catch (EOFException e) {
            System.out.println("\n---------------\nUser disconnected\n---------------\n");
            this.connections.remove(this);
        } catch (IOException e) {
            System.out.println("IO:" + e);
            this.connections.remove(this);
        }
    }

    // command parsing function
    private void parseCommand(String data) throws IOException {

        String[] command = data.split(" ");

        usersFile = new File("users.txt");

        // --------------------------------------------------- CLIENT AUTHENTICATION
        if (command[0].equalsIgnoreCase("AU")) {
            if (command.length != 3) {
                out.writeUTF("\n[ERROR] AU <username> <password>\n");
                out.flush();
                return;
            }

            if (this.username.isEmpty()) {
                if (find_user(command[1], command[2], usersFile)) {
                    this.username = command[1];
                    this.password = command[2];
                    out.writeUTF("\n\n-------\nWelcome " + this.username + ", you are now authenticated!\n-------\n");
                    out.flush();
                } else
                    out.writeUTF("\n[ERROR] Wrong username or password!\n");
                out.flush();
            }

            else if (this.username.equals(command[0]) || !this.username.isEmpty()) {
                out.writeUTF("\n[ERROR] You're already logged in!\n");
                out.flush();
            }

        }

        // --------------------------------------------------- MODIFY PASSWORD
        else if (command[0].equalsIgnoreCase("PW")) {

            if (command.length != 3) {
                out.writeUTF("\n[ERROR] PW <password> <new password>\n");
                out.flush();
                return;
            }

            if (!this.username.isEmpty()) {
                if (this.password.equals(command[1])) {
                    modify_user_info(this.username, this.password, this.directory, 1, command[2], out);
                    username = "";
                    password = "";
                }

                else {
                    out.writeUTF("\n[ERROR] Current password invalid, please try again.\n");
                    out.flush();
                }
            }

            else {
                out.writeUTF("\n[ERROR] You're not registered!\n");
                out.flush();
            }
        }

        // --------------------------------------------------- LIST REMOTE DIRECTORY
        else if (command[0].equalsIgnoreCase("LSS"))

        {

            if (command.length != 1) {
                out.writeUTF("\n[ERROR] LSS\n");
                out.flush();
                return;
            }

            if (!this.username.isEmpty()) {
                listServerDirectory(out);
            } else
                out.writeUTF("\n[ERROR] You're not registered!\n");
            out.flush();
        }

        // --------------------------------------------------- CHANGE REMOTE DIRECTORY
        else if (command[0].equalsIgnoreCase("CDS")) {
            if (command.length != 2) {
                out.writeUTF("\n[ERROR] CDS <new directory>\n");
                out.flush();
                return;
            }

            if (!this.username.isEmpty()) {
                String oldDir = getDirectory();
                setDirectory(changeServerDirectory(oldDir, command[1], out));
                modify_user_info(this.username, this.password, oldDir, 2, command[1], out);
            } else
                out.writeUTF("\n[ERROR] You're not registered!\n");
            out.flush();
        }

        // --------------------------------------------------- DOWNLOAD FROM REMOTE DIRECTORY
        else if (command[0].equalsIgnoreCase("DN")) {

            System.out.println("Entrou no DN");

            if (command.length != 2) {
                out.writeUTF("\n[ERROR] DN <filename>\n");
                out.flush();
                return;
            }

            if (!this.username.isEmpty()) {
                downloadFile(command[1], out, this.otherServerAddress);
            } else
                out.writeUTF("\n[ERROR] You're not registered!\n");
            out.flush();
        }

        // --------------------------------------------------- UPLOAD TO REMOTE DIRECTORY
        else if (command[0].equalsIgnoreCase("UP")) {
            if (command.length != 2) {
                out.writeUTF("\n[ERROR] UP <filename>\n");
                out.flush();
                return;
            }

            if (!this.username.isEmpty()) {
                uploadFile(command[1], this.otherServerAddress);
            } else
                out.writeUTF("\n[ERROR] You're not registered!\n");
            out.flush();
        }

        else
            out.writeUTF("\n[ERROR] Wrong command!\n");
        out.flush();
    }

    // checks if user exists "in database"
    private boolean find_user(String username, String password, File user_file) throws FileNotFoundException {
        try (Scanner scan = new Scanner(user_file)) {
            while (scan.hasNextLine()) {
                String[] line = scan.nextLine().split(",");
                if (line[0].equals(username) && line[1].equals(password)) {
                    System.out.println(line[2]);
                    setDirectory(line[2]);
                    return true;
                }
            }
        }
        return false;
    }

    // function that handles client password update--> must disconnect user
    // after calling this function and authenticate him again
    private void modify_user_info(String username, String oldPassword, String userFilePath, int option, String command,
            DataOutputStream out) throws IOException {

        // command = password if option = 1
        // command = new dir if option = 2

        String dir = new File("").getAbsolutePath() + "/users.txt";
        Scanner sc = new Scanner(new File(dir));

        StringBuffer buffer = new StringBuffer();
        while (sc.hasNextLine())
            buffer.append(sc.nextLine() + System.lineSeparator());
        String fileContent = buffer.toString();
        sc.close();

        String oldLine = username + "," + oldPassword + "," + userFilePath;
        String newLine = "";

        // change password
        if (option == 1) {
            setDirectory("clients/" + this.username + "/home");
            newLine = this.username + "," + command + "," + getDirectory();
            out.writeUTF("\n\n-------\nPassword changed!\n-------\nPlease, authenticate again...\n");
        }

        // change directory
        else if (option == 2) {
            newLine = this.username + "," + this.password + "," + getDirectory();
        }

        // overwrite user info in users.txt
        fileContent = fileContent.replaceAll(oldLine, newLine);
        try (FileWriter writer = new FileWriter(dir)) {
            writer.append(fileContent);
            writer.flush();
        }
        new FileSyncSender(fileSyncPort, "users.txt", otherServerAddress);
    }

    // returns list of files in current directory (aka $ls)
    private void listServerDirectory(DataOutputStream out) throws IOException {

        String dir = new File("").getAbsolutePath() + "/" + getDirectory();
        File curDir = new File(dir);

        File[] filesList = curDir.listFiles();

        if (!curDir.isDirectory()) {
            out.writeUTF("\n[ERROR] Invalid directory!\n");
            out.flush();
            return;
        }

        StringBuilder display = new StringBuilder();
        display.append("\n~~~~~~~~~~~~~~~~~~~~~~~\n[remote dir]: " + getDirectory());

        if (curDir.list().length == 0) {
            display.append("\nEmpty.");
            out.writeUTF(display.toString());
            out.flush();
            return;
        }

        for (File f : filesList) {
            if (f.isDirectory()) {
                display.append("\n---- " + f.getName() + " (FOLDER)");

            }
            if (f.isFile()) {
                display.append("\n---- " + f.getName());
            }
        }
        out.writeUTF(display.toString() + "\n~~~~~~~~~~~~~~~~~~~~~~~\n");
        out.flush();

    }

    // handles remote directory navigation (aka $cd)
    private String changeServerDirectory(String curDir, String nextDir, DataOutputStream out) throws IOException {

        if (nextDir.equals("..")) {
            File curDirFolder = new File(curDir);
            String previousFolder = curDirFolder.getParent();
            String[] folders = previousFolder.split("/");

            if (!folders[folders.length - 1].equals(this.username)) {
                out.writeUTF("\n----------------\nNew [server] directory: " + previousFolder + "\n----------------\n");
                out.flush();
                return previousFolder;
            } else {
                out.writeUTF("\n[ERROR] Already on home page!");
                out.flush();
                return curDir;
            }
        }

        String dir = new File("").getAbsolutePath() + "/" + getDirectory();
        String newPath = dir + "/" + nextDir;
        File newPathDir = new File(newPath);

        if (!newPathDir.isDirectory()) {
            out.writeUTF("\n[ERROR] Directory not found!\n");
            out.flush();
        } else {
            out.writeUTF("\n----------------\nNew [server] directory: " + getDirectory() + "/" + nextDir
                    + "\n----------------\n");
            out.flush();
            return getDirectory() + "/" + nextDir;
        }
        return curDir;
    }

    // function that handles file download
    private void downloadFile(String filename, DataOutputStream out, String otherServerAddress) throws IOException {

        int fileExists = 0;

        String dir = new File("").getAbsolutePath() + "/" + getDirectory();

        try (ServerSocket listenSocket = new ServerSocket(0)) {

            int port = listenSocket.getLocalPort();

            File curDir = new File(dir);
            File[] filesList = curDir.listFiles();
            for (File f : filesList) {
                if (f.isFile() && f.getName().equals(filename)) {
                    fileExists = 1;
                }
            }

            if (fileExists == 1) {
                
                out.writeInt(port);
                Socket downloadSocket = listenSocket.accept(); // BLOQUEANTE
                FileInputStream fis = new FileInputStream(new File(dir + "/" + filename));
                new FileDownload(downloadSocket, fis);
                out.flush();
            } else {
                out.writeInt(0);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // function that handles file upload
    private void uploadFile(String filename, String otherServerAddress) {
        String dir = new File("").getAbsolutePath() + "/" + getDirectory();

        try (ServerSocket listenSocket = new ServerSocket(0)) {
            int port = listenSocket.getLocalPort();
            out.writeInt(port);

            Socket uploadSocket = listenSocket.accept(); // BLOQUEANTE
            File uploaded = new File(dir + "/" + filename);
            uploaded.createNewFile();
            FileOutputStream fos = new FileOutputStream(uploaded);
            new FileUpload(uploadSocket, fos, getDirectory() + "/" + filename, otherServerAddress, this.fileSyncPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
