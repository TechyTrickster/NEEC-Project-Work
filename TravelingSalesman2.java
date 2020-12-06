import java.util.LinkedList;
import java.util.Random;
import java.util.Arrays;
import java.time.Instant;
import java.io.PrintWriter;
import java.io.File;
import java.util.ListIterator;
//this mihgt only make sense when the cost of the repair crew is much greater than the cost of down time, making roving repair crews impossible.
/* need to integrate crew wage and vehical operation cost*/
public class TravelingSalesman2
{
	long counter;
	float hourlyCrewWage;
	float vehicalOperationCost;  //how much does it cost to operate their vehical per mile
	AssetModel pointsOfInterest[];
	Geolocation startingPoint;
	Geolocation endingPoint;
	int arrayLength;
	long secondsInAnHour;
	float crewSpeed; //crew speed is in miles per hour
	final long theTime;  //UNIX TIMESTAMP of when the simulations begin
	Random generator;

	public static void main(String args[])
	{
		try
		{
			//generate constructor data
			Random gen = new Random();
			Geolocation start = new Geolocation(1,2,3);
			Geolocation end = new Geolocation(3,2,1);
			int size = Integer.parseInt(args[0]);
			float movementSpeed = 30f;
			AssetModel tempCollection[];
			long initTime = Instant.now().getEpochSecond();
			System.out.println("basic setup, done");
			TracerouteLoader2 loader = new TracerouteLoader2();
			loader.loadAddresses();
			//LoadData test = new LoadData(args[1]);
			//test.LoadAll();
			System.out.println("loaded data");
			//tempCollection = test.generateAssetModels();
			tempCollection = loader.generateModels();
			System.out.println("number of Assets" + tempCollection.length);
			System.out.println("generated assets");
			Cluster[] groups = kmeans(tempCollection, size);
			System.out.println("number of clusters: " + groups.length);
			System.out.println("clustered the data");
			//for(int index = 0; index < size; index++)
			//{tempCollection[index] = new AssetModel(new Geolocation());}

			for(Cluster group: groups)
			{
				AssetModel[] input = group.getArray();
				if(input.length > 1)
				{
					TravelingSalesman2 tester = new TravelingSalesman2(start, end, 45, 0.1f, input, 30f, initTime);
					tester.runSimulations();
				}
			}
		}
		catch(Exception e)
		{e.printStackTrace();}   //something broke!
	}


	public TravelingSalesman2(Geolocation s, Geolocation e, float hCW, float vOC, AssetModel coll[], float cS, long tT)
	{
		counter = 0;
		startingPoint = s.clone();
		endingPoint =  e.clone();
		hourlyCrewWage = hCW;
		vehicalOperationCost = vOC;
		pointsOfInterest = new AssetModel[coll.length];
		generator = new Random();
		crewSpeed = cS;
		arrayLength = pointsOfInterest.length;
		secondsInAnHour = 60 * 60;
		theTime = tT;

		for(int index = 0; index < coll.length; index++)  {pointsOfInterest[index] = coll[index].clone();}
	}



	public void runSimulations() //simulated paths always move from the startingPoint to the endingPoint
	{
		System.out.println("beginning Simulations");
		System.out.println("number of Nodes: " + arrayLength);
		System.out.println("problem space: " + factorial(arrayLength));
		//randomWalk(99999);
		greedy();
		if(arrayLength < 9)  {bruteForce();}
		heuristicGreedy();
	}



	private void heuristicGreedy()
	{
		System.out.println("greedy with modified cost function");
		greedyApproach("amp");
	}

	private void greedy()
	{
		System.out.println("greedy with standard cost function");
		greedyApproach("norm");
	}


