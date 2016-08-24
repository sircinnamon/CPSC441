import java.net.Socket;
import java.io.FileInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
public class WebServerThread extends Thread
{
	private Socket socket;
	private int id;
	private WebServerMaster parent;
	private File file;

	/* @param a socket, an id number and a parent master
	 *  @returns 
	 *  @throws
	 */
	public WebServerThread(Socket sck, int threadId, WebServerMaster serv)
	{
		socket = sck;
		id = threadId;
		parent = serv;
	}
	/* @param
	 *  @returns 
	 *  @throws
	 * reads the get request, processes it and writes the appropriate reply
	 */
	public void run()
	{
		try
		{
			OutputStream out = socket.getOutputStream();
			InputStream in = socket.getInputStream();
			String request = readRequest(in);
			int code = verifyRequest(request);
			//done inside verifyRequest
			//file = new File(getPath(request));
			byte[] reply = createReply(file, code);
			out.write(reply);
			out.flush();
			socket.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/* @param an input stream from the client
	 *  @returns the request from the client as a string
	 *  @throws Exceptions if there are IO problems
	 */
	public String readRequest(InputStream in) throws Exception
	{
		//read lines until a fully blank line comes
		String request = "";
		String line = "";
		while(!line.equals("\n"))
		{
			line = readLine(in);
			request = request + line;
		}
		return request;
	}

	/* @param an input stream
	 *  @returns a String representing one input line from client
	 *  @throws Exception from IO errors
	 */
	public String readLine(InputStream in) throws Exception
	{	
		char inChar = 0;
		String line = "";
		while(inChar != '\n')
		{
			inChar = (char)in.read();
			line = line + inChar;
		}
		return line;
		
	}
	/* @param a request from a client
	 *  @returns the appropriate http code (200, 400, 404)
	 *  @throws
	 */
	public int verifyRequest(String request)
	{
		//if file is found and request is fine, 200
		//if file not found but request is fine, 404
		//else 400
		if(!request.split("\n")[0].startsWith("GET ")){return 400;}
		try
		{
			file = new File(getPath(request));
		}
		catch(Exception e)
		{
			return 400;
		}

		if(file.exists() && file.isFile())
		{
			return 200;
		}
		else
		{
			return 404;
		}
	}

	/* @param a get request
	 *  @returns the path to the file referenced
	 *  @throws
	 */
	public String getPath(String request)
	{
		return request.split("\n")[0].replace("GET ", "");
	}

	/* @param the file requested and the http code to reply with
	 *  @returns teh reply as a byte array
	 *  @throws
	 */
	public byte[] createReply(File file, int code)
	{
	/*  
		HOST: people.ucalgary.ca
		PATH: /~mghaderi/test/test.html
		PORT: 80
		Object people.ucalgary.ca/~mghaderi/test/test.html has not been modifed.
		HTTP/1.1 304 Not Modified
		Date: Sun, 18 Oct 2015 17:32:50 GMT
		Server: Apache/2.0.52 (Red Hat)
		Connection: close
		ETag: "2100454-23-c76efec0"
	*/
		String header = "HTTP/1.0 ";
		if(code == 404)
		{
			header += "404 Not Found\n";
			header += "Connection: Closed\n";
			header += "\n";
			return header.getBytes();
		}
		else if(code == 200)
		{
			header += "200 OK\n";
			header += "Content Length: "+ file.length() +"\n";
			header += "Connection: Closed\n";
			header += "\n";
			byte[] headerBytes = header.getBytes();
			byte[] full = new byte[headerBytes.length + (int)file.length()];
			System.arraycopy(headerBytes, 0, full, 0, headerBytes.length);
			try{
				new FileInputStream(file).read(full, (int)headerBytes.length, (int)file.length());
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			return full;

		}
		else if(code == 400)
		{
			header += "400 Bad Request\n";
			header += "Connection: Closed\n";
			header += "\n";
			return header.getBytes();
		}
		return null;
	}
	/* @param
	 *  @returns the socket of the thread
	 *  @throws
	 */
	public Socket getSocket()
	{
		return socket;
	}
}