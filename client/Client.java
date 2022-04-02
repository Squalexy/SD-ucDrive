import java.net.*;
import java.io.*;

public class Client {

	private static int serversocket = 7000;

	public static void main(String args[]) {
		if (args.length == 0) {
			System.out.println("java TCPClient hostname");
			System.exit(0);
		}

		Socket s = null;
		Socket uploadSocket = null;
		Socket downloadSocket = null;
		File curDir = new File(".");

		boolean connectAgain = false;
		

		while (true){

			try {
				s = new Socket(args[0], serversocket);
				
				// System.out.println("SOCKET=" + s);

				commandMenu();

				if (connectAgain){
					System.out.println("\n:::: Please, connect again ::::\n");
					connectAgain = false;
				}

				DataInputStream in = new DataInputStream(s.getInputStream());
				DataOutputStream out = new DataOutputStream(s.getOutputStream());

				String texto = "";
				InputStreamReader input = new InputStreamReader(System.in);
				BufferedReader reader = new BufferedReader(input);

				int authenticated = 0;

				// new MessageReader(in);

				while (true) {

					// READ STRING FROM KEYBOARD
					try {
						System.out.print("> ");
						texto = reader.readLine();
					} catch (Exception e) {
						System.out.println(e);
					}

					String data = "";
					String[] command = texto.split(" ");

					// --------------------------------------------------- LIST LOCAL DIRECTORY
					if (command[0].equalsIgnoreCase("LSC")) {

						if (command.length != 1) {
							System.out.println("\n[ERROR] LSC\n");
							continue;
						}

						if (authenticated == 0){
							System.out.println("\n[ERROR] You're not registered!\n");
							continue;
						}

						listClientDirectory(curDir);
					}

					// --------------------------------------------------- CHANGE LOCAL DIRECTORY
					else if (command[0].equalsIgnoreCase("CDC")) {

						if (command.length != 2) {
							System.out.println("\n[ERROR] CDC <new directory>\n");
							continue;
						}

						if (authenticated == 0){
							System.out.println("\n[ERROR] You're not registered!\n");
							continue;
						}

						curDir = changeClientDirectory(curDir, command[1]);
					}

					// --------------------------------------------------- SHOW MENU
					else if (command[0].equalsIgnoreCase("MENU")) {

						if (command.length != 1) {
							System.out.println("\n[ERROR] MENU\n");
							continue;
						}

						commandMenu();
					} 

					// --------------------------------------------------- UPLOAD
					else if (authenticated == 1 && command[0].equalsIgnoreCase("UP")) {

						if (command.length != 2){
							System.out.println("\n[ERROR] UP <filename>\n");
							continue;
						}

						int fileExists = 0;
						String dir = new File("").getAbsolutePath() + "/";
						File currentDir = new File(dir);

						File[] filesList = currentDir.listFiles();
						for (File f : filesList) {
							if (f.isFile() && f.getName().equals(command[1])) {
								fileExists = 1;
							}
						}	

						if (fileExists == 1) {
							out.writeUTF(texto);
							// esperar resposta do servidor
							int port = in.readInt();
							uploadSocket = new Socket(args[0], port);
							// DataInputStream upIn = new DataInputStream(uploadSocket.getInputStream());
							DataOutputStream upOut = new DataOutputStream(uploadSocket.getOutputStream());

							// meter o ficheiro num buffer para enviar ao servidor
							System.out.println("\nUploading file...");
							copyFileData(curDir + "/" + command[1], upOut);
							System.out.println("[SUCCESS] Upload Finished!\n");
							out.flush();
						}

						else {
							System.out.println("\n[ERROR] File doesn't exist\n");
				
						}
					}

					// --------------------------------------------------- DOWNLOAD
					else if (authenticated == 1 && command[0].equalsIgnoreCase("DN")) {
						
						if (command.length != 2){
							System.out.println("\n[ERROR] DN <filename>\n");
							continue;
						}
						
						out.writeUTF(texto);
						
						// esperar resposta do servidor;
						int port = in.readInt();

						if (port != 0){
							downloadSocket = new Socket(args[0], port);
							DataInputStream inDownload = new DataInputStream(downloadSocket.getInputStream());
							// DataOutputStream outDownload = new
							// DataOutputStream(downloadSocket.getOutputStream());

							// descarregar o ficheiro
							System.out.println("\nDownloading...");
							downloadFileData(curDir + "/" + command[1], inDownload);
							System.out.println("[SUCCESS] Download Finished!\n");
						}

						else {
							System.out.println("\n[ERROR] File doesn't exist!\n");
						}
					}

					else{
						out.writeUTF(texto);
						out.flush();
						data = in.readUTF();
						System.out.println(data);
						
						if (command.length > 1 && data.equals("\n\n-------\nWelcome " + command[1] + ", you are now authenticated!\n-------\n")){
							authenticated = 1;
						}
						else if (data.equals("\n\n-------\nPassword changed!\n-------\nPlease, authenticate again...\n")){
							authenticated = 0;
						}
					}
				}

			} catch (UnknownHostException e) {
				System.out.println("Sock:" + e.getMessage());
				// nao conseguiu encontrar o hostname
			} catch (EOFException e) {
				System.out.println("Server went down!");
			} catch (ConnectException e){
				if(serversocket == 7000){
					serversocket = 6000;
				}
				else if(serversocket == 6000){
					serversocket = 7000;
				}
				System.out.println("Trying to connect to server...");
				connectAgain = true;
			} catch (IOException e) {
				System.out.println("IO:" + e.getMessage());
			} finally {
				if (s != null)
					try {
						s.close();
					} catch (IOException e) {
						System.out.println("close:" + e.getMessage());
					}

				if (uploadSocket != null)
					try {
						uploadSocket.close();
					} catch (IOException e) {
						System.out.println("close:" + e.getMessage());
					}
				if (downloadSocket != null)
					try {
						downloadSocket.close();
					} catch (IOException e) {
						System.out.println("close:" + e.getMessage());
					}
			}
		}

	}