	private void randomWalk(int numberOfTries)
	{
		//int numberOfTries = (int) (999999);
		System.out.println("random walk attempts: " + numberOfTries);
		float shortestPath = Float.MAX_VALUE;
		float longestPath = -Float.MAX_VALUE;
		float shortestIncome = 0;
		float longestIncome = 0;
		long longestPathTime = 0;
		long shortestPathTime = 0;
		long start = System.currentTimeMillis();
		setVersion("norm");
		for(int index = 0; index < numberOfTries; index++)
		{
			int path[] = generateRandomSequence();
			TimeCost testValue = walk(path, theTime);
			float testIncome = tabulateTheCosts(testValue.getT()); resetAssets();
			if(testIncome < shortestPath)
			{
				shortestPath = testValue.getC();
				shortestPathTime = testValue.getT();
				shortestIncome = testIncome;
			}
			if(testIncome > longestPath)
			{
				longestPath = testValue.getC();
				longestPathTime = testValue.getT();
				longestIncome = testIncome;
			}
		}
		long end = System.currentTimeMillis();
		long runtime = end - start;
		long sPathTime = shortestPathTime - theTime;
		long lPathTime = longestPathTime - theTime;
		System.out.println("random search, runtime: " + runtime + "ms");
		System.out.println("random search, min: " + shortestPath);
		System.out.printf("random search, max income: %f\n", shortestIncome);
		System.out.println("random search, min time: " + sPathTime);
		System.out.println("random search, max: " + longestPath);
		System.out.println("random search, max time: " + lPathTime);
		System.out.printf("random search, max income: %f\n", longestIncome);
	}


	private void greedyApproach(String version)
	{
		LinkedList<AssetModel> order = new LinkedList<AssetModel>();
		//greedy, nearest neighbor
		long internalTime = theTime;
		long start = System.currentTimeMillis();
		boolean[] taken = new boolean[arrayLength];
		//int[] internalPath = new int[arrayLength];
		TimeCostIndex current = findNextNearest(taken, -1, internalTime);
		//internalPath[0] = current.getI();
		float totalCost = current.getC();
		internalTime = current.getT();
		setVersion(version); resetAssets();

		for(int index = 0; index < arrayLength - 1; index++)
		{
			current = findNextNearest(taken, current.getI(), internalTime);
			//internalPath[index + 1] = current.getI();
			totalCost += current.getC();
			internalTime = current.getT();
			pointsOfInterest[current.getI()].setTR(internalTime);
			order.add(pointsOfInterest[current.getI()]);
		}

		totalCost += pointsOfInterest[current.getI()].costFunction(internalTime, endingPoint, crewSpeed, hourlyCrewWage, vehicalOperationCost);
		internalTime = pointsOfInterest[current.getI()].timeAfterRepair(internalTime, endingPoint, crewSpeed);
		float income = tabulateTheCosts(internalTime); resetAssets();
		long end = System.currentTimeMillis();
		long runtime = end - start;
		long pathTime = internalTime - theTime;
		System.out.println("Greedy, runtime: " + runtime + "ms");
		System.out.println("Greedy, cost: " + totalCost);
		System.out.println("Greedy, time: " + pathTime);
		System.out.printf("Greedy, income: %f\n", income);
	}


	private void bruteForce()
	{
		float allCostMin = Float.MAX_VALUE;
		float allCostMax = -Float.MAX_VALUE;
		float allCostMinIncome = 0;
		float allCostMaxIncome = 0;
		long allCostTimeMin = 0;
		long allCostTimeMax = 0;
		long start = System.currentTimeMillis();
		int[] testPath = new int[arrayLength];
		for(int index = 0; index < arrayLength; index++)  {testPath[index] = index;}
		setVersion("norm");

		for(long index = 0; index < factorial(arrayLength); index++)
		{
			if(index % 10000 == 0)  {System.out.print("*");}
			generateNextCombinatoric(testPath);  //alters the array: testPath
			TimeCost testValue = walk(testPath, theTime);
			float testIncome = tabulateTheCosts(testValue.getT());  resetAssets();
			if(testIncome < allCostMin)
			{
				allCostMin = testValue.getC();
				allCostTimeMin = testValue.getT();
				allCostMinIncome = testIncome;
			}

			if(testIncome > allCostMax)
			{
				allCostMax = testValue.getC();
				allCostTimeMax = testValue.getT();
				allCostMaxIncome = testIncome;
			}
		}
		long end = System.currentTimeMillis();
		long runtime = end - start;
		long pathTimeMin = allCostTimeMin - theTime;
		long pathTimeMax = allCostTimeMax - theTime;
		System.out.println();
		System.out.println("exhaustive, runtime: "  + runtime + "ms");
		System.out.println("exhaustive, min cost: " + allCostMin);
		System.out.println("exhaustive, min time: " + pathTimeMin);
		System.out.printf("exhaustive, min income: %f\n", allCostMinIncome);
		System.out.println("exhaustive, max cost: " + allCostMax);
		System.out.println("exhaustive, max time: " + pathTimeMax);
		System.out.printf("exhaustive, max income: %f\n", allCostMaxIncome);
	}


