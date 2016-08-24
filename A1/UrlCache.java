import java.io.BufferedReader;
//import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.io.FileReader;
/**
 * UrlCache Class
 * 
 * @author 	Majid Ghaderi
 * @version	1.0, Sep 22, 2015
 *
 */
public class UrlCache {

    public final int DEFAULT_PORT_NUM = 80;
    public final String HTTP_VERSION = "HTTP/1.1";
    public final String CATALOG_PATH = "cache/catalog";
    public final boolean DEBUG = false; //set true for debug messages to be printed
    public UrlCacheNode head;

    /**
     * Default constructor to initialize data structures used for caching/etc
	 * If the cache already exists then load it. If any errors then throw exception.
	 *
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public UrlCache() throws UrlCacheException
    {
        File cacheDirectory = new File("cache");
        if(!cacheDirectory.exists() || !cacheDirectory.isDirectory())
        {
            cacheDirectory.mkdir();
        }
        //check if a catalog file exists
        File catalog = new File(CATALOG_PATH);
        if(catalog.exists())
        {
            head = initializeCatalog(catalog);
        }
    }
	
    /**
     * Downloads the object specified by the parameter url if the local copy is out of date.
	 *
     * @param url	URL of the object to be downloaded. It is a fully qualified URL.
     * @throws UrlCacheException if encounters any errors/exceptions
     */
	public void getObject(String url) throws UrlCacheException
    {
        int portNum = DEFAULT_PORT_NUM;
        Socket socket;
        //Check if url includes a port number - if so remove and record
        if(url.matches(".*:[0-9]+.*"))
        {
            portNum = extractPortNum(url);
            url = url.replaceAll(":[0-9]+", "");
        }
        //Split out the host and path names
        String host = getHost(url);
        String path = getPath(url);
        if(DEBUG)
        {
            System.out.println("HOST: " + host + "\nPATH: " + path + "\nPORT: " + portNum);
        }
        try
        {
            //create a socket connection to host and IO streams
            socket = new Socket(host, portNum);
            PrintWriter  toServer = new PrintWriter(socket.getOutputStream(), true);
            //BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            InputStream fromServer = socket.getInputStream();
            String httpReturnMsg = "";
            if(isInCatalog(url))
            {
                //If file found in catalog, use conditional get
                toServer.print("GET " + path + " " + HTTP_VERSION + "\n");
                toServer.print("Host: "+ host + "\n");
                toServer.print("If-Modified-Since: " + findNode(url).getFormattedLastModified()+"\n\n");
                toServer.flush();
            }
            else
            {
                //or else use regular get
                toServer.print("GET " + path + " " + HTTP_VERSION + "\n");
                toServer.print("Host: "+ host + "\n\n");
                toServer.flush();
            }
            //get the HTTP header
            String httpLine;
            httpLine = readLine(fromServer);
            while(!httpLine.equals("") && httpLine != null)
            {
                httpReturnMsg = httpReturnMsg + httpLine + "\n";
                httpLine = readLine(fromServer);
            }
            //parse HTTP header according to HTTP code returned
            if(httpReturnMsg.split("\n")[0].contains("200 OK"))
            {
                //Get the important attributes and create a byte array for data
                int dataSize = parseContentLength(httpReturnMsg);
                String lastModified = parseLastModified(httpReturnMsg);
                byte[] data = new byte[dataSize];
                int bytesRead = 0;
                int readCounter;
                //pull from output stream until its the same size as HTTP header claimed
                while(bytesRead < dataSize)
                {
                    while((readCounter = fromServer.read(data, bytesRead, (dataSize-bytesRead))) != -1)
                    {
                        bytesRead = bytesRead + readCounter;
                        if(bytesRead == dataSize)
                        {
                            break;
                        }
                    }
                }
                //save data to a file in the cache
                String filename = writeToFile(url, data);
                //Add new object to the catalog
                if(head == null)
                {
                    head = new UrlCacheNode(url, lastModified, filename);
                }
                else
                {
                    head.append(new UrlCacheNode(url, lastModified, filename));
                }
                //save catalog to file
                updateCatalog();
            }
            else if(httpReturnMsg.split("\n")[0].contains("304 Not Modified"))
            {
                if(DEBUG){System.out.println("Object " + url + " has not been modifed.");}
            }
            else
            {
                throw new UrlCacheException("Bad HTTP code: " + httpReturnMsg.split("\n")[0]);
            }
            //Parse http header to get status code, size, and last modified
            //if not modified code, all good
            //if OK code, get object and update
            //else error out

            //make byte array of size(-header size?) and fill with remaining data
            //print bytes out to file
            if(DEBUG){System.out.println(httpReturnMsg);}

            socket.close();
        }
        catch(Exception ex)
        {
            System.out.println(ex.toString());
        }  
    }
	
    /**
     * Returns the Last-Modified time associated with the object specified by the parameter url.
	 *
     * @param url 	URL of the object 
	 * @return the Last-Modified time in millisecond as in Date.getTime()
     * @throws UrlCacheException if the specified url is not in the cache, or there are other errors/exceptions
     */
	public long getLastModified(String url) throws UrlCacheException
    {
        UrlCacheNode object = findNode(url);
        return object.getLastModified().getTime();
    }

