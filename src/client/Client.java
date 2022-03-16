package client;

import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class Client {
    public static void main(String args[]) {
	// args[0] <- hostname of destination
	if (args.length == 0) {
	    System.out.println("java TCPClient hostname");
	    System.exit(0);
	}


    /* LEITURA DE FICHEIRO
    FileInputStream fis = new FileInputStream(new File(filename));
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



	Socket s = null;
	int serversocket = 7000;
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
		// READ STRING FROM KEYBOARD
		try {
		    texto = reader.readLine();
		} catch (Exception e) {
		}

		// WRITE INTO THE SOCKET
		out.writeUTF(texto);
		out.flush();

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
	}
    }
}

class MessageReader extends Thread {
	DataInputStream in;

	public MessageReader (DataInputStream in)  {
		this.in=in;
		this.start();
	}
	//=============================
    @Override
	public void run(){
		String resposta;
		try{
			while(true){
				// READ FROM SOCKET
				String data = in.readUTF();

				// DISPLAY WHAT WAS READ
				System.out.println("Received: " + data);
                
			}
		}catch(EOFException e){System.out.println("EOF:" + e);
		}catch(IOException e){System.out.println("IO:" + e);}
	}
}