	private float tabulateTheCosts(long finalTime)
	{
		float theCost = 0;
		for(AssetModel device: pointsOfInterest)
		{theCost += device.accountProfitAndLoss(finalTime, theTime);}
		return(theCost);
	}


	private void setVersion(String v)  {for(AssetModel device: pointsOfInterest)  {device.setVersion(v);}}


	private void resetAssets()  {for(AssetModel device: pointsOfInterest)  {device.blankTR();}}


	private void generateNextCombinatoric(int[] input)
	{
		do
		{
			input[arrayLength - 1]++;
			for(int index = arrayLength - 1; index >= 1; index--)
			{
				if(input[index] >= arrayLength)
				{
					input[index - 1]++;
					input[index] = 0;
				}
				else  {break;}
			}
			if(input[0] >= arrayLength)  {input[0] = 0;}
		}
		while(!isValidPath(input));
	}



	private static Cluster[] kmeans(AssetModel[] input, int maxLoadSize)
	{
		//set random centers
		System.out.println("starting kmeans");
		int numberOfClusters = input.length / maxLoadSize;
		Cluster[] clusters = new Cluster[numberOfClusters];
		for(int index = 0; index < numberOfClusters; index++)
		{clusters[index] = new Cluster(new Geolocation());}
		float minThresh = 0.5f;
		float diff = 100f;

		System.out.println("set up for kmeans complete");
		while(diff > minThresh)
		{
			for(Cluster group: clusters)  {group.clear();}
			System.out.println("moving centers");
			diff = 0;
			for(AssetModel device: input)
			{
				float nearestDistance = Float.MAX_VALUE;
				AssetModel closetCenter = null;
				Cluster group = null;
				for(Cluster element: clusters)
				{
					if(element.size() <= maxLoadSize)
					{
						float distance = device.getLocation().getDistance(element.getCenter());
						if(distance < nearestDistance)
						{
							nearestDistance = distance;
							group = element;
						}
					}
				}
				group.add(device);
			}

			for(Cluster group: clusters)  {diff += group.recenter();}
			System.out.print(diff + " ");
		}

		for(int index = 0; index < clusters.length; index++)
		{
			System.out.println(clusters[index].size() + " " + index);
		}

		return(clusters);
	}



	private TimeCostIndex findNextNearest(boolean[] isTaken, int current, long inputTime)  //WILL WRITE TO isTaken[]
	{
		int bestTarget = -1;
		float minCost = Float.MAX_VALUE;
		long finalTime = 0;  //to keep javac happy
		for(int index = 0; index < arrayLength; index++)
		{
			if(!isTaken[index])
			{
				Geolocation curr;
				if(current < 0)  {curr = startingPoint;}
				else  {curr = pointsOfInterest[current].getLocation();}

				AssetModel destination = pointsOfInterest[index];
				float cost = destination.costFunction(inputTime, curr, crewSpeed, hourlyCrewWage, vehicalOperationCost);
				if(cost < minCost)
				{
					minCost = cost;
					bestTarget = index;
					finalTime = destination.timeAfterRepair(inputTime, curr, crewSpeed);
				}
			}
		}
		isTaken[bestTarget] = true;
		return(new TimeCostIndex(finalTime, minCost, bestTarget));
	}



	private TimeCost walk(int sequence[], long inputTime)
	{
		float cost = pointsOfInterest[sequence[0]].costFunction(inputTime, startingPoint, crewSpeed, hourlyCrewWage, vehicalOperationCost);
		inputTime = pointsOfInterest[sequence[0]].timeAfterRepair(inputTime, startingPoint, crewSpeed);
		for(int index = 0; index < arrayLength - 1; index++)
		{
			Geolocation source = pointsOfInterest[sequence[index]].getLocation();
			AssetModel destination = pointsOfInterest[sequence[index + 1]];
			cost += destination.costFunction(inputTime, source, crewSpeed, hourlyCrewWage, vehicalOperationCost);
			inputTime = destination.timeAfterRepair(inputTime, source, crewSpeed);
			destination.setTR(inputTime);
		}
		cost += pointsOfInterest[sequence[arrayLength - 1]].costFunction(inputTime, endingPoint, crewSpeed, hourlyCrewWage, vehicalOperationCost);
		inputTime = pointsOfInterest[sequence[arrayLength - 1]].timeAfterRepair(inputTime, endingPoint, crewSpeed);
		return(new TimeCost(inputTime, cost));
	}


//	private TimeCost costFunction(long inputTime, Geolocation source)
//	{
//	}


