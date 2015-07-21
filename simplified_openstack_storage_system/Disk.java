import java.net.InetAddress;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Disk {
	private static final String PATTERN = 
			"^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"; // re for ipv4
	String ip;
	String name;
	int port = 0;
	ArrayList<Partition> listOfPartitions;
	
	public Disk(String str) {		
		// If host name is used, convert it to IP
		try {
			InetAddress address = InetAddress.getByName(str);
			this.ip = address.getHostAddress();
			this.name = address.getHostName();
		} catch(Exception e) {
			System.out.println("Invalid host\nExiting...");
			System.exit(0);
		}
		
		listOfPartitions = new ArrayList<Partition>();
	}
	
	public Disk(String str, int port) {
		this.port = port;
		
		try {
			InetAddress address = InetAddress.getByName(str);
			this.ip = address.getHostAddress();
			this.name = address.getHostName();
		} catch(Exception e) {
			System.out.println("Invalid host\nExiting...");
			System.exit(0);
		}
		
		listOfPartitions = new ArrayList<Partition>();
	}
	
	// validate ipv4
	public static boolean validate(String ip){
		Pattern pattern = Pattern.compile(PATTERN);
		Matcher matcher = pattern.matcher(ip);
		return matcher.matches();
	}
	
	// Assign partitions to disk
	public void addPartitions(ArrayList<Partition> listOfPartitions) {
		for(Partition partition : listOfPartitions)
			this.listOfPartitions.add(partition);
	}
	
	// Assign a partition to disk
	public void addPartition(Partition partition) {
		this.listOfPartitions.add(partition);
	}
	
	// show partitions
	public String details() {
		String str = ip + ":" + port;
		for(Partition p : listOfPartitions)
			str = str + "\n" + p;
		return str;
	}
	
	public String toString() {
		return "Disk -- " + this.name + " " + this.ip + ":" + this.port;
	}

}
