
/**
 * A simple test driver
 * 
 * @author 	Majid Ghaderi
 * @version	3.0, Oct 2, 2015
 *
 */
 
import java.util.*;
 
public class Tester {
	
	public static void main(String[] args) {
		
		// default server port
		// port numbers < 1024 are generally reserved by the OS
		int serverPort = 2225;
		
		Scanner keyboard = new Scanner(System.in);
		
		// user can specify an alternative port number
		if (args.length == 1) {
			serverPort = Integer.parseInt(args[0]);
		}
		
		// start the server
		// stop the server when user types "quit"
		WebServer server = new WebServer(serverPort);
		System.out.println("Server starting...");
		server.start();
		System.out.println("Server started. Type \"quit\" to close.");

		while ( !keyboard.next().equals("quit") );
		
		server.stop();
		System.out.println("Server stopped.");
	}
	
}
