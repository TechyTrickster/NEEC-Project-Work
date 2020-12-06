import java.io.*;

public class LoadData
{
	String fileName;
	//BufferedReader input;
	int lineCount;
	boolean hasLineCount;
	boolean loadedData;
	boolean generatedModels;
	Field[] data;
	AssetModel[] models;



	public static void main(String args[])
	{
		try
		{
			LoadData test = new LoadData(args[0]);
			System.out.println("line count: " + test.getLineCount());
			Field[] data = test.LoadAll();
			for(Field element: data)
			{System.out.println(element.toString());}
		}
		catch(Exception e)
		{System.out.println("couldn't find the file, probably");}
	}

	public LoadData(String fileName) throws IOException
	{
		this.fileName = fileName;
		hasLineCount = false;
		loadedData = false;
		generatedModels = true;
		lineCount = -1;
		//input = new BufferedReader(new FileReader(new File(fileName)));
	}


	public int getLineCount()
	{
		if(!hasLineCount)
		{
			try
			{
				BufferedReader copy = new BufferedReader(new FileReader(new File(fileName)));
				while(copy.readLine() != null)  {lineCount++;}
				hasLineCount = true;
			}
			catch(Exception e) //getting the line count failed, set the return value to -1
			{lineCount = -1;}
		}
		return(lineCount);
	}



	public Field[] LoadAll()
	{
		if(!loadedData)
		{
			try
			{
				getLineCount();
				data = new Field[lineCount];
				int index = 0;
				String line = null;
				BufferedReader copy = new BufferedReader(new FileReader(new File(fileName)));
				while((line = copy.readLine()) != null)
				{
					data[index] = new Field(line);
					index++;
				}
				loadedData = true;
			}
			catch(Exception e)  {}  //load failed!
		}

		return(data);
	}


	public Field[] getFields()  {return(data);}



	public AssetModel[] generateAssetModels()
	{
		models = new AssetModel[lineCount];
		for(int index = 0; index < lineCount; index++)
		{models[index] = new AssetModel(data[index].getLocation());}
		return(models);
	}
}




class Field
{
	String IPAddress;
	Geolocation location;
	public Field(String input)
	{
		int inputLength = input.length();
		int commaOne   = input.indexOf(',');
		int commaTwo   = input.indexOf(',', commaOne + 1);
		IPAddress = input.substring(0, commaOne -1);
		float lat = Float.valueOf(input.substring(commaOne + 1, commaTwo -1));
		float lon = Float.valueOf(input.substring(commaTwo + 1, inputLength));
		float ele = 51f;
		location = new Geolocation(lat, lon, ele);
	}



	public Geolocation getLocation()  {return(location);}
	public String getIP()  {return(IPAddress);}
	public String toString()
	{return(new String("IP: " + IPAddress + " LOC: " + location.toString()));}
}
