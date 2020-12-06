import java.io.*;
import java.util.Scanner;
import java.util.LinkedList;
//this program should be passed the name of the CSV database to use.
//need to implement a binary search, instead of a linear search,  2 million ip ranges is too large for linear!
public class Geolocate2
{
    Scanner input;
    LinkedList<IPRange> data;
    RangeBucket[] buckets;

    public static void main(String args[])
    {
        Geolocate2 service = new Geolocate2(args[0]);
        while(true)  {service.takeRequest();}
    }



    public Geolocate2(String n)
    {
	System.out.println("starting");
        int linesRead = 0;
	buckets = new RangeBucket[256];
	for(int index = 0; index < 256; index++)  {buckets[index] = new RangeBucket(index);}  //initialize
	System.out.println("allocated data structures");
        try
        {
            Scanner fileInput = new Scanner(new File(n));
            String dumpFirstLine = fileInput.nextLine();
            input = new Scanner(System.in);
            //data = new LinkedList<IPRange>();
	    System.out.println("gotten file handles");
            while(fileInput.hasNextLine())
            {
		//System.out.println("looping");
		String line = "";
                try
                {
                    line = fileInput.nextLine();
		    String IP = line.split("/")[0];
		    int fb = getFirstByte(IP);  //System.out.println(fb);
		    IPRange temp = new IPRange(line);
		    buckets[fb].addRange(temp, line);
                    linesRead++;
                }
                catch(NumberFormatException e)  {} //{System.out.println(line);  e.printStackTrace();}  //just don't read that line
            }
            fileInput.close();
        }
        catch(Exception e)  {e.printStackTrace();  System.exit(-1);}
        System.out.println("ranges loaded: " + linesRead);
    }


    public static int getFirstByte(String ip)  {return(Integer.parseInt(ip.split("\\.")[0]));}



    public void waitForInput()
    {
        try  {while(!input.hasNext()) {Thread.sleep(1);}}
        catch(Exception e)  {e.printStackTrace(); System.exit(-1);}  //this should never fail, but if it does, report why and exit
    }


    public void takeRequest()
    {
        waitForInput();
        String IP = input.nextLine();
        Geolocation temp = search(IP);
        if(temp != null)
        {System.out.println(IP + "," + temp.toStringIL());}
        else
        {System.out.println(IP + "," + "null,null,null");}
    }


    public Geolocation search(String IP)
    {
	int fb = getFirstByte(IP);
	return(buckets[fb].searchForValue(IP));  //didn't find anything
    }
}





class IPRange
{
    int IPlow;
    int IPhigh;

    int range;
    Geolocation position;

    public IPRange(String input) throws Exception
    {
        String[] data = input.split(",");
        IPlow     = getIPlow(data[0]);
        IPhigh    = IPlow + generateVariation(data[0]);
        float latitude  = Float.parseFloat(data[7]);
        float longitude = Float.parseFloat(data[8]);
        float elevation = 50;
        position = new Geolocation(latitude, longitude, elevation);
        range    = Integer.parseInt(data[9]);
    }



    private static int getIPlow(String ipRange)
    {
        String[] IPbase = ipRange.split("/");
        String[] values = IPbase[0].split("\\.");
        int output = 0;
        //System.out.println(ipRange);
        for(String element: values)
        {

            output = output << 8;
            //System.out.println(Byte.parseByte(element));
            output = output + Integer.parseInt(element);
        }
        return(output);
    }


    private int generateVariation(String ipRange)
    {
        //System.out.println(ipRange);
        String[] IPbase = ipRange.split("/");
        int variation = (int) Math.pow(2, 32 - Integer.parseInt(IPbase[1]));
        return(variation);
    }



    public boolean matches(String IP)
    {
        int ip = getIPlow(IP);
        //System.out.println(ip + "," + IPlow + "," + IPhigh);
        return((ip >= IPlow) && (ip <= IPhigh));
    }


    public Geolocation getLocation()  {return(position);}
}




class RangeBucket
{
	int firstByte;
	LinkedList<IPRange>[] set;
	public RangeBucket(int r)
	{
		firstByte = r;
		set = new LinkedList[256];
		for(int index = 0; index < 256; index++)  {set[index] = new LinkedList<IPRange>();}
	}


	public void addRange(IPRange data, String ip)  {set[getSB(ip)].add(data);}
	public int getFB()  {return(firstByte);}
	private static int getSB(String IP)  {return(Integer.parseInt(IP.split("\\.")[1]));}


	public Geolocation searchForValue(String ip)
	{
//		System.out.println(ip);
		int sb = getSB(ip);
		LinkedList<IPRange> group = set[sb];
		for(IPRange element: group)
		{if(element.matches(ip))  {return(element.getLocation());}}
		return(null);
	}
}
