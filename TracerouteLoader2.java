import java.util.LinkedList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.IOException;


public class TracerouteLoader2
{
	BufferedReader in;
	//LinkedList<NetworkNode> nodes;
	NetworkNodeCollection nodes;
	Geolocate2 locator;


	public static void main(String args[])
	{
		TracerouteLoader2 test = new TracerouteLoader2();
		test.loadAddresses();
	}



	public TracerouteLoader2()
	{
		in = new BufferedReader(new InputStreamReader(System.in));
		//nodes = new LinkedList<NetworkNode>();
		nodes = new NetworkNodeCollection();
		locator = new Geolocate2("GeoLite2-City-Blocks-IPv4-Adjusted.csv");
	}



	public TracerouteLoader2(String fileName) throws IOException
	{
		in = new BufferedReader(new FileReader(fileName));
		//nodes = new LinkedList<NetworkNode>();
		nodes = new NetworkNodeCollection();
		locator = new Geolocate2("GeoLite2-City-Blocks-IPv4-Adjusted.csv");
	}



	public AssetModel[] generateModels()
	{
		int index = 0;
		int nulls = 0;
		int outputSize = 0;
		LinkedList<NetworkNode> temp = nodes.getList();
		for(NetworkNode element: temp)  {if((element == null) || (element.getLoc() == null))  {nulls++;}}
		outputSize = temp.size() - nulls;
		System.out.println("node list size: " + temp.size());
		System.out.println("output list size: " + outputSize);
		AssetModel[] output = new AssetModel[outputSize];
		for(NetworkNode element: temp)
		{
			if(element != null)
			{
				if(element.getLoc() != null)
				{
					output[index] = new AssetModel(element);
					index++;
				}
			}
		}
		return(output);
	}



	public void loadAddresses()
	{
		String line = "";
		int lineCount = 0;
		try
		{
			while((line = in.readLine()) != null)
			{
				if(lineCount % 1000 == 0)  {System.out.println("line: " + lineCount);}
				lineCount++;
				try
				{
					if(!line.equals(""))
					{
						if(line.split(":").length > 1)
						{
							//line = in.readLine();
							String[] data = line.split(":");
							//System.out.println(line);
							NetworkNode lastNode = null;
						
							for(String element: data)
							{
								if(!element.equals("*"))
								{
									//System.out.println(element);
									String[] interior = element.split(" ");
									if(interior.length == 1)
									{
										lastNode = new NetworkNode(interior[0]);
										addNode(lastNode);
									}
									if(interior.length == 2)
									{
										if(!interior[1].equals("!X")  && !interior[1].equals("!H")  && !interior[1].equals("!N"))
										{
											NetworkNode temp = new NetworkNode(interior[0]);
											lastNode.addConnection(temp, Float.parseFloat(interior[1]));
											addNode(temp);
											lastNode = temp;
											//System.out.println("adding");
										}
									}
								}
							}
						}
					}
				}
				catch(Exception e) {e.printStackTrace(); System.out.println(lineCount);}  //fail silently, just don't process the line that caused the error
			}
		}  catch(IOException x)  {System.exit(-1);} //just fail
	}



	public void addNode(NetworkNode n)
	{
		if(!alreadyLoaded(n))
		{nodes.add(n);  n.setLoc(locator.search(n.getIP()));}
		else
		{
			//System.out.println(n.getIP() + " already exists!");
		}
	}


	public boolean alreadyLoaded(NetworkNode n)
	{return(nodes.contains(n.getIP()));}
}




class NetworkNode
{
	String IP;
	LinkedList<Link> connections;
	Geolocation location;

	public NetworkNode(String n)
	{
		IP = n;
		connections = new LinkedList<Link>();
	}


	public LinkedList<Link> getConnections()  {return(connections);}
	public String getIP()  {return(IP);}
	public boolean match(String ip)  {return(IP.equals(ip));}
	public boolean equals(NetworkNode n)  {return(n.getIP().equals(IP));}
	protected void addLink(Link l)  {connections.add(l);}


	public boolean hasLink(NetworkNode n)
	{
		boolean hasLink = false;
		for(Link element: connections)
		{
			if(element.contains(this, n))
			{
				hasLink = true;
				break;
			}
		}
		return(hasLink);
	}



	public Link getContainingLink(NetworkNode n)
	{
		for(Link element: connections)
		{
			if(element.contains(this, n))
			{return(element);}
		}
		return(null);
	}