	public int[] generateRandomSequence() //makes wild guess
	{
		int[] testPath = new int[arrayLength];
		boolean[] taken = new boolean[arrayLength];
		for(int index = 0; index < arrayLength; index++)  {taken[index] = false;}

		for(int index = 0; index < arrayLength; index++)
		{
			testPath[index] = generator.nextInt(arrayLength);
			//System.out.println(index);
			while(taken[testPath[index]])
			{
				//System.out.print("t:" + testPath[index]);
				if(!taken[testPath[index]])
				{taken[testPath[index]] = true;}
				else {testPath[index] = generator.nextInt(arrayLength);}
			}
			taken[testPath[index]] = true;
			//System.out.print(testPath[index]);
		}
		//System.out.println(isValidPath(testPath));
		return(testPath);
	}


	public boolean isValidPath(int potential[])
	{
		for(int hi = 0; hi < arrayLength; hi++)
		{
			for(int lo = hi + 1; lo < arrayLength; lo++)
			{if(potential[hi] == potential[lo])  {return(false);}}
		}
		return(true);
	}


	private static long factorial(int input)
	{
		long result = 1;
		for(int index = 2; index <= input; index++)
		{result = result * input;}
		return(result);
	}


	private static void printArray(int[] input)
	{
		for(int element: input)  {System.out.print(element + ",");}
		System.out.println();
	}


	private static void dumpToCSV(LinkedList<AssetModel> input, String fileName)
	{
		try
		{
			PrintWriter output = new PrintWriter(new File(fileName));
			ListIterator<AssetModel> list = input.listIterator();
			while(list.hasNext())
			{
				output.println(list.next().getLocation().toString());
				output.flush();
			}
			output.close();
		}
		catch(Exception e)
		{
			//couldn't open the file
		}
	}
}





class Geolocation
{
	float latitude;
	float longitude;
	float elevation;

	public Geolocation(float x, float y, float z)
	{
		latitude = x;
		longitude = y;
		elevation = z;
	}


	public Geolocation()
	{
		Random gen = new Random();
		latitude = gen.nextFloat() * 360;
		longitude = gen.nextFloat() * 360;
		elevation = gen.nextFloat() * 30;
	}


	public float getLat()   {return latitude;}
	public float getLon()  {return longitude;}
	public float getEle()  {return elevation;}


	public float getDistance(Geolocation loc)
	{
		double R = 6371 * (10 * 10 * 10);
		float x = (float) Math.toRadians(loc.getLat());
		float y = (float) Math.toRadians(loc.getLon());
		float z = (float) Math.toRadians(loc.getEle());
		double diffX = Math.toRadians(x - latitude);
		double diffY = Math.toRadians(y - longitude);
		double diffZ = Math.toRadians(z - elevation);

		double a = Math.pow(Math.sin(diffX / 2), 2) + Math.cos(diffX) * Math.pow(Math.sin(diffY / 2), 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double d = R * c;
		return((float) c);
	}



	public boolean equals(Object comparison)
	{
		if(comparison instanceof Geolocation)
		{
			Geolocation internal = (Geolocation) comparison;
			boolean s0 = internal.getLat() == latitude;
			boolean s1 = internal.getLon() == longitude;
			boolean s2 = internal.getEle() == elevation;

			return(s0 && s1 && s2);
		}
		else
		{return false;}
	}


	public Geolocation clone()  {return new Geolocation(latitude, longitude, elevation);}
	public String toStringIL()  {return(latitude + "," + longitude);}

	public String toString()
	{
		String la = "Latitude: " + latitude;
		String lo = "Longitude: " + longitude;
		String el = "Elevation: " + elevation;
		return(la + "\n" + lo + "\n" + el + "\n");
	}
}



class AssetModel  //probably best to have everything copy by reference, to avoid weird bugs when using multithreading
{
	Geolocation location;
	//String name;
	//String assetTrackingValue;
	float hourlyOperationCost;  //how much money does this thing cost to run?
	float hourlyGrossIncome;  //how much money does this thing make us just by it running?
	float hourlyDowntimeCost;  //how much money is being lost as a result of this thing NOT running?  does not include money lost from hourly gross income
	long uptime;  //start of last uptime interval on the UNIX EPOC
	long originalInstall;  //in seconds since UNIX EPOC
	boolean specialAccess;  //does someone have to get a key or a ladder or crawl somewhere to get to the thing
	float missionCriticality; //on a scale from one to zero, how important is this thing, outside of monatary considerations.  perhaps of historical importantance.
	long timeToRepair;
	long failTime;  //moment in the UNIX EPOC that the machine went down
	long timeFixed;
	boolean works;
	String version;
	NetworkNode connections;

