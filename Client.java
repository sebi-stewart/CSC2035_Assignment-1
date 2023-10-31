import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;

public class Client {
	DatagramSocket socket;
	static final int RETRY_LIMIT = 4;	/* 
	 * UTILITY METHODS PROVIDED FOR YOU 
	 * Do NOT edit the following functions:
	 *      exitErr
	 *      checksum
	 *      checkFile
	 *      isCorrupted  
	 *      
	 */

	/* exit unimplemented method */
	public void exitErr(String msg) {
		System.out.println("Error: " + msg);
		System.exit(0);
	}	

	/* calculate the segment checksum by adding the payload */
	public int checksum(String content, Boolean corrupted)
	{
		if (!corrupted)  
		{
			int i; 
			int sum = 0;
			for (i = 0; i < content.length(); i++)
				sum += (int)content.charAt(i);
			return sum;
		}
		return 0;
	}


	/* check if the input file does exist */
	File checkFile(String fileName)
	{
		File file = new File(fileName);
		if(!file.exists()) {
			System.out.println("SENDER: File does not exists"); 
			System.out.println("SENDER: Exit .."); 
			System.exit(0);
		}
		return file;
	}


	/* 
	 * returns true with the given probability 
	 * 
	 * The result can be passed to the checksum function to "corrupt" a 
	 * checksum with the given probability to simulate network errors in 
	 * file transfer 
	 */
	public boolean isCorrupted(float prob)
	{ 
		double randomValue = Math.random();   
		return randomValue <= prob; 
	}

	/**
	 * This function reads all the files contents into a Java string
	 * @param file a file containing text
	 * @return A string containing all contents of the file
	 */
	public String fileReader(File file)  {
		Scanner scanner;

		try {
			scanner = new Scanner(file);
			scanner.useDelimiter(System.getProperty("line.separator"));
		} catch (FileNotFoundException e){
			System.err.println("The file couldn't be found!");
			System.exit(1);
			return "";
		}
		String importedFile = "";

		boolean run = true;
		String line;

		while (run){
			try{
			line = scanner.nextLine();
			importedFile = importedFile.concat(line);
			// System.out.println(line);
			if(line.equals("") || scanner.hasNextLine()){
				importedFile = importedFile.concat("\n");
			}} catch(Exception e){
				// System.out.println(e);
				run = false;
			}
		}
		//System.out.println(importedFile);
		return importedFile;
	}

	/**
	 * This function reformats the String of text into 4 character long segments that can be sent to the Server
	 * @param importedFile a String containing all characters of the imported File
	 * @param segmentNumber the amount of segments that the final array will contain
	 * @return an ArrayList containing all finished segments
	 */
	private ArrayList<Segment> getSegments(String importedFile, int segmentNumber, float corruptionChance) {
		ArrayList<Segment> segments = new ArrayList<>();

		for (int sequence = 0; sequence< segmentNumber; sequence++){
			String stringSegment = "";
			Segment segment = new Segment();
			try{
				stringSegment = stringSegment.concat(importedFile.substring(sequence*4, sequence*4+4));
			} catch (Exception e){
				stringSegment = stringSegment.concat(importedFile.substring(sequence*4));
			}

			segment.setSize(stringSegment.length());
			segment.setSq(sequence%2);
			segment.setType(SegmentType.Data);
			segment.setPayLoad(stringSegment);
			segment.setChecksum(checksum(stringSegment, isCorrupted(corruptionChance)));

			segments.add(segment);
		}
		return segments;
	}



