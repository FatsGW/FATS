import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

class Simulation
{
	public static void main(String[] args)
	{
		Simulation s = new Simulation();
		//s.generateTestFiles();

		s.loadTestFiles();
		s.run();
		s.saveResults(1);
		s.saveResults(2);
		s.saveResults(3);
	}

	private final String TEST_FILE_NAME_1 = "testcase1_5min.txt";
	private final String TEST_FILE_NAME_2 = "testcase2_5min.txt";
	private final String TEST_FILE_NAME_3 = "testcase3_30min.txt";
	private final String TEST_FILE_NAME_4 = "testcase4_30min.txt";
	private final String TEST_FILE_NAME_5 = "testcase5_6hour.txt";
	private final String TEST_FILE_NAME_6 = "testcase6_6hour.txt";

	private final int TEST_CASES = 6;

	private final String RESULT1_FILE_NAME = "control (5 min range).csv";
	private final String RESULT2_FILE_NAME = "control (30 min range).csv";
	private final String RESULT3_FILE_NAME = "control (6 hour range).csv";

	private final int DEVICE_COUNT = 1000;
	private Device[][] _devices;
	private LinkedList<LinkedList<Receiver>> _results;

	public Simulation()
	{

	}

	public void generateTestFiles()
	{
		generateTestFile(TEST_FILE_NAME_1, 300);
		generateTestFile(TEST_FILE_NAME_2, 300);
		generateTestFile(TEST_FILE_NAME_3, 1800);
		generateTestFile(TEST_FILE_NAME_4, 1800);
		generateTestFile(TEST_FILE_NAME_5, 21600);
		generateTestFile(TEST_FILE_NAME_6, 21600);
	}

	public void generateTestFile(String filename, int timeRangeInSeconds)
	{
		//generate test cases
		Device[] devices = new Device[DEVICE_COUNT];
		for (int i = 0; i < DEVICE_COUNT; i++)
			devices[i] = Device.create(timeRangeInSeconds);

		//write test cases to file
		File file = new File(filename);
		try
		{
			FileOutputStream fop = new FileOutputStream(file);
			file.createNewFile(); //create new file if and only if it does not exist

			for (int i = 0; i < devices.length; i++)
			{
				String content = devices[i].toDataString();
				fop.write(content.getBytes());
			}
			fop.flush();
			fop.close();

			System.out.println("Test File generated");

		} 
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public void loadTestFiles()
	{
		_devices = new Device[TEST_CASES][DEVICE_COUNT];
		_devices[0] = loadTestFile(TEST_FILE_NAME_1);
		_devices[1] = loadTestFile(TEST_FILE_NAME_2);
		_devices[2] = loadTestFile(TEST_FILE_NAME_3);
		_devices[3] = loadTestFile(TEST_FILE_NAME_4);
		_devices[4] = loadTestFile(TEST_FILE_NAME_5);
		_devices[5] = loadTestFile(TEST_FILE_NAME_6);
	}

	public Device[] loadTestFile(String filename)
	{
		//initialize holder
		Device[] devices = new Device[DEVICE_COUNT];

		//load file and data
		File file = new File(filename);
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			int i = 0;
			while((line = br.readLine()) != null)
			{
				devices[i] = Device.load(line);

				if (++i >= DEVICE_COUNT)
					break;
			}
			br.close();

			System.out.println("Test File Loaded");
		} 
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return devices;
	}

