import java.util.TimerTask;
//Riley Lahd
//10110724
//06/11/15
class TimeoutHandler extends TimerTask {

	public FastFtp parent;

	/**
	 * @param the parent FastFtp obj
	 * @return void
	 * @throws none
	 */
	public TimeoutHandler(FastFtp ftp)
	{
		parent = ftp;
		//need this?
		//run();
	}

	/**
	 * @param none
	 * @return void
	 * @throws none
	 * 
	 * tell parent to process the timeout
	 */
	public void run()
	{
		try
		{
			parent.processTimeout();
		}
		catch(Exception e)
		{
			System.out.println("Error in TimeoutHandler");
			e.printStackTrace();
		}
	}
}
