import java.util.TimerTask;
//Riley Lahd
//10110724
//06/11/15
class BroadcastTimer extends TimerTask {

	public Router parent;

	/**
	 * @param the parent FastFtp obj
	 * @return void
	 * @throws none
	 */
	public BroadcastTimer(Router r)
	{
		parent = r;
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
			if(parent.active)
			{
				parent.broadcast();
			}
		}
		catch(Exception e)
		{
			System.out.println("Error in TimeoutHandler");
			e.printStackTrace();
		}
	}
}