	public void run()
	{
		System.out.println("Starting Simulation...");
		//initialize result holder
		_results = new LinkedList<LinkedList<Receiver>>();

		//simulate test 

		for (int i = 0; i < TEST_CASES; i++)
		{
			System.out.println("Starting Test case" + (i+1) + "...");
			LinkedList<Receiver> layer = new LinkedList<Receiver>();

			//layer.add(run(120, 0, i));
			System.out.println("... control test complete...");
			

			layer.add(run(10, 120, i));
			layer.add(run(20, 120, i));
			layer.add(run(30, 120, i));
			layer.add(run(60, 120, i));
			layer.add(run(90, 120, i));
			layer.add(run(120, 120, i));


			/*for (int tr = Receiver.INTERMISSION_MIN; tr <= Receiver.INTERMISSION_MAX; tr *= 2 )
			{
				layer.add(run(10, tr, i));
				if (tr == 80)
					layer.add(run(10,120,i));
			}
			System.out.println("... test 10s complete...");
			for (int tr = Receiver.INTERMISSION_MIN; tr <= Receiver.INTERMISSION_MAX; tr *= 2)
			{
				layer.add(run(20, tr, i));
				if (tr == 80)
					layer.add(run(20,120,i));
			}
			System.out.println("... test 20s complete...");
			for (int tr = Receiver.INTERMISSION_MIN; tr <= Receiver.INTERMISSION_MAX; tr *= 2)
			{
				layer.add(run(30, tr, i));
				if (tr == 80)
					layer.add(run(30,120,i));
			}
			System.out.println("... test 30s complete...");
			for (int tr = Receiver.INTERMISSION_MIN; tr <= Receiver.INTERMISSION_MAX; tr *= 2)
			{
				layer.add(run(60, tr, i));
				if (tr == 80)
					layer.add(run(60,120,i));
			}
			System.out.println("... test 60s complete...");
			for (int tr = Receiver.INTERMISSION_MIN; tr <= Receiver.INTERMISSION_MAX; tr *= 2)
			{
				layer.add(run(90, tr, i));
				if (tr == 80)
					layer.add(run(90,120,i));
			}
			System.out.println("... test 90s complete...");
			for (int tr = Receiver.INTERMISSION_MIN; tr <= Receiver.INTERMISSION_MAX; tr *= 2)
			{
				layer.add(run(120, tr, i));
				if (tr == 80)
					layer.add(run(120,120,i));
			}
			System.out.println("... test 120s complete...");*/

			_results.add(layer);
			System.out.println("Test case" + (i+1) + " completed");
		}


		System.out.println("Simulation Complete.");
	}

	public Receiver run(int bcDur, int bcTrans, int layer)
	{
		//initialize simulated receiver
		Receiver receiver = new Receiver(bcDur, bcTrans);

		//loop through all simulated devices
		for (int i = 0; i < _devices[layer].length; i++)
		{
			//initialize the device
			Device device = _devices[layer][i];
			device.init(bcDur, bcTrans);
			boolean hasMet = false;

			//simulate time passing as long as device has not exited the range
			while (!device.hasExited())
			{
				device.timePassed();
				receiver.timePassed();

				if (!hasMet && device.canMeet()) //device not met before and within range + broadcasting
				{
					//check if the receiver met the device
					hasMet = receiver.meet();
				}
			}

			//increment the miss counter
			if (!hasMet)
				receiver.miss();

		}

		//return the receiver which holds the result
		return receiver;
	}

	public void saveResults(int type)
	{
		File file;
		switch (type)
		{
		case 1:
			file = new File(RESULT1_FILE_NAME);
			break;
		case 2:
			file = new File(RESULT2_FILE_NAME);
			break;
		case 3:
			file = new File(RESULT3_FILE_NAME);
			break;
		default:
			file = new File(RESULT1_FILE_NAME);
			break;
		}

		try
		{
			FileOutputStream fop = new FileOutputStream(file);
			if (!file.exists())
				file.createNewFile();

			//write in the headers first
			String header = "Duration,Intermission,Broadcast Count, Total BC Duration,Hits,Misses,Hit %\n";
			fop.write(header.getBytes());

			//loop through all layers of results and save average
			int resultSize = _results.getFirst().size();
			int sampleSize = 2;
			int startIndex = (type - 1) * 2;

			for (int i = 0; i < resultSize; i++)
			{
				Receiver r = new Receiver();
				for (int j = (type - 1) * 2; j < startIndex + sampleSize; j++)
				{
					r.sum(_results.get(j).get(i));
				}
				r.average();
				fop.write(r.getCSVString().getBytes());
			}

			fop.flush();
			fop.close();

			System.out.println("Test Result Saved");
		} 
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}

