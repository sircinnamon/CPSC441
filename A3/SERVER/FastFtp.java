import java.util.Timer;
import java.net.*;
import java.io.*;
import java.util.TimerTask;

/**
 * FastFtp Class
 * 
 * FastFtp implements a basic FTP application based on UDP data transmission.
 * The main mehtod is send() which takes a file name as input argument and send the file 
 * to the specified destination host.
 * 
 */
public class FastFtp {
	
	public final boolean DEBUG = false;
	public int window;
	public int timeout;
	public DatagramSocket socket;
	public TxQueue queue;
	public boolean running = true;
	public Timer timer;
	public ReceiverThread receiver;
	public InetAddress ip;
	public int portNum;
	public Socket tcpSock;
    /**
     * Constructor to initialize the program 
     * 
     * @param windowSize	Size of the window for Go-Back_N (in segments)
     * @param rtoTimer		The time-out interval for the retransmission timer (in milli-seconds)
     */
	public FastFtp(int windowSize, int rtoTimer) {
		//
		// to be completed
		//
		window = windowSize;
		timeout = rtoTimer;
		queue = new TxQueue(window);

	}
	

    /**
     * Sends the specified file to the specified destination host:
     * 1. send file name and receiver server confirmation over TCP
     * 2. send file segment by segment over UDP
     * 3. send end of transmission over tcp
     * 3. clean up
     * 
     * @param serverName	Name of the remote server
     * @param serverPort	Port number of the remote server
     * @param fileName		Name of the file to be trasferred to the rmeote server
     */
	public void send(String serverName, int serverPort, String fileName) {
		// TCP connect to server
		// Send server the filename
		// Get response
		// Open a UDP
		// Divide file into segments
		// Send each segment (Send, wait for ACK, have timer running)
		// Send end transmission to server
		// Shut down
		if(!tcpConnect(serverName, serverPort, fileName)) //true if tcp steps are done
		{
			System.out.println("Error estalishing TCP connection.");
			System.exit(1);
		}
		try
		{
			//socket = new DatagramSocket(serverPort);
			socket = new DatagramSocket(tcpSock.getLocalPort());
			ip = InetAddress.getByName(serverName);
			portNum = serverPort;

			File file = new File(fileName);
			long filesize = file.length();
			FileInputStream fis = new FileInputStream(file);

			int bytesRead = 0;
			int seqNum = 0;

			receiver = new ReceiverThread(this, socket);
			receiver.start();

			
			//System.out.println("bytes read "+bytesRead);
			//System.out.println("filesize "+file.length());
			while(bytesRead < filesize)
			{
				Segment current = new Segment();
				if(DEBUG){System.out.println("Sending packet #"+seqNum);}
				byte[] data;
				if(filesize-bytesRead < current.MAX_PAYLOAD_SIZE)
				{
					data = new byte[(int)(filesize-bytesRead)];
				}
				else
				{
					data = new byte[current.MAX_PAYLOAD_SIZE];
				}
				fis.read(data);
				current.setPayload(data);
				current.setSeqNum(seqNum);
				seqNum++;
				bytesRead+= data.length;
				//System.out.println(current.toString());
				//wait for room in queue
				while(queue.isFull()){}
				processSend(current);
			}
			while(!queue.isEmpty()){}
			//send end of transmission msg
			endTransmission(serverName, serverPort);
			running = false;
			while(receiver.isAlive()){}
			socket.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public synchronized void processSend(Segment seg) throws IOException, InterruptedException
	{
		//ship it
		//System.out.println(seg.toString());
		if(DEBUG){System.out.println("ip/port: " + ip + "/" + portNum);}
		DatagramPacket pkt = new DatagramPacket(seg.getBytes(), seg.getBytes().length, ip, portNum);
		if(DEBUG){System.out.println(new Segment(pkt).toString());}
		socket.send(pkt);
		//add current to queue
		queue.add(seg);
		if(DEBUG){System.out.println("full queue: " + queueToString());}
		//if its the first one in queue (queue.length == 0) start timer
		if(queue.size() == 1)
		{
			startTimer();
		}
	}

	public synchronized void processAck(Segment ack) throws InterruptedException
	{
		//cancel timer
		endTimer();
		//remove anything with seq number lower than ack from queue
		if(DEBUG)
		{
			System.out.println("ACK: " + ack.getSeqNum());
			System.out.println("queue: " + queue.size());
			System.out.println("element: " + queue.element().getSeqNum());
			System.out.println("full queue: " + queueToString());
		}
		while(!queue.isEmpty() && queue.element().getSeqNum() < ack.getSeqNum())
		{
			queue.remove();
		}
		//if queue not empty, start new timer
		if(!queue.isEmpty())
		{
			startTimer();
		}
	}

	public synchronized void processTimeout() throws IOException
	{
		//get all segments in queue
		//resend them all
		//if queue is not empty, restart timer
		if(DEBUG){System.out.println("TIMEOUT");}
		endTimer();
		Segment[] pending = queue.toArray();
		for(Segment seg:pending)
		{
			if(DEBUG){System.out.println("Resending #"+seg.getSeqNum());}
			DatagramPacket pkt = new DatagramPacket(seg.getPayload(), seg.getPayload().length, ip, portNum);
			socket.send(pkt);
		}
		if(!queue.isEmpty())
		{
			startTimer();
		}
	}


	
    /**
     * A simple test driver
     * 
     */
	public static void main(String[] args) {
		int windowSize = 10; //segments
		int timeout = 100; // milli-seconds
		
		String serverName = "localhost";
		String fileName = "";
		int serverPort = 0;
		
		// check for command line arguments
		if (args.length == 3) {
			// either privide 3 paramaters
			serverName = args[0];
			serverPort = Integer.parseInt(args[1]);
			fileName = args[2];
		}
		else if (args.length == 2) {
			// or just server port and file name
			serverPort = Integer.parseInt(args[0]);
			fileName = args[1];
		}
		else {
			System.out.println("wrong number of arguments, try agaon.");
			System.out.println("usage: java FastFtp server port file");
			System.exit(0);
		}

		
		FastFtp ftp = new FastFtp(windowSize, timeout);
		
		System.out.printf("sending file \'%s\' to server...\n", fileName);
		ftp.send(serverName, serverPort, fileName);
		System.out.println("file transfer completed.");
	}

	private boolean tcpConnect(String serverName, int serverPort, String fileName)
	{
		try
		{
			tcpSock = new Socket(serverName, serverPort);
			DataInputStream in = new DataInputStream(tcpSock.getInputStream());
			DataOutputStream out = new DataOutputStream(tcpSock.getOutputStream());
			out.writeUTF(fileName);
			byte msg = -1;
			while((msg = in.readByte()) == -1){}

			return (msg == 0) ? true : false;
		}
		catch(Exception e)
		{
			System.out.println("TCP Socket Error:");
			e.printStackTrace();
		}
		return false;
	}
	
	private void endTransmission(String serverName, int serverPort)
	{
		try
		{
			DataOutputStream out = new DataOutputStream(tcpSock.getOutputStream());
			out.writeByte(0);
			tcpSock.close();
		}
		catch(Exception e)
		{
			System.out.println("TCP End Transmission Error:");
			e.printStackTrace();
		}
	}

	public void startTimer()
	{
		if(DEBUG){System.out.println("Timer starting...");}
		timer = new Timer(true);
		timer.schedule(new TimeoutHandler(this), (long)timeout);
	}

	public void endTimer()
	{
		timer.cancel();
	}
	
	public String queueToString()
	{
		String str = "[";
		Segment[] segs = queue.toArray();
		for(int i = 0; i<segs.length;i++)
		{
			if(segs[i]==null){str += "null";}
			else{str += segs[i].getSeqNum();}
			
			if(i!=segs.length-1){str+=",";}
		}
		return str+"]";
	}

}
