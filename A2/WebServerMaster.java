import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
public class WebServerMaster extends Thread
{
	private ServerSocket socket;
	private WebServer parent;
	private final int MAX_THREADS = 64;
	private WebServerThread[] threads = new WebServerThread[MAX_THREADS];

	 /* @param a server socket to run and a parent WebServer
	 *  @returns
	 *  @throws
	 */
	public WebServerMaster(ServerSocket sck, WebServer serv)
	{
		socket = sck;
		parent = serv;
	}
	 /* @param 
	 *  @returns
	 *  @throws
	 *  tries to accept connections, when one arrives spawn a WebServerThread
	 */
	public void run()
	{
		try
		{
			while(parent.getRunning())
			{
				Socket connect = socket.accept();
				int id = getIndex();
				threads[id] = new WebServerThread(connect, id, this);
				//threads[id].run();
				threads[id].start();
			}
		}
		catch(java.net.SocketException e)
		{
			//do nothing?
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}

	}

	 /* @param 
	 *  @returns an unused thread id
	 *  @throws ArrayIndexOutOfBoundsException if a new index cannot be found
	 */
	public int getIndex()throws ArrayIndexOutOfBoundsException
	{
		//go through array and overwrite any killed threads or empty spots
		for(int i = 0; i<threads.length; i++)
		{
			if(threads[i] == null)
			{
				return i;
			}
			else if(!threads[i].isAlive())
			{
				return i;
			}
		}
		throw new ArrayIndexOutOfBoundsException();
	}
	/* @param 
	 *  @returns 
	 *  @throws Exception from shutDown
	 * closes sockets of any living threads
	 */
	public void killAll() throws Exception
	{
		for(int i = 0; i<threads.length; i++)
		{
			if(threads[i] == null)
			{
				//do nothing
			}
			else if(threads[i].isAlive())
			{
				threads[i].getSocket().close();
			}
		}
	}
	/* @param 
	 *  @returns 
	 *  @throws Exception if socket closing is unsuccessful
	 * closes server socket
	 */
	public void shutDown() throws Exception
	{
		socket.close();
	}
	/* @param 
	 *  @returns boolean indicating if server is running 
	 *  @throws 
	 */
	public boolean getRunning()
	{
		return parent.getRunning();
	}
}