	// this method is for copying data to send to server for UPLOAD
	private static void copyFileData(String fileToUpload, DataOutputStream dos) throws IOException {

		InputStream fis = null;

		try {
			fis = new FileInputStream(new File(fileToUpload));
			byte[] buf = new byte[4096];
			int bytesRead;

			do {
				bytesRead = fis.read(buf);
				if (bytesRead > 0)
					dos.write(buf, 0, bytesRead);
			} while (bytesRead > -1);
		}

		catch (Exception e) {
			System.out.println(e);
		}

		finally {
			fis.close();
			dos.close();
		}
	}

	private static void downloadFileData(String fileToDownload, DataInputStream dis) throws IOException {
		FileOutputStream fos = new FileOutputStream(fileToDownload);
		int nread;
		int bufsize = 4096;
		byte[] buf = new byte[bufsize];

		try {
			do {
				nread = dis.read(buf);
				if (nread > 0) {
					fos.write(buf, 0, nread);
				}
			} while (nread > -1);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			fos.close();
		}
	}

	private static void listClientDirectory(File curDir) {

		File[] filesList = curDir.listFiles();

		if (!curDir.isDirectory()) {
			System.out.println("\n[ERROR] Invalid directory!\n");
			return;
		}

		System.out.println("\n~~~~~~~~~~~~~~~~~~~~~~~\n[local dir]: " + curDir.getName());
		if (curDir.list().length == 0)
			System.out.println("Empty.");
		for (File f : filesList) {
			if (f.isDirectory())
				System.out.println("---- " + f.getName() + " (FOLDER)");
			if (f.isFile())
				System.out.println("---- " + f.getName());
		}
		System.out.println("~~~~~~~~~~~~~~~~~~~~~~~\n");
	}

	private static File changeClientDirectory(File curDir, String nextDir) {

		if (nextDir.equals("..")) {
			String previousFolder = curDir.getParent();
			if (!curDir.getName().equals(".")) {
				File previousPath = new File(new File("").getAbsolutePath() + "/" + previousFolder);
				System.out.println(
						"\n----------------\nNew [local] directory: " + previousFolder + "\n----------------\n");
				return previousPath;
			} else {
				System.out.println("\n[ERROR] Already on source folder!\n");
				return curDir;
			}
		}

		File newPath = new File(curDir + "/" + nextDir);
		String newPathDir = curDir + "/" + nextDir;

		if (!newPath.isDirectory())
			System.out.println("\n[ERROR] Directory not found!\n");
		else {
			System.out.println("\n----------------\nNew [local] directory: " + newPathDir + "\n----------------\n");
			return newPath;
		}
		return curDir;
	}

	private static void commandMenu() {
		System.out.println("\n---------------------------------------------------------");
		System.out.println("COMMAND FORMAT: OPCODE <ARGS>\n");
		System.out.println("[authentication] AU <username> <password>");
		System.out.println("[modify password] PW <old password> <new password>");
		System.out.println("[list SERVER files] LSS");
		System.out.println("[list CLIENT files] LSC");
		System.out.println("[change SERVER directory] CDs <new directory>");
		System.out.println("[change CLIENT directory] CDc <new directory>");
		System.out.println("[download file] DN <filename>");
		System.out.println("[upload file] UP <filename>");
		System.out.println("[show menu again] MENU");
		System.out.println("---------------------------------------------------------\n");
	}
}