	public void addConnection(NetworkNode n, float latency)
	{
		boolean localLink = hasLink(n);
		boolean otherLink = n.hasLink(n);

		if(localLink && otherLink)  {getContainingLink(n).addLatency(latency);}
		else if(localLink)
		{
			Link l = getContainingLink(n);
			l.addLatency(latency);
			n.addLink(l);
		}
		else if(otherLink)
		{
			Link l = n.getContainingLink(n);
			l.addLatency(latency);
			addLink(l);
		}
		else
		{
			Link l = new Link(this, n, latency);
			connections.add(l);
		}
	}


	public Geolocation getLoc()  {return(location);}
	public void setLoc(Geolocation input)  {location = input;}
}



class Link  //this won't work without making sure everything is copy by reference, instead of making copies of the objects
{
	float latency;
	float total;
	int count;
	NetworkNode connection0;
	NetworkNode connection1;

	public Link(NetworkNode n0, NetworkNode n1, float l)
	{
		latency = l;
		count = 1;
		connection0 = n0;
		connection1 = n1;
	}


	public NetworkNode getC0()  {return(connection0);}
	public NetworkNode getC1()  {return(connection1);}
	public void addLatency(float input)  {count++; total+=input; latency = total / count;}
	public boolean contains(NetworkNode c0, NetworkNode c1)  {return(((c0 == connection0) && (c1 == connection1)) || ((c0 == connection1) && (c1 == connection0)));}
}






class NetworkNodeCollection
{
	NetworkNodeSubCollection[] nodes;
	int size;
	public NetworkNodeCollection()
	{
		nodes = new NetworkNodeSubCollection[(int) Math.pow(2, 16)];
		size = 0;
	}


	public static int getHigh(String IP)
	{
		String[] values = IP.split("\\.");
		int output = (int) Math.pow(Integer.parseInt(values[0]), 2) + Integer.parseInt(values[1]);
		return(output);
	}


	public static int getLow(String IP)
	{
		String[] values = IP.split("\\.");
		int output = (int) Math.pow(Integer.parseInt(values[2]), 2) + Integer.parseInt(values[3]);
		return(output);
	}


	public boolean contains(String IP)
	{
		boolean output = false;
		int high = getHigh(IP);
		if(nodes[high] != null)
		{output = nodes[high].contains(IP);}
		return(output);
	}


	public boolean contains(NetworkNode thing)  {return(contains(thing.getIP()));}


	public void add(NetworkNode thing)
	{
		int high = getHigh(thing.getIP());
		if(nodes[high] == null)
		{nodes[high] = new NetworkNodeSubCollection();}
		size += nodes[high].add(thing);
	}


	public void add(String IP)
	{
		int high = getHigh(IP);
		if(nodes[high] == null)
		{nodes[high] = new NetworkNodeSubCollection();}
		size += nodes[high].add(IP);
	}


	public NetworkNode get(String IP)  {return(nodes[getHigh(IP)].get(IP));}

	public int size()  {return(size);}

	public LinkedList<NetworkNode> getList()
	{
		LinkedList<NetworkNode> output = new LinkedList<NetworkNode>();
		for(int index = 0; index < nodes.length; index++)
		{
			//System.out.print(index + ":" + nodes.length + " ");
			if(nodes[index] != null)
			{
				NetworkNode[] work = nodes[index].getArray();
				for(int lex = 0; lex < work.length; lex++)
				{output.add(work[lex]);}
			}
		}
		return(output);
	}
}




class NetworkNodeSubCollection
{
	NetworkNode[] nodes;
//	int range;

	public NetworkNodeSubCollection()
	{
//		range = r;
		nodes = new NetworkNode[(int) Math.pow(2, 16)];
	}


//	public int getRange()  {return(range);}

	public int add(String IP)
	{
		int low = NetworkNodeCollection.getLow(IP);
		if(!contains(IP))
		{
			nodes[low] = new NetworkNode(IP);
			return(1);
		}
		return(0);
	}


	public int add(NetworkNode n)
	{
		int low = NetworkNodeCollection.getLow(n.getIP());
		if(!contains(n.getIP()))
		{
			nodes[low] = n;
			return(1);
		}
		return(0);
	}


	public boolean contains(String IP)  {return(nodes[NetworkNodeCollection.getLow(IP)] != null);}

	public NetworkNode get(String IP) {return(nodes[NetworkNodeCollection.getLow(IP)]);}

	public NetworkNode[] getArray()  {return(nodes);}
}
