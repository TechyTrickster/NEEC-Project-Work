import java.util.LinkedList;
import java.util.Random;
public class generateIPs
{
	//generates all possible IP addresses, with lower and upper bounds given in standard ip form
	//or generates a fixed number or random IP addresses
	//works with IPv4 only, because I'm lazy....
	public static void main(String args[])
	{
		if(args.length == 0)
		{System.out.println("you must chose a mode of operation!"); System.exit(1);}

		if(args[0].equals("-r") && args.length >= 2)
		{
			int numberOfIps = Integer.parseInt(args[1]);
			randomMode(numberOfIps);
		}else if(args[0].equals("-l") && args.length >= 3)
		{
			int lowerBound[] = convertIpv4ToArray(args[1]);
			int upperBound[] = convertIpv4ToArray(args[2]);
			linearMode(lowerBound, upperBound);
		}
	}


	private static int[] convertIpv4ToArray(String ipv4)
	{
		String buffer = "";
		int index = 0;
		int output[] = new int[4];
		char v4[] = ipv4.toCharArray();
		for(char element: v4)
		{
			if(element != '.')
			{buffer += Character.toString(element);}
			else
			{
				output[index] = Integer.parseInt(buffer);
				buffer = "";
				index++;
			}
		}
		output[index] = Integer.parseInt(buffer);
		return output;
	}


	private static void randomMode(int count)
	{
		LinkedList<String> allIPs = new LinkedList<String>();
		int index = 0;
		while(index < count)
		{
			String newIP = generateRandomIPv4();
			if(!allIPs.contains(newIP))
			{
				System.out.println(newIP);
				allIPs.add(newIP);
				index++;
			}
		}
	}


	private static String generateRandomIPv4()
	{
		Random generator = new Random();
		String v0 = Integer.toString(generator.nextInt(256));
		String v1 = Integer.toString(generator.nextInt(256));
		String v2 = Integer.toString(generator.nextInt(256));
		String v3 = Integer.toString(generator.nextInt(256));
		return v0 + "." + v1 + "." + v2 + "." + v3;
	}


	private static void linearMode(int lower[], int upper[])
	{
		for(int in0 = lower[0]; in0 <= upper[0]; in0++)
		{
			for(int in1 = lower[1]; in1 <= upper[1]; in1++)
			{
				for(int in2 = lower[2]; in2 <= upper[2]; in2++)
				{
					for(int in3 = lower[3]; in3 <= upper[3]; in3++)
					{System.out.println(intVsToIPv4(in0, in1, in2, in3));}
				}
			}
		}
	}



	private static String intVsToIPv4(int v0, int v1, int v2, int v3)
	{
		String s0 = Integer.toString(v0);
		String s1 = Integer.toString(v1);
		String s2 = Integer.toString(v2);
		String s3 = Integer.toString(v3);

		return s0 + "." + s1 + "." + s2 + "." + s3;
	}
}
