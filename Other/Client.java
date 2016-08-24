import java.io.*;
import java.net.*;
import java.io.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.interfaces.*;
import java.math.*;
import java.security.SecureRandom;
import java.util.Scanner;

/**
 * Client program.  Connects to the server and sends text accross.
 * Modified by Riley Lahd - 10110724
 */

public class Client 
{
    private Socket sock;  //Socket to communicate with.
	
    /**
     * Main method, starts the client.
     * @param args args[0] needs to be a hostname, args[1] a port number.
     */
    public static void main (String [] args)
    {
    boolean debug = false;
	if (args.length != 2 && args.length != 3) {
	    System.out.println ("Usage: java Client hostname port# (optional: debug)");
	    System.out.println ("hostname is a string identifying your server");
	    System.out.println ("port is a positive integer identifying the port to connect to the server");
	    return;
	}

	try {
		if(args.length == 3 && args[2].toLowerCase().equals("debug"))
		{
			debug = true;
		}
		else if (args.length == 3)
		{
			throw new IllegalArgumentException("Invalid Argument Format");
		}
	    Client c = new Client (args[0], Integer.parseInt(args[1]), debug);
	}
	catch (NumberFormatException e) {
	    System.out.println ("Usage: java Client hostname port# (optional: debug)");
	    System.out.println ("Second argument was not a port number");
	    return;
	}
	catch (IllegalArgumentException e) {
    System.out.println (e);
    return;
	}
    }
	
    /**
     * Constructor, in this case does everything.
     * @param ipaddress The hostname to connect to.
     * @param port The port to connect to.
     */
    public Client (String ipaddress, int port, boolean debug)
    {

		//STEPS TO RUN
		//1: recieve session key encrypted and hashed with seeded key
		//2: Send FIlename wait for ack (Decrypt and verify)
		//3: Send filesize wait for ack (Decrypt and verify)
		//4: Send data and wait for ack (Decrypt and verify)
		//6: Await final Ack and shutdown
		
		InputStream in;
		OutputStream out;
		byte[] incoming = null;
		byte[] outgoing;
		byte[] seed;
		byte[] fileData;
		File file;
		String destination;
		SecretKey sessionKey;
		SecretKey seededKey;
		String msg;

		try 
		{
			//Get seed, source filename and destination filename
			seed = getInput("Enter a seed: ").getBytes();
			seededKey = CryptoUtil.genSeededKey(seed);
			if(debug){System.out.println("SEEDEDKEY: " + CryptoUtil.toHexString(seededKey.getEncoded()));}
			file = new File(getInput("Enter filename to send: "));
			if(!file.exists() || !file.isFile())
			{
				throw new Exception("Entered file does not exist locally");
			}
			fileData = readFile(file);
			destination = getInput("Enter destination file name: ");
			
		/* Try to connect to the specified host on the specified port. */
		    sock = new Socket (InetAddress.getByName(ipaddress), port);
		    in = sock.getInputStream();
		    out = sock.getOutputStream();

			/* Status info */
			if(debug){System.out.println ("Connected to " + sock.getInetAddress().getHostAddress() + " on port " + port);}

			//Step 1: recieve session key
			while(in.available() < 48)
			{
				//System.out.print((in.available()!=0)?in.available()+"\n":"");
			}
		    incoming = new byte[in.available()];
		    in.read(incoming);
		    if(debug){System.out.println("SERVER: " + CryptoUtil.toHexString(incoming));}
			sessionKey = CryptoUtil.buildSessionKey(CryptoUtil.unwrapMsgBytes(incoming, seededKey));
			if(debug){System.out.println("SERVER: " + CryptoUtil.toHexString(sessionKey.getEncoded()));}

			//Send filename and get ack
			outgoing = CryptoUtil.wrapMsg(("FILENAME: " + destination).getBytes(), sessionKey);
		    if(debug){System.out.println("CLIENT: " + CryptoUtil.toHexString(outgoing));}
		    out.write(outgoing);
		    out.flush();

		    while(in.available() < 32)
		    {
				//System.out.print((in.available()!=0)?in.available()+"\n":"");
			}
		    incoming = new byte[in.available()];
		    in.read(incoming);
		    if(debug){System.out.println("SERVER: " + CryptoUtil.toHexString(incoming));}
			msg = CryptoUtil.unwrapMsg(incoming, sessionKey);
			if(debug){System.out.println("SERVER: " + msg);}
			if(!msg.equals("ACK FILENAME"))
			{
				throw new Exception("Invalid ack");
			}

			//Send filesize and get ack
			outgoing = CryptoUtil.wrapMsg(("FILESIZE: " + file.length()).getBytes(), sessionKey);
		    if(debug){System.out.println("CLIENT: " + CryptoUtil.toHexString(outgoing));}
		    out.write(outgoing);
		    out.flush();

		    while(in.available() < 32)
		    {
				//System.out.print((in.available()!=0)?in.available()+"\n":"");
			}
		    incoming = new byte[in.available()];
		    in.read(incoming);
		    if(debug){System.out.println("SERVER: " + CryptoUtil.toHexString(incoming));}
			msg = CryptoUtil.unwrapMsg(incoming, sessionKey);
			if(debug){System.out.println("SERVER: " + msg);}
			if(!msg.equals("ACK FILESIZE"))
			{
				throw new Exception("Invalid ack");
			}

			//Send filedata and get ack
			outgoing = CryptoUtil.wrapMsg(fileData, sessionKey);
		    if(debug){System.out.println("CLIENT: " + CryptoUtil.toHexString(outgoing));}
		    out.write(outgoing);
		    out.flush();

		    while(in.available() < 32)
		    {
				//System.out.print((in.available()!=0)?in.available()+"\n":"");
			}
		    incoming = new byte[in.available()];
		    in.read(incoming);
		    if(debug){System.out.println("SERVER: " + CryptoUtil.toHexString(incoming));}
			msg = CryptoUtil.unwrapMsg(incoming, sessionKey);
			if(debug){System.out.println("SERVER: " + msg);}
			if(!msg.equals("ACK DATA"))
			{
				throw new Exception("Invalid ack");
			}
			in.close();
			out.close();
			System.exit(0);

		}
		catch (UnknownHostException e) 
		{
		    System.out.println ("Usage: java Client hostname port#");
		    System.out.println ("First argument is not a valid hostname");
		    return;
		}
		catch (IOException e) 
		{
		    System.out.println ("Could not connect to " + ipaddress + ".");
		    return;
		}
		catch(Exception e)
		{
			System.out.println(e);
			return;
		}
    }

    private String getInput(String msg)
    {
    	Scanner in = new Scanner(System.in);
    	System.out.print(msg);
    	return in.nextLine();
    }

    private byte[] readFile(File file) throws IOException, FileNotFoundException
    {
    	FileInputStream in = new FileInputStream(file);
    	byte[] data = new byte[(int)file.length()];
    	in.read(data);
    	return data;
    }
}