	//float assetValue;  //how much was this thing purchased for?

	public AssetModel(Geolocation loc, float hOC, float hGI, float hDC, long uT, long oI, boolean sA, float mC, long tTR, long fT, long tF, boolean w)
	{
		location = loc.clone();
		hourlyOperationCost = hOC;
		hourlyGrossIncome = hGI;
		hourlyDowntimeCost = hDC;
		uptime = uT;
		originalInstall = oI;
		specialAccess = sA;
		missionCriticality = mC;
		timeToRepair = tTR;
		failTime = fT;
		timeFixed = tF;
		works = w;
		version = "norm";
	}



	public AssetModel(Geolocation loc)
	{
		Random gen = new Random();
		long initTime = Instant.now().getEpochSecond();
		location = loc.clone();
		hourlyOperationCost = gen.nextFloat() * 50f + 5f;
		hourlyGrossIncome = gen.nextFloat() * 500f + 50f;
		hourlyDowntimeCost = gen.nextFloat() * 500f + 50f;
		uptime = gen.nextLong();
		originalInstall = gen.nextLong();
		specialAccess = gen.nextBoolean();
		missionCriticality = gen.nextFloat();
		timeToRepair = gen.nextInt(60 * 60 * 2) + (60 * 30);
		failTime = initTime - gen.nextInt(60 * 60 * 24);
		timeFixed = -1;
		works = false;
		version = "normal";
	}



	public AssetModel(NetworkNode con)
	{
		Random gen = new Random();
		long initTime = Instant.now().getEpochSecond();
		location = con.getLoc();
		hourlyOperationCost = gen.nextFloat() * 50f + 5f;
		hourlyGrossIncome = gen.nextFloat() * 500f + 50f;
		hourlyDowntimeCost = gen.nextFloat() * 500f + 50f;
		uptime = gen.nextLong();
		originalInstall = gen.nextLong();
		specialAccess = gen.nextBoolean();
		missionCriticality = gen.nextFloat();
		timeToRepair = gen.nextInt(60 * 60 * 2) + (60 * 30);
		failTime = initTime - gen.nextInt(60 * 60 * 24);
		timeFixed = -1;
		works = false;
		version = "normal";
		connections = con;
	}


	public float calculateDownTimeCost(long currentTime)  //this method could be called by many different threads at once!  make sure it doesn't make any writes to any instance variables!
	{
		float total = 0;
		float hoursDown = (currentTime - failTime) / 3600;
		total += -hoursDown * hourlyOperationCost;
		total += hoursDown * hourlyGrossIncome;
		total += hoursDown * hourlyDowntimeCost;
		return(total);
	}



	public float costFunctionAmp1(long currentTime, Geolocation previous, float speed, float crewWage, float vehical)
	{
		return(costFunctionDefault(currentTime, previous, speed, crewWage, vehical) * ((hourlyGrossIncome - hourlyOperationCost) / hourlyOperationCost));
	}


	public long timeAfterRepair(long currentTime, Geolocation previous, float speed)
	{
		float distance = location.getDistance(previous);
		long time = (long) ((distance / speed) * 3600);
		time += timeToRepair + currentTime;
		return(time);
	}


	public float costFunctionDefault(long currentTime, Geolocation previous, float speed, float crewWage, float vehical)
	{
		float hours = (timeAfterRepair(currentTime, previous, speed) - currentTime) / 3600;
		float vehicalCost = location.getDistance(previous) * vehical;
		float crewCost = crewWage * hours;
		float cost = calculateDownTimeCost(currentTime) + vehicalCost + crewCost;
		//System.out.println(hours + " " + vehicalCost + " " + crewCost + " " + cost);
		return(cost);
	}



	public float costFunction(long currentTime, Geolocation previous, float speed, float crewWage, float vehical)
	{
		float value;
		if(version.equals("normal"))
		{value = costFunctionDefault(currentTime, previous, speed, crewWage, vehical);}
		else if(version.equals("amp"))
		{value = costFunctionAmp1(currentTime, previous, speed, crewWage, vehical);}
		else
		{value = costFunctionDefault(currentTime, previous, speed, crewWage, vehical);}
		return(value);
	}


