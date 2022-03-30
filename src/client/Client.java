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

		while (true){

			try {
				s = new Socket(args[0], serversocket);
				s.setSoTimeout(1000 * 5);
				// System.out.println("SOCKET=" + s);

				commandMenu();

				DataInputStream in = new DataInputStream(s.getInputStream());
				DataOutputStream out = new DataOutputStream(s.getOutputStream());

				String texto = "";
				InputStreamReader input = new InputStreamReader(System.in);
				BufferedReader reader = new BufferedReader(input);

				// new MessageReader(in);

				while (true) {

					// READ STRING FROM KEYBOARD
					try {
						texto = reader.readLine();
					} catch (Exception e) {
						System.out.println(e);
					}

					String data;
					String[] command = texto.split(" ");

					if (command[0].equals("UP")) {

						if (command.length != 2) {
							System.out.println("\n[ERROR] UP <filename>\n");
							continue;
						}

						// enviar comando ao servidor
						out.writeUTF(texto);

						// esperar resposta do servidor
						int port = in.readInt();
						uploadSocket = new Socket(args[0], port);
						// DataInputStream upIn = new DataInputStream(uploadSocket.getInputStream());
						DataOutputStream upOut = new DataOutputStream(uploadSocket.getOutputStream());

						// meter o ficheiro num buffer para enviar ao servidor
						copyFileData(curDir + "/" + command[1], upOut);
						System.out.println("\n[SUCCESS] Upload Finished!\n");
						out.flush();
					}

					else if (command[0].equalsIgnoreCase("DN")) {

						if (command.length != 2) {
							System.out.println("\n[ERROR] DN <filename>\n");
							continue;
						}

						// enviar comando ao servidor
						out.writeUTF(texto);

						// esperar resposta do servidor;
						int port = in.readInt();
						downloadSocket = new Socket(args[0], port);
						DataInputStream inDownload = new DataInputStream(downloadSocket.getInputStream());
						// DataOutputStream outDownload = new
						// DataOutputStream(downloadSocket.getOutputStream());

						// descarregar o ficheiro
						downloadFileData(curDir + "/" + command[1], inDownload);
						System.out.println("\n[SUCCESS] Download Finished!\n");
						out.flush();
					}

					else if (command[0].equalsIgnoreCase("LSC")) {

						if (command.length != 1) {
							System.out.println("\n[ERROR] LSC\n");
							continue;
						}

						listClientDirectory(curDir);
					}

					else if (command[0].equalsIgnoreCase("CDC")) {

						if (command.length != 2) {
							System.out.println("\n[ERROR] CDC <new directory>\n");
							continue;
						}

						curDir = changeClientDirectory(curDir, command[1]);
					}

					else if (command[0].equalsIgnoreCase("MENU")) {

						if (command.length != 1) {
							System.out.println("\n[ERROR] MENU\n");
							continue;
						}

						commandMenu();
					} else {
						// write into the socket
						out.writeUTF(texto);
						out.flush();
						data = in.readUTF();
						System.out.println(data);
					}
				}

			} catch (UnknownHostException e) {
				System.out.println("Sock:" + e.getMessage());
				// nao conseguiu encontrar o hostname
			} catch (EOFException e) {
				System.out.println("Server went down!");
				serversocket = 6000;
			} catch (ConnectException e){
				System.out.println("Trying to connect to server...");
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

		System.out.println("\n\n[local dir]: " + curDir.getName());
		if (curDir.list().length == 0)
			System.out.println("Empty.");
		for (File f : filesList) {
			if (f.isDirectory())
				System.out.println("---- " + f.getName() + " (FOLDER)");
			if (f.isFile())
				System.out.println("---- " + f.getName());
		}
		System.out.println();
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
			System.out.println("Directory not found!");
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
		System.out.println("[modify password] PW <username> <new password>");
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