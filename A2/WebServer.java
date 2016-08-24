import java.net.ServerSocket;
import java.net.Socket;

public class WebServer 
{
	private int port;
	private boolean running;
	private ServerSocket mainSocket;
	private WebServerMaster master;
	/* @param a port number to use
	 * @returns
	 * @throws
	 */
	public WebServer(int inputPort)
	{
		//initialization
		port = inputPort;
	}
 	
 	/* @param
	 * @returns
	 * @throws
	 * start the server by creating the serversocket and starting it
	 */
	public void start(){
		// open the server socket 
		// create the master thread by passing the serve socket as its constructor argument 
		// start the master thread 	
		running = true;
		try
		{
			mainSocket = new ServerSocket(port);
			master = new WebServerMaster(mainSocket, this);
			master.start();
			//master.run();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	 
	 /* @param 
	 *  @returns
	 *  @throws
	 * Kills the running threads with some degree of safety
	 */
	public void stop()
	{
	 	// interrupt the master thread 
		// close the server socket
		running = false;
		try
		{
			master.killAll();
			master.shutDown();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
	}

	 /* @param 
	 *  @returns a boolean indicating if the server has been shut down
	 *  @throws
	 */
	public boolean getRunning()
	{
		return running;
	}
}