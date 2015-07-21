import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

class FileObj {
	String name;
	String owner;
	Date date;
	int size; // file size in byte
	ArrayList<Partition> pList;
	
	public FileObj(String owner, String name, int size, ArrayList<Partition> pList) {
		this.owner = owner;
		this.name = name;
		this.size = size;
		this.date = new Date();
		this.pList = pList;
		for(Partition p : pList)
			p.file = this;
	}
	
	public String toString() {
		SimpleDateFormat format = new SimpleDateFormat("MMM dd HH:mm");
		String dateString = format.format(date);
		return owner + ", " + size + " bytes, " + dateString + ", " + name;
	}
	
	public String details() {
		String result = this.toString();
		for(Partition p : pList) {
			result = result + "\n\t\t" + p;
		}
		return result;
	}
}