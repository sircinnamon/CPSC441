import java.net.*;
import java.io.InputStream;
//Riley Lahd
//10110724
//06/11/15
public class ReceiverThread extends Thread
{
	public final int DATAGRAM_SIZE = 8+4;
	public final int TIMEOUT = 1000;
	public FastFtp parent;
	public DatagramSocket socket;

	/**
	 * @param The parent FastFtp obj and the UDP socket to listen on
	 * @return A RecieverThread
	 * @throws none
	 */
	public ReceiverThread(FastFtp ftp, DatagramSocket sock)
	{
		parent = ftp;
		socket = sock;
		try
		{
		socket.setSoTimeout(TIMEOUT);
		}
		catch(Exception e)
		{
			System.out.println("Error setting socket timeout.");
		}
	}

	/**
	 * @param none
	 * @return void
	 * @throws none
	 * 
	 * Listens for ACKs and informs parent when they come
	 */
	public void run()
	{
		//System.out.println("Reciever thread running");

		while (parent.running)
		{
			//read from input stream
			//if an ack comes in
			
			//System.out.println("Looking for ack...");
			byte[] data = new byte[DATAGRAM_SIZE];
			DatagramPacket pkt = new DatagramPacket(data, DATAGRAM_SIZE);
			try
			{
				socket.receive(pkt);
				parent.processAck(new Segment(pkt));
			}
			catch(SocketTimeoutException e)
			{
				//DO nothing
			}
			catch(Exception e)
			{
				System.out.println("Error in ReceiverThread");
				e.printStackTrace();
			}
			
		}
	}
}
