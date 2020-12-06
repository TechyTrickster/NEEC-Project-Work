import java.io.*;
import java.util.Scanner;
import java.util.LinkedList;
import java.util.Random;
//this program should be passed the name of the CSV database to use.
//need to implement a binary search, instead of a linear search,  2 million ip ranges is too large for linear!
public class Geolocate3
{
    Scanner input;
    LinkedList<IPRange> data;
    RangeBucket[] buckets;
	float minLatitude;
	float maxLatitude;
	float minLongitude;
	float maxLongitude;

    public static void main(String args[])
    {
    	String databaseName = args[0];
    	if(args[1].equals("-range"))
    	{
    		float minLat = Float.parseFloat(args[2]);
    		float maxLat = Float.parseFloat(args[3]);
    		float minLon = Float.parseFloat(args[4]);
    		float maxLon = Float.parseFloat(args[5]);
    		Geolocate3 batch = new Geolocate3(databaseName, minLat, maxLat, minLon, maxLon);
    	}
    	else if(args[1].equals("-ip"))
    	{
            	Geolocate3 service = new Geolocate3(databaseName);
            	while(true)  {service.takeRequest();}
    	}
        else if(args[1].equals("-area"))
        {
            Geolocate3 service = new Geolocate3(databaseName);
            while(true)
            {
                service.takeRequest();
                service.printRange();
            }
        }
    }



	public Geolocate3(String database, float mLatitude, float MLatitude, float mLongitude, float MLongitude)
	{
		this(database);
		System.out.println("finding addresses in the range of:");
		System.out.println("min Lat:" + mLatitude + " max Lat: " + MLatitude + " min Lon: " + mLongitude + " max Lon: " + MLongitude);
        int nodeID = 0;
        Random generator = new Random();
		for(RangeBucket container: buckets)
		{
			LinkedList<IPRange> temp = container.searchInArea(mLatitude, MLatitude, mLongitude, MLongitude);
			for(IPRange element: temp)
			{
                int working = 1;  if(generator.nextFloat() > 0.99)  {working = 0;}
                System.out.println(nodeID + "," + generator.nextFloat() + "," + working + "," + "Building" + "," + element.getLocation().getLat() + "," + element.getLocation().getLon() + "," + element.getLocation().getEle() + "," + "," + "," + "," + "," + "1" + "," + "Ethernet");
				System.out.println(element.getLocation().toStringIL() + " " + element.getIPLowString());
                nodeID++;
			}
		}
	}



    public Geolocate3(String n)
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
        {
            if(temp.getLat() < minLatitude)
            {minLatitude = temp.getLat();}
            else if(temp.getLat() > maxLatitude)
            {maxLatitude = temp.getLat();}
            else if(temp.getLon() < minLongitude)
            {minLongitude = temp.getLon();}
            else if(temp.getLon() > maxLongitude)
            {maxLongitude = temp.getLon();}

            System.out.println(IP + "," + temp.toStringIL());
        }
        else
        {System.out.println(IP + "," + "null,null,null");}
    }


    private void printRange()
    {
        System.out.println("minimum latitude: " + minLatitude + " minimum longitude:" + minLongitude);
        System.out.println("maximum latitude: " + maxLatitude + " maximum longitude:" + maxLongitude);
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


	public int getRange()  {return(range);}
	public int getIPLow()  {return(IPlow);}

    public String getIPLowString()
    {
        int internalCopy = IPlow;
        String one = Integer.toString((internalCopy % 256) & 0xFF);
        internalCopy = internalCopy >> 8;
        String two = Integer.toString((internalCopy % 256) & 0xFF);
        internalCopy = internalCopy >> 8;
        String three = Integer.toString((internalCopy % 256) & 0xFF);
        internalCopy = internalCopy >> 8;
        String four = Integer.toString((internalCopy % 256) & 0xFF);
        internalCopy = internalCopy >> 8;
        return(four + "." + three + "." + two + "." + one);
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

	public LinkedList<IPRange> searchInArea(float mLat, float MLat, float mLon, float MLon)
	{ //returns IP ranges that fall within the location parameters given
		LinkedList<IPRange> output = new LinkedList<IPRange>();
		for(LinkedList<IPRange> group: set)
		{
			for(IPRange element: group)
			{
				Geolocation loc = element.getLocation();
				float lat = loc.getLat();
				float lon = loc.getLon();
				if(((lat >= mLat) && (lat <= MLat)) && ((lon >= mLon) && (lon <= MLon)))
				{output.add(element);}
			}
		}
		return(output);
	}


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