	public float accountProfitAndLoss(long currentTime, long startTime)  //the method to assess how effective a given path actually is.
	{
		long tDiff = currentTime - startTime;
		long workingTime = currentTime - timeFixed;
		float loss = calculateDownTimeCost(timeFixed);
		float profit = 0;
		if(works)
		{profit = (hourlyGrossIncome - hourlyOperationCost) * (workingTime / 3600);}
		return(profit - loss);
	}


	public Geolocation getLocation()  {return(location.clone());}
	public float getHOC()  {return(hourlyOperationCost);}
	public float getHGI()  {return(hourlyGrossIncome);}
	public float getHDC()  {return(hourlyDowntimeCost);}
	public long getUT()    {return(uptime);}
	public long getOI()    {return(originalInstall);}
	public boolean getSA() {return(specialAccess);}
	public float getMC()   {return(missionCriticality);}
	public long getTTR()   {return(timeToRepair);}
	public long getFT()    {return(failTime);}
	public void setTR(long time)	{works = true; timeFixed = time;}
	public void blankTR()		{works = false; timeFixed = 0;}
	public boolean isWorking()		{return(works);}
	public long getTF() 		{return(timeFixed);}
	public void setVersion(String v)    {version = new String(v);}


	public boolean equals(Object input)
	{
        	if(input instanceof AssetModel)
        	{
            		AssetModel internal = (AssetModel) input;
			boolean s0 = location.equals(internal.getLocation());
			boolean s1 = hourlyOperationCost == internal.getHOC();
			boolean s2 = hourlyGrossIncome == internal.getHGI();
			boolean s3 = hourlyDowntimeCost == internal.getHDC();
			boolean s4 = uptime == internal.getUT();
			boolean s5 = originalInstall == internal.getOI();
			boolean s6 = specialAccess = internal.getSA();
			boolean s7 = missionCriticality == internal.getMC();
			boolean s8 = timeToRepair == internal.getTTR();
			boolean s9 = failTime == internal.getFT();
			boolean s10= works == internal.isWorking();
			boolean s11= timeFixed == internal.getTF();
			return(s0 && s1 && s2 && s3 && s4 && s5 && s6 && s7 && s8 && s9 && s10 && s11);
		}
		else
		{return false;}
	}


	public AssetModel clone()  {return new AssetModel(location.clone(), hourlyOperationCost, hourlyGrossIncome, hourlyDowntimeCost, uptime, originalInstall, specialAccess, missionCriticality, timeToRepair, failTime, timeFixed, works);}
}




class IntegerFloat
{
	int i;
	float f;
	public IntegerFloat(int i, float f)
	{
		this.i = i;
		this.f = f;
	}

	public int getI()  {return(i);}
	public float getF()    {return(f);}
}




class TimeCostIndex
{
	long t;
	float c;
	int i;
	public TimeCostIndex(long t, float c, int i)
	{
		this.t = t;
		this.c = c;
		this.i = i;
	}

	public long getT()  {return(t);}
	public float getC() {return(c);}
	public int getI()   {return(i);}
}



class TimeCost
{
	long t;
	float c;
	public TimeCost(long t, float c)
	{
		this.t = t;
		this.c = c;
	}

	public long getT()  {return(t);}
	public float getC() {return(c);}
}




class Cluster
{
	LinkedList<AssetModel> list;
	Geolocation center;

	public Cluster(Geolocation loc)
	{
		center = loc.clone();
		list = new LinkedList<AssetModel>();
	}


	public Geolocation getCenter()  {return(center);}
	public LinkedList<AssetModel> getList()  {return(list);}
	public AssetModel[] getArray()  {return(list.toArray(new AssetModel[list.size()]));}
	public void add(AssetModel element)  {list.add(element);}
	public int size()  {return(list.size());}
	public void clear()  {list.clear();}


	public float recenter()
	{
		float sumX = 0;
		float sumY = 0;
		float sumZ = 0;

		for(AssetModel device: list)
		{
			Geolocation loc = device.getLocation();
			sumX += loc.getLat();
			sumY += loc.getLon();
			sumZ += loc.getEle();
		}

		float newX = sumX / list.size();
		float newY = sumY / list.size();
		float newZ = sumZ / list.size();
		Geolocation oldCenter = center;
		center = new Geolocation(newX, newY, newZ);
		return(center.getDistance(oldCenter));
	}

}