    //@return the head of a linked list constructed from data in catalog file
    //@param a file object representing the catalog 
    public UrlCacheNode initializeCatalog(File catalog)
    {
        UrlCacheNode head = null;
        try(BufferedReader reader = new BufferedReader(new FileReader(catalog)))
        {
            String urlLine ="";
            String dateLine = "";
            String cacheFileLine = "";
            
            while ((urlLine != null) && (dateLine != null) && (cacheFileLine != null))
            {
                urlLine = reader.readLine();
                dateLine = reader.readLine();
                cacheFileLine = reader.readLine();
                if(DEBUG){System.out.println(urlLine+"\n"+dateLine+"\n"+cacheFileLine);}
                if((urlLine == null)||(dateLine == null) || (cacheFileLine == null))
                {
                    break;
                }
                if(!reader.readLine().equals(""))
                {

                    throw new UrlCacheException("catalog file is corrupt.");
                }

                if(head == null)
                {
                    head = new UrlCacheNode(urlLine, dateLine, cacheFileLine);
                }
                else
                {
                    head.getTail().setNext(new UrlCacheNode(urlLine, dateLine, cacheFileLine));
                }
            }
            reader.close();
        }
        catch(Exception ex)
        {
            System.out.println("Error while initializing catalog");
            System.out.println(ex.toString());
        }
        return head;
    }
    //writes the linked list out to CATALOG_PATH
    public void updateCatalog() throws Exception
    {
        File catalog = new File(CATALOG_PATH);
        catalog.createNewFile();
        PrintWriter writer = new PrintWriter(catalog);
        writer.print(head.recursiveToString());
        writer.flush();
        writer.close();
    }
    //@param a Url to look for in catalog
    //@return true if object at given url has been downloaded and saved to cache
    public boolean isInCatalog(String url)
    {
        UrlCacheNode current = head;
        if(head == null)
        {
            return false;
        }
        while(!current.getUrl().equals(url))
        {
            current = current.getNext();
            if(current == null)
            {
                return false;
            }
        }
        return true;
    }
    //@param a url that is in linked list 
    //@return the node representing the onject at that url
    //Assumes object does exist - throws exception otherwise
    public UrlCacheNode findNode(String url) throws UrlCacheException
    {
        UrlCacheNode current = head;
        while(!current.getUrl().equals(url))
        {
            current = current.getNext();
            if(current == null)
            {
                throw new UrlCacheException("File not in catalog");
            }
        }
        return current;
    }

    //@param a url includes a port number
    //@return that number
    public int extractPortNum(String url) throws UrlCacheException
    {
        url = url.split(":")[1];
        url = url.split("/")[0];
        return Integer.parseInt(url);
    }

    //@param a url
    //@return the host portion of a given url
    public String getHost(String url)
    {
        url = url.replace("http[s]+://", "");
        return url.split("/")[0];
    }
    //@param a url
    //@return the path portion of a given url
    public String getPath(String url)
    {
        url = url.replace("http[s]+://", "");
        return "/" + url.split("/",2)[1];
    }

    //@param an HTTP header
    //extracts the content length line and converts to an int
    //@return content length as an int
    public int parseContentLength(String httpMsg) throws UrlCacheException
    {
        String[] httpLines = httpMsg.split("\n");
        for(int i = 0; i < httpLines.length; i++)
        {
            if(httpLines[i].contains("Content-Length:"))
            {
                String contentLength = httpLines[i].replaceAll("[^0-9]", "");
                if(DEBUG){System.out.println("CONTENT LENGTH: " + contentLength);}
                return Integer.parseInt(contentLength);
            }
        }
        throw new UrlCacheException("Bad HTTP message: No Content-Length found.");
    }

    //@param an HTTP header
    //extracts the "last modified" line
    //@return Last modified date
    public String parseLastModified(String httpMsg) throws UrlCacheException
    {
        String[] httpLines = httpMsg.split("\n");
        for(int i = 0; i < httpLines.length; i++)
        {
            if(httpLines[i].contains("Last-Modified:"))
            {
                if(DEBUG){System.out.println("LAST MOD: " + httpLines[i].replace("Last-Modified: ", "").trim());}
                return httpLines[i].replace("Last-Modified: ", "").trim();
            }
        }
        throw new UrlCacheException("Bad HTTP message: No Last-Modified found.");
    }
    //@param an input stream from a socket
    //Reads bytes from an input stream as chars until finding a newline character
    //@return all found chars as a String
    public String readLine(InputStream input) throws Exception
    {
        String str = "";
        char current = (char)input.read();
        while(current != '\n')
        {
            str = str + current;
            current = (char)input.read();
        }
        str = str.replaceAll("\r", "");
        str = str.replaceAll("\n", "");
        //if(DEBUG){System.out.println("READLINE:" + str + "\nLENGTH: " + str.length());}
        return str;
    }
    //@param an object URL and the data of the object creates a file in the cache
    //and stores the given data
    //generates a filename by replacing "/" in url with "-"
    //@return filename
    public String writeToFile(String url, byte[] data) throws Exception
    {
        File file = new File("cache/"+url.replaceAll("/","-"));
        FileOutputStream fileOut = new FileOutputStream(file);
        fileOut.write(data);
        fileOut.flush();
        return file.getPath();
    }

}