	/*
	 * The main method for the client.
	 * Do NOT change anything in this method.
	 *
	 * Only specify one transfer mode. That is, either nm or wt
	 */

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length < 5) {
			System.err.println("Usage: java Client <host name> <port number> <input file name> <output file name> <nm|wt>");
			System.err.println("host name: is server IP address (e.g. 127.0.0.1) ");
			System.err.println("port number: is a positive number in the range 1025 to 65535");
			System.err.println("input file name: is the file to send");
			System.err.println("output file name: is the name of the output file");
			System.err.println("nm selects normal transfer|wt selects transfer with time out");
			System.exit(1);
		}

		Client client = new Client();
		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);
		InetAddress ip = InetAddress.getByName(hostName);
		File file = client.checkFile(args[2]);
		String outputFile =  args[3];
		System.out.println ("----------------------------------------------------");
		System.out.println ("SENDER: File "+ args[2] +" exists  " );
		System.out.println ("----------------------------------------------------");
		System.out.println ("----------------------------------------------------");
		String choice=args[4];
		float loss = 0;
		Scanner sc=new Scanner(System.in);  


		System.out.println ("SENDER: Sending meta data");
		client.sendMetaData(portNumber, ip, file, outputFile); 

		if (choice.equalsIgnoreCase("wt")) {
			System.out.println("Enter the probability of a corrupted checksum (between 0 and 1): ");
			loss = sc.nextFloat();
		} 

		System.out.println("------------------------------------------------------------------");
		System.out.println("------------------------------------------------------------------");
		switch(choice)
		{
		case "nm":
			client.sendFileNormal (portNumber, ip, file);
			break;

		case "wt": 
			client.sendFileWithTimeOut(portNumber, ip, file, loss);
			break; 
		default:
			System.out.println("Error! mode is not recognised");
		} 


		System.out.println("SENDER: File is sent\n");
		sc.close(); 
	}


	/*
	 * THE THREE METHODS THAT YOU HAVE TO IMPLEMENT FOR PART 1 and PART 2
	 * 
	 * Do not change any method signatures 
	 */

	/* TODO: send metadata (file size and file name to create) to the server 
	 * outputFile: is the name of the file that the server will create
	*/
	public void sendMetaData(int portNumber, InetAddress IPAddress, File file, String outputFile) throws IOException {
		DatagramSocket clientSocket= null;

		try {

			clientSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("the socket could not be opened, or the socket could not bind to the specified port " +
					portNumber);
			System.exit(1);
		}

		MetaData meta = new MetaData();
		meta.setName(outputFile);
		meta.setSize((int)file.length());

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream;
		objectStream = new ObjectOutputStream(outputStream);
		objectStream.writeObject(meta);

		byte[] data = outputStream.toByteArray();
		DatagramPacket sentPacket = new DatagramPacket(data, data.length, IPAddress, portNumber);
		clientSocket.send(sentPacket);

		System.out.println("SENDER: Metadata is sent (filename, size): (" + outputFile + ", " + file.length() + ")");

		//exitErr("sendMetaData is not implemented");
	}


	/* TODO: Send the file to the server without corruption*/
	public void sendFileNormal(int portNumber, InetAddress IPAddress, File file) throws IOException {
		DatagramSocket clientSocket= null;

		try {

			clientSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("the socket could not be opened, or the socket could not bind to the specified port " +
					portNumber);
			System.exit(1);
		}

		String importedFile = fileReader(file);

		int segmentNumber = (int)Math.ceil(importedFile.length()/4.0);
		ArrayList<Segment> segments = getSegments(importedFile, segmentNumber, 0.0f);

		// Total Acknowledgements
		int totalAcks = 0;
		Segment currentSegment;

		byte[] incomingData = new byte[1024];
		Segment dataSeg = new Segment();

		while (totalAcks < segmentNumber){

			currentSegment = segments.get(totalAcks);

			System.out.println("SENDER: Sending new Segment: Sequence:" + currentSegment.getSq()+ ", Size:" +
					currentSegment.getSize() + ", CheckSum: " + currentSegment.getChecksum() + ", Content: (" +
					currentSegment.getPayLoad() + ")");


			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectStream;
			objectStream = new ObjectOutputStream(outputStream);
			objectStream.writeObject(currentSegment);

			byte[] data = outputStream.toByteArray();
			DatagramPacket sentPacket = new DatagramPacket(data, data.length, IPAddress, portNumber);
			clientSocket.send(sentPacket);

			DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);

			System.out.println("SENDER: Waiting for an Acknowledgement");
			//receive from the server
			clientSocket.receive(incomingPacket);

			byte[] outgoingData = incomingPacket.getData();
			ByteArrayInputStream in = new ByteArrayInputStream(outgoingData);
			ObjectInputStream is = new ObjectInputStream(in);


			try {
				dataSeg = (Segment) is.readObject();
				System.out.println("SENDER: An Acknowledgement Sequence "+ dataSeg.getSq()+" RECEIVED");
				totalAcks++;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			System.out.println("\t\t>>>>>SENDER: Segment transmitted successfully<<<<<");
			System.out.println("------------------------------");

		}
		System.out.println("Total segments " + segmentNumber);



		//exitErr("sendFileNormal is not implemented");
	}

	/* TODO: This function is essentially the same as the sendFileNormal function
	 *      except that it resends data segments if no ACK for a segment is 
	 *      received from the server.*/
	public void sendFileWithTimeOut(int portNumber, InetAddress IPAddress, File file, float loss) throws IOException {
		DatagramSocket clientSocket= null;

		try {

			clientSocket = new DatagramSocket();

			//Setting the timeout period
			clientSocket.setSoTimeout(2000);

		} catch (SocketException e) {
			System.err.println("the socket could not be opened, or the socket could not bind to the specified port " +
					portNumber);
			System.exit(1);
		}

		String importedFile = fileReader(file);

		int segmentNumber = (int)Math.ceil(importedFile.length()/4.0);
		System.out.println(loss);
		ArrayList<Segment> segments = getSegments(importedFile, segmentNumber, loss);

		// Total Acknowledgements
		int totalAcks = 0;
		int retryCounter = 0;
		ArrayList<Segment> retransmittedSegments = new ArrayList<>();
		Segment currentSegment;

		byte[] incomingData = new byte[1024];
		Segment dataSeg = new Segment();

		while (totalAcks < segmentNumber){
			currentSegment = segments.get(totalAcks);

			System.out.println("SENDER: Sending new Segment: Sequence:" + currentSegment.getSq()+ ", Size:" +
					currentSegment.getSize() + ", CheckSum: " + currentSegment.getChecksum() + ", Content: (" +
					currentSegment.getPayLoad() + ")");

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ObjectOutputStream objectStream;
			objectStream = new ObjectOutputStream(outputStream);
			objectStream.writeObject(currentSegment);

			byte[] data = outputStream.toByteArray();
			DatagramPacket sentPacket = new DatagramPacket(data, data.length, IPAddress, portNumber);
			clientSocket.send(sentPacket);


			DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);

			System.out.println("SENDER: Waiting for an Acknowledgement");
			try{
				//receive from the server
				clientSocket.receive(incomingPacket);
			} catch (SocketTimeoutException e){
				retryCounter++;
				if (retryCounter >= RETRY_LIMIT) {
					System.out.println("SENDER: The Acknowledgement wasn't received 4x in a row, the program is terminating");
					System.exit(1);
				}
				System.out.println("SENDER: The Acknowledgement timed out, resending segment with recalculated checkSum");
				if (!retransmittedSegments.contains(currentSegment)) retransmittedSegments.add(currentSegment);
				currentSegment.setChecksum(checksum(currentSegment.getPayLoad(), isCorrupted(loss)));
				segments.set(totalAcks, currentSegment);
				System.out.println("**********************");
				continue;
			}
			retryCounter = 0;

			byte[] outgoingData = incomingPacket.getData();
			ByteArrayInputStream in = new ByteArrayInputStream(outgoingData);
			ObjectInputStream is = new ObjectInputStream(in);


			try {
				dataSeg = (Segment) is.readObject();
				System.out.println("SENDER: An Acknowledgement Sequence "+ dataSeg.getSq()+" RECEIVED");
				totalAcks++;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println("\t\t>>>>>SENDER: Segment sent successfully<<<<<");
			System.out.println("------------------------------");

		}
		System.out.println("Total segments: " + segmentNumber + ", Retransmitted segments: " + retransmittedSegments.size());
		// System.out.println(retransmittedSegments);
	} 


}