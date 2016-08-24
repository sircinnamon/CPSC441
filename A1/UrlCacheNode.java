import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
public class UrlCacheNode {
	private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
    private String fullUrl;
    private UrlCacheNode next;
    private Date lastModified;
    private String cacheFile;

    //Constructs a node using a URL a Last-Modified date and a Filename where the data is stored
    public UrlCacheNode(String inputUrl, String inputLastModified, String inputCacheFile) throws UrlCacheException
    {
    	DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    	try
    	{
    		fullUrl = inputUrl;
    		//Convert HTTP formatted String to a Date object
    		lastModified = DATE_FORMAT.parse(inputLastModified);
    		cacheFile = inputCacheFile;
    	}
    	catch(Exception ex)
    	{
    		throw new UrlCacheException("Problem initializing UrlCacheNode");
    	}
    }
    //@return a String representation of this nodes data
    public String toString()
    {
    	return fullUrl+"\n"+DATE_FORMAT.format(lastModified)+"\n"+cacheFile+"\n\n";
    }
    //@return a String representation of this node and all following nodes
    public String recursiveToString()
    {
    	return toString() + ((next!=null)? next.recursiveToString() : "");
    }
    public String getUrl()
    {
    	return fullUrl;
    }
    public String getCacheFile()
    {
    	return cacheFile;
    }
    public Date getLastModified()
    {
    	return lastModified;
    }
    //@return last modified as an HTTP formatted String
    public String getFormattedLastModified()
    {
    	return DATE_FORMAT.format(lastModified);
    }
    public UrlCacheNode getNext()
    {
    	return next;
    }
    public void setNext(UrlCacheNode node)
    {
    	next = node;
    }
    //@return the end of the linked list - i.e. the first node that has no "next" node
    public UrlCacheNode getTail()
    {
    	if(next == null)
    	{
    		return this;
    	}
    	else
    	{
    		UrlCacheNode current = next;
    		while(current.getNext() != null)
    		{
    			current = current.getNext();
    		}
    		return current;
    	}
    }
    //Add a node to the end of the linked list
    //@param node to be added
    public void append(UrlCacheNode node)
    {
    	getTail().setNext(node);
    }
}