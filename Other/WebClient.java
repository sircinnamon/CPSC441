import java.io.File;
import java.net.*;
import java.io.InputStream;
import java.io.OutputStream;

public class WebClient 
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
	    WebClient c = new WebClient (args[0], Integer.parseInt(args[1]), debug);
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
    public WebClient (String ipaddress, int port, boolean debug)
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
		String outgoing;
		File file;
		String destination;
		String msg;

		try 
		{
		/* Try to connect to the specified host on the specified port. */
		    sock = new Socket (InetAddress.getByName(ipaddress), port);
		    in = sock.getInputStream();
		    out = sock.getOutputStream();

			/* Status info */
			System.out.println ("Connected to " + sock.getInetAddress().getHostAddress() + " on port " + port);

			//Send filename and get ack
			outgoing = "GET WebServer.java\n\n";
			System.out.print(outgoing);
		    out.write(outgoing.getBytes());
		    out.flush();

			while(in.available() < 40)
			{
				System.out.print((in.available()!=0)?in.available()+"\n":"");
			}
		    incoming = new byte[in.available()];
		    in.read(incoming);
			System.out.println(new String(incoming));

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
		catch(Exception e)
		{
			System.out.println(e);
			return;
		}
    }
}