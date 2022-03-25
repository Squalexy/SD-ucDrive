package client;

import java.net.*;
import java.io.*;
import java.util.ArrayList;

import javax.sound.midi.SysexMessage;

public class Client {

	private static final int serversocket = 7000;

	public static void main(String args[]) {
		// args[0] <- hostname of destination
		if (args.length == 0) {
			System.out.println("java TCPClient hostname");
			System.exit(0);
		}

		Socket s = null;
		Socket uploadSocket = null;
		Socket downloadSocket = null;

		try {
			// 1o passo
			s = new Socket(args[0], serversocket);

			System.out.println("SOCKET=" + s);
			// 2o passo
			DataInputStream in = new DataInputStream(s.getInputStream());
			DataOutputStream out = new DataOutputStream(s.getOutputStream());

			String texto = "";
			InputStreamReader input = new InputStreamReader(System.in);
			BufferedReader reader = new BufferedReader(input);
			new MessageReader(in);
			System.out.println("Introduza texto:");

			// 3o passo
			while (true) {

				commandMenu();

				// READ STRING FROM KEYBOARD
				try {
					texto = reader.readLine();
				} catch (Exception e) {
					System.out.println(e);
				}

				String[] command = texto.split(" ");
				if (command[0].equals("UP")) {

					// enviar comando ao servidor
					out.writeUTF(texto);

					// esperar resposta do servidor
					int port = in.readInt();
					uploadSocket = new Socket(args[0], port);
					DataInputStream upIn = new DataInputStream(uploadSocket.getInputStream());
					DataOutputStream upOut = new DataOutputStream(uploadSocket.getOutputStream());

					// meter o ficheiro num buffer para enviar ao servidor
					copyFileData(command[1], upOut);
					out.flush();
				}

				else if (command[0].equals("DW")) {

					// enviar comando ao servidor
					out.writeUTF(texto);

					// esperar resposta do servidor
					int port = in.readInt();
					downloadSocket = new Socket(args[0], port);
					DataInputStream inDownload = new DataInputStream(downloadSocket.getInputStream());
					// DataOutputStream outDownload = new DataOutputStream(downloadSocket.getOutputStream());

					// descarregar o ficheiro
					downloadFileData(command[1] ,inDownload);
				}

				else {
					// write into the socket
					out.writeUTF(texto);
					out.flush();
				}
			}

		} catch (UnknownHostException e) {
			System.out.println("Sock:" + e.getMessage());
			// nao conseguiu encontrar o hostname
		} catch (EOFException e) {
			System.out.println("EOF:" + e.getMessage());
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
		int offset = 0;
		int bufsize = 4096;
		byte[] buf = new byte[bufsize];

		try {
			do {
				nread = dis.read(buf);
				if (nread > 0) {
					fos.write(buf, offset, nread);
					offset += nread;
				}
			} while (nread > -1);
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} finally {
			fos.close();
		}
	}

	private static void commandMenu() {
		System.out.println("COMMAND FORMAT: OPCODE|ARGS");
		System.out.println("authentication: AU <username> <password>\n");
		System.out.println("modify password: PW <username> <new password>\n");
		System.out.println("list SERVER files: LSs\n");
		System.out.println("list CLIENT files: LSc\n");
		System.out.println("change SERVER directory: CDs <new directory>\n");
		System.out.println("change CLIENT directory: CDc <new directory>\n");
		System.out.println("download file: DN <filename>\n");
		System.out.println("upload file: UP <filename>");
	}
}

class MessageReader extends Thread {
	DataInputStream in;

	public MessageReader(DataInputStream in) {
		this.in = in;
		this.start();
	}

	// =============================
	@Override
	public void run() {
		String resposta;
		try {
			while (true) {
				// READ FROM SOCKET
				String data = in.readUTF();

				// DISPLAY WHAT WAS READ
				System.out.println("Received: " + data);

			}
		} catch (EOFException e) {
			System.out.println("EOF:" + e);
		} catch (IOException e) {
			System.out.println("IO:" + e);
		}
	}
}

/*
 * HEARTBEATS
 * 
 * 
 * InetAdress ia = InetAdress.getByName("localhost");
 * try (DatagramSocket ds = new DatagramSocket()){
 * ds.setSoTimeout(1);
 * int failedheartbeats = 0;
 * white (failedheartbeats < maxfailedrounds){
 * try{
 * ByteArrayOutputStream baos = new ByteArrayOutputStream();
 * DataInputStream dos = new DataInputStream(baos);
 * dos.writeIt(count++);
 * byte[] buf = baos.toByteArray();
 * 
 * DatagramPacket dp = new DatagramPacket(buf, buf.length, ia, 4000);
 * ds.send(dp);
 * 
 * 
 * byte [] rbuf = new byte[4096];
 * DatagramPacket dr = new DatagramPacket(rbuf, rbuf.length);
 * 
 * ds.receive(dr);
 * failedheartbeats = 0;
 * 
 * }
 * }
 * }
 */