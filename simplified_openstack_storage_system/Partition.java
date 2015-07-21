public class Partition {
	int index;
	Disk disk;
	boolean isOccupied;
	FileObj file;
	
	public Partition(int index, Disk disk) {
		this.index = index;
		this.disk = disk;
		this.isOccupied = false;
		this.file = null;
	}
	
	public String toString() {
		return "Partition # -- " + this.index + " (" + this.disk + ")";
	}
}