import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.*;


public class Server {
	public static final int PORT_START = 1024; // port # to start searching from
	public static final int PORT_END = 65535; // port # to end searching at
	public static final int BACKLOG = 10; // client queue size
	public static final int SPACE = 32; // hash value space power
	public static ArrayList<Disk> listOfDisks = new ArrayList<Disk>();
	public static HashMap<Long, ArrayList<FileObj>> mapOfHashValue = new HashMap<Long, ArrayList<FileObj>>(); // hash value and FileObj pair
	public static HashMap<String, ArrayList<ArrayList<FileObj>>> mapOfUser = new HashMap<String, ArrayList<ArrayList<FileObj>>>(); // user and hash value pair
	public static HashMap<Integer, Partition> mapOfPartition = new HashMap<Integer, Partition>(); // partition # and partition pair
	public static int partitionPower = 0;
	public static int numOfPartitions = 0;
	public static int pSize = 0; // partition size in byte
	
	public static void main(String[] args) {
		// echo command line input
		for(String s : args) {
			System.out.print(s + "\t");
		}
		System.out.println();
		
		// Check num of parameters, must be more than one
		if(args.length < 2) {
			System.out.println("Invalid number of parameters\nExiting...");
			System.exit(0);
		}
		
		// Get partition power and calculate number of partitions
		try {
			partitionPower = Integer.parseInt(args[0]);
			numOfPartitions = (int)Math.pow(2, partitionPower);
			pSize = (int)Math.pow(2, SPACE - partitionPower);
			System.out.println("Partition power: " + partitionPower);
			System.out.println("Number of partitions: " + numOfPartitions);
			System.out.println();
		} catch(Exception e) {
			System.out.println("Invalid partition power\nExiting...");
			System.exit(0);
		}
		
		// Get IPs and ports of disks
		for(int i = 1; i < args.length; i++) {
			String[] strs = args[i].split(":");
			listOfDisks.add(new Disk(strs[0], Integer.parseInt(strs[1])));
		}
		
//		System.out.println("Storage hosts include:");
//		for(Disk disk : listOfDisks)
//			System.out.println("\t" + disk.details());
//		System.out.println();
		
		// Assign partitions to disks
		for(int i = 0; i < numOfPartitions; i++) {
			int numOfPartitionsPerDisk = numOfPartitions/listOfDisks.size();
			Disk disk = listOfDisks.get(i/numOfPartitionsPerDisk);
			Partition p = new Partition(i, disk);
			disk.addPartition(p);
			mapOfPartition.put(i, p);
		}
		
		// Show disk
		System.out.println("Storage hosts include:");
		for(Disk d : listOfDisks) {
			System.out.println(d + " (" + d.listOfPartitions.size() + " partitions)");
			//for(Partition p : d.listOfPartitions)
			//	System.out.println("\t" + p);
		}
		
		// Find a random unused port dynamically
		// If the random port is being used, then loop to find the first port available
		// If no port available(unlikely), exit program
		ServerSocket serverSocket = null;
		
		for(int i = 0; i < 1000; i++) {
			try{
				serverSocket = new ServerSocket(0, BACKLOG);
				System.out.println("Storage server: " + InetAddress.getLocalHost().getHostName() + " " 
						+ InetAddress.getLocalHost().getHostAddress() 
						+ ", listening on port " + serverSocket.getLocalPort() + "...");
				break;
			} catch(Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		
		System.out.println("\n\n");
		// Infinite loop to server clients one at a time
		while(true) {
			// All IO is in try-catch block
			try {
				// Accept requests from client
				Socket socket = serverSocket.accept();
				System.out.println("Accepted connection: " + socket);
				
				// Get input and output stream
				InputStream input = socket.getInputStream();
				BufferedInputStream is = new BufferedInputStream(input);
				OutputStream output = socket.getOutputStream();
				BufferedOutputStream os = new BufferedOutputStream(output);
				
				// read request from client
				byte[] inputBytes = new byte[8192];
				String inputStr = "";
				int count;
				count = is.read(inputBytes, 0, inputBytes.length); // num of bytes in request
				byte[] copy = new byte[count];
				System.arraycopy(inputBytes, 0, copy, 0, count);
				inputStr += new String(copy, "UTF-8"); // convert bytes to string
				String[] request = inputStr.split("\t"); // split operation and target in request
				
				String reply = ""; // store output string to send to client
				
				// types of operations
				// 0 -- download user/file
				// 1 -- upload user/file
				// 2 -- delete user/file
				// 3 -- list user
				// 4 -- add disk
				// 5 -- remove disk
				
				// errors
				// 6 -- file doesn't exist
				// 10 -- file already exists, overwrite
				// 7 -- user doesn't exist
				// 8 -- disk already exist
				// 9 -- disk doesn't exist
				int operation = Integer.parseInt(request[0]); // operation code
				String target = request[1].trim(); // file or disk name
				
				// prepare and initialize parameters for operations
				long hashValue = 0; // hash value of user/file string
				ArrayList<FileObj> fileList = null; // store replicas of a file, default is 2
				ArrayList<ArrayList<FileObj>> listOfFileList = null; // store files of a user
				
				if(operation == 0 || operation == 2) {
					// in download and delete, target format is user/file
					hashValue = getHashValue(target); // get hash value
					fileList = mapOfHashValue.get(hashValue); // list of file and replica
					if(fileList == null) // file doesn't exist
						operation = 6;
				}
				else if(operation == 1) {
					// in upload, target format is user/file/size
					String[] temp = target.split("/");
					hashValue = getHashValue(temp[0] + "/" + temp[1]);
					fileList = mapOfHashValue.get(hashValue); // use hash value to get list of file and replica
					if(fileList != null) // file already exists
						operation = 10;
				}
				else if(operation == 3) {
					listOfFileList = mapOfUser.get(target); // get a list of lists-of-replicas of a user
					if(listOfFileList == null) // user doesn't exist
						operation = 7;
				}
				else if(operation == 4){ // disk already exist or offline, cannot add
					if(getDisk(target) != null) {
						reply += "disk already exists";
						operation = 8; 
					}
					else if(!isOnline(target)) {
						reply += "disk offline";
						operation = 8;
					}
				}
				else if(operation == 5){
					operation = 9; // assume the disk doesn't exist
					if(getDisk(target) != null) operation = 5;
				}
				
				// send messages to client based on operations
				boolean isGood, isOnline;
				switch(operation) {
					// download user/object, display where file is saved
					case 0: 
						System.out.println("Request: download " + target);
						
						// original location
						reply += "\n" + showFileLocation(target, fileList);
						
						// find if disk fail, fix issue
						isOnline = checkDisks();
						if(!isOnline) {
							reply += "\nNew locations:\n" + showFileLocation(target, fileList);
						}
						
						// find if removed or corrupted, fix issue
						isGood = checkFile(hashValue);
						break;
						
					// upload user/object, display which disks to save
					case 1: 
						System.out.println("Request: upload " + target); // in upload, target include size info
						reply = upload(target, hashValue); // intended location
						
						// find failed disk, reassign its partitions and replicate files
						isOnline = checkDisks();
						if(!isOnline) {
							reply += "\nNew locations:\n" + overwrite(target, hashValue); // revised location
						}
						break;
						
					// delete user/object, display confirmation
					case 2: 
						System.out.println("Request: delete " + target);
						
						// original location
						reply += "\n" + showFileLocation(target, fileList);
						
						// find failed disk, reassign its partitions and replicate files
						isOnline = checkDisks();
						if(!isOnline) {
							reply += "\nNew locations:\n" + showFileLocation(target, fileList); // revised location
						}
						
						// find if removed or corrupted, fix issue
						isGood = checkFile(hashValue);
						
						reply += "\n" + deleteFile(target, hashValue); // delete if exist
						break;
					// list user's files
					case 3: 
						System.out.println("Request: list " + target);
						
						reply += "\n" + listFiles(target);
						
						// find failed disk, reassign its partitions and replicate files
						isOnline = checkDisks();
						if(!isOnline)
							reply += "\nNew locations:\n" + listFiles(target);
						
						// check removed/corrupted files
						for(ArrayList<FileObj> fList : listOfFileList) {
							for(FileObj f : fList) {
								if(!checkFile(getHashValue(f.owner + "/" + f.name))) {
									isGood = false;
								}
							}
						}
						
						break;
					// add disk
					case 4: 
						System.out.println("Request: add " + target);
						// find failed disk, reassign its partitions and replicate files
						isOnline = checkDisks();
						
						String[] strs = target.split(":");
						reply = addDisk(new Disk(strs[0], Integer.parseInt(strs[1])));
						break;
					// remove disk
					case 5: 
						System.out.println("Request: remove " + target);
						// find failed disk, reassign its partitions and replicate files
						isOnline = checkDisks(); // the disk to be remove may be off line
						
						Disk disk = getDisk(target);
						ArrayList<Partition> list = null;
						if(disk != null) { // disk still online
							list = disk.listOfPartitions; 
							
							// delete file in the disk
							for(Partition p : list) {
								deleteStoredFile(p);
							}
						}
						
						
						reply = removeDisk(target);
						System.out.println(reply);
						
						// replicate files in the partitions of the removed disk
						if(list != null) {
							for(Partition p : list) {
								// fetch file(one segment in one partition) from other storage server to replace it
								int pReplicaIndex = 
										p.index < numOfPartitions/2 ? p.index+numOfPartitions/2 : p.index-numOfPartitions/2;
								Partition pReplica = mapOfPartition.get(pReplicaIndex);
								
								if(pReplica.isOccupied) {
									fetchFile(pReplica);
									storeFile(p);
								}
							}
						}
						
						break;
					// 6 -- file doesn't exist
					case 6: 
						System.out.println("Request: download/delete " + target);
						reply = "\nNo file " + target;
;						break;
					// 10 -- file already exists, overwrite
					case 10: 
						System.out.println("Request: upload " + target);
						reply = overwrite(target, hashValue);
						
						// find failed disk, reassign its partitions and replicate files
						isOnline = checkDisks();
						if(!isOnline) {
							reply += "\nNew locations:\n" + upload(target, hashValue); // revised location
						}
						
						// find if removed or corrupted, fix issue
						isGood = checkFile(hashValue);
						if(!isGood) {
							reply += "\nfile was removed/corrupted and is uploaded anyway";
						}
						
						break;
					// 7 -- user doesn't exist
					case 7: 
						System.out.println("Request: list " + target);
						reply = "\nUser " + target + " doesn't exist";
						break;
					// 8 -- disk already exist or offline
					case 8: 
						System.out.println("Request: add " + target);
						reply += "\nCannot add disk. Disk already exists or offline";
						break;
					// 9 -- disk doesn't exist
					case 9: 
						System.out.println("Request: remove " + target);
						reply = "\nCannot remove disk. Disk isn't included: " + target;
						break;
					case 11:
						System.out.println("Request: list all disks");
						reply = listDisks();
						break;
					default: reply = "\nInvalid operation...";
				}
				
				// send reply to client
				inputBytes = reply.getBytes();
				System.out.println(reply); // show reply message in server screen
				os.write((""+inputBytes.length).getBytes()); // first send length of reply message
				os.flush();
				is.read(); // read response from client
				os.write(inputBytes); // send reply to client
				os.flush();
				
				// file transfer
				File file = null;
				if(operation == 0) { // download
					// fetch all relevant file from storage server
					reconstructFile(hashValue);
					
					is.read(); // response from client
					
					String name = "_" + target.split("/")[0] + "_" + target.split("/")[1];
					file = new File(name);
					
					// detect file type
					String fileType = Files.probeContentType(file.toPath());
					os.write(fileType.getBytes().length); // send length of fileType
					os.flush();
					is.read();
					os.write(fileType.getBytes()); // send file type
					os.flush();
					is.read();
					
					downloadFile(os, file);// process download request
					
					file.delete();
				}
				if((operation == 1 || operation == 10) && !reply.equals("Disk is full")) { // upload or overwrite
					String name = "_" + target.split("/")[0] + "_" + target.split("/")[1]; // change user/file to _user_file
					file = new File(name);
					uploadFile(is, file); // process upload request
					
					deliverFile(hashValue);
				}
				
				os.close();
				
				System.out.println("\n\n\n\n\n");
			} catch(Exception e) {
				continue;
			}
			
		}
		
	}
	
	// store file segements to respective partitions for both replicas, for client upload
	public static void deliverFile(long hashValue) throws Exception {
		ArrayList<FileObj> fList = mapOfHashValue.get(hashValue); // contains both replica
		File file = new File("_" + fList.get(0).owner + "_" + fList.get(0).name);
		
		for(FileObj f : fList) {
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
			
			for(Partition p : f.pList) {
				Socket socket = new Socket(p.disk.ip, p.disk.port);
				BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
				
				String request = "upload\t_" + p.file.owner + "_" + p.file.name + "_p" + p.index; // _user_file_p####
				bos.write(request.getBytes().length);
				bos.flush();
				bos.write(request.getBytes());
				bos.flush();
				
				System.out.println("Starting to upload to storage server " + p + "...");
				int count;
				int total = 0;
				byte[] buffer = new byte[8192];
				
				// when partition is full or file end is reached, stop writing
				while ((count = is.read(buffer)) > 0) {
					bos.write(buffer, 0, count);
					total += count;
					if(total == pSize) break;
				}
				bos.flush();
				System.out.println("File upload completed");
				socket.close();
			}
			
			is.close();
		}
		file.delete();
	}
	
	// fetch all file segments from storage server and reconstruct the original file, for client download
	// since any inconsistency is solved before this, just fetch the first replica
	public static void reconstructFile(long hashValue) throws Exception {
		FileObj f = mapOfHashValue.get(hashValue).get(0);
		
		// get all pieces, write to the file
		File file = new File("_" + f.owner + "_" + f.name);
		BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file, true));
		
		System.out.println("Starting to reconstruct file...");
		for(Partition p : f.pList) {
			Socket socket = new Socket(p.disk.ip, p.disk.port);
			BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
			BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
			
			String request = "download\t_" + p.file.owner + "_" + p.file.name + "_p" + p.index;
			bos.write(request.getBytes().length);
			bos.flush();
			bos.write(request.getBytes());
			bos.flush();
			
			
			int count;
			byte[] buffer = new byte[8192];
			while ((count = bis.read(buffer)) > 0) {
				os.write(buffer, 0, count);
			}
			
			socket.close();
		}
		System.out.println("File reconstruction completed");
		os.close();
	}
	
	// find removed/corrupted file, replicate
	public static boolean checkFile(long hashValue) throws Exception {
		boolean isGood = true;
		
		ArrayList<FileObj> fileList = mapOfHashValue.get(hashValue);
		for(FileObj f : fileList) {
			ArrayList<Partition> pList = f.pList;
			for(Partition p : pList) {
				if(!verifyChecksum(p))
					isGood = false;
			}
		}
		
		return isGood;
	}
	
	// verify the checksum of the file segment in the partition, if removed or corrupted, replace it
	public static boolean verifyChecksum(Partition p) throws Exception {
		FileObj f = p.file;
		String fName = "_" + f.owner + "_" + f.name + "_p" + p.index; // file name in storage disk
		
		// set up for communication with storage server
		Socket socket = new Socket(p.disk.ip, p.disk.port);
		BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
		BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
		
		// send request to check file
		String request = "check\t" + fName;
		bos.write(request.getBytes().length);
		bos.flush();
		bos.write(request.getBytes());
		bos.flush();
		
		// receive reply
		if(bis.read() == 0) { // file removed or corrupted
			System.out.println("file " + p.file + " in " + p + " removed/corrupted");
			
			// fetch file(one segment in one partition) from other storage server to replace it
			int pReplicaIndex = 
					p.index < numOfPartitions/2 ? p.index+numOfPartitions/2 : p.index-numOfPartitions/2;
			Partition pReplica = mapOfPartition.get(pReplicaIndex);
			System.out.println("file will be replaced with replica in " + pReplica);
			
			fetchFile(pReplica);
			storeFile(p);
			
			socket.close();
			return false;
		}
		
		socket.close();
		return true;
	}
	
	// fetch one file segment from one partition, only for recovering removed/corrupted file or partition rearrangement
	public static void fetchFile(Partition p) throws Exception {
		// set up for communication with storage server
		Socket socket = new Socket(p.disk.ip, p.disk.port);
		BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
		BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
		
		String request = "download\t_" + p.file.owner + "_" + p.file.name + "_p" + p.index;
		bos.write(request.getBytes().length);
		bos.flush();
		bos.write(request.getBytes());
		bos.flush();
		
		File file = new File("_" + p.file.owner + "_" + p.file.name);
		try {
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file)); // is to read file
			System.out.println("Starting to download " + file.getName() + " from " + p + "...");
			int count;
			byte[] buffer = new byte[8192];
			while ((count = bis.read(buffer)) > 0) {
				os.write(buffer, 0, count);
			}
			os.close();
			System.out.println("File download completed");
		} catch(Exception e) {
			System.out.println(e.getStackTrace());
		}
		socket.close();
	}
	
	// store one file segment to one partition then delete it, only for recovering removed/corrupted file or partition rearrangement
	public static void storeFile(Partition p) throws Exception {
		// set up for communication with storage server
		Socket socket = new Socket(p.disk.ip, p.disk.port);
		BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
		
		String request = "upload\t_" + p.file.owner + "_" + p.file.name + "_p" + p.index;
		bos.write(request.getBytes().length);
		bos.flush();
		bos.write(request.getBytes());
		bos.flush();
		
		File file = new File("_" + p.file.owner + "_" + p.file.name);
		try {
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(file)); // is to read file
			System.out.println("Starting to upload " + file.getName() + " to " + p + "...");
			int count;
			byte[] buffer = new byte[8192];
			while ((count = is.read(buffer)) > 0) {
				bos.write(buffer, 0, count);
			}
			
			bos.flush();
			bos.close();
			is.close();
			System.out.println("File upload completed");
		} catch(Exception e) {
			System.out.println(e.getStackTrace());
		}
		
		file.delete();
		socket.close();
	}
	
	
	// find failed disk, reassign its partitions and replicate files
	public static boolean checkDisks() throws Exception {
		for(Disk d : listOfDisks) {
			try {
				Socket socket = new Socket(d.ip, d.port);
				
				// Get input and output stream
				InputStream input = socket.getInputStream();
				BufferedInputStream is = new BufferedInputStream(input);
				OutputStream output = socket.getOutputStream();
				BufferedOutputStream os = new BufferedOutputStream(output);
				
				// try a message
				os.write("disk\tcheck".getBytes().length);
				os.flush();
				os.write("disk\tcheck".getBytes());
				os.flush();
				is.read();
				System.out.println(d + " is online");
				
				socket.close();
			} catch(Exception e) {
				System.out.println(d + " is off-line");
				ArrayList<Partition> pList = d.listOfPartitions; // list of partitions need to e replicated
				removeDisk(d.ip + ":" + d.port);
				
				// replicate files in the partitions
				for(Partition p : pList) {
					// fetch file(one segment in one partition) from other storage server to replace it
					int pReplicaIndex = 
							p.index < numOfPartitions/2 ? p.index+numOfPartitions/2 : p.index-numOfPartitions/2;
					Partition pReplica = mapOfPartition.get(pReplicaIndex);
					
					if(pReplica.isOccupied) {
						fetchFile(pReplica);
						storeFile(p);
					}
				}
				System.out.println("lost files are recovered");
				
				return false;
			}
		}
		
		return true;
	}
	
	// check if the disk online
	public static boolean isOnline(String target) {
		Disk d = null;
		try {
			String[] strs = target.split(":");
			d = new Disk(strs[0], Integer.parseInt(strs[1]));
			Socket socket = new Socket(d.ip, d.port);
			
			// Get input and output stream
			InputStream input = socket.getInputStream();
			BufferedInputStream is = new BufferedInputStream(input);
			OutputStream output = socket.getOutputStream();
			BufferedOutputStream os = new BufferedOutputStream(output);
			
			// try a message
			os.write("disk\tcheck".getBytes().length);
			os.flush();
			os.write("disk\tcheck".getBytes());
			os.flush();
			is.read();
			System.out.println(d + " is online");
			
			socket.close();
		} catch(Exception e) {
			System.out.println(d + " is off-line");
			return false;
		}
		return true;
	}
	
	// list all disks with their partitions and files
	public static String listDisks() {
		String reply = "Disk-Partition assignment: " 
				+ listOfDisks.size() + " disks, " + numOfPartitions + " partitions";
		for(Disk d : listOfDisks) {
			reply = reply + "\n\t" + d + " (" + d.listOfPartitions.size() + " partitions)";
			for(Partition p : d.listOfPartitions) 
				reply = reply + "\n\t\t" + p + "\t" + p.file;
		}
		return reply;
	}
	
	// file transfer for download request
	public static void downloadFile(OutputStream os, File file) {
		try {
			// use a loop to read file into a byte array and send to client
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
			int count;
			byte[] buffer = new byte[8192];
			System.out.println("Client start downloading...");
			while ((count = is.read(buffer)) > 0) {
				os.write(buffer, 0, count);
			}
			is.close();
			System.out.println("Client download completed");
		} catch(Exception e) {
			System.out.println(e.getStackTrace());
		}
	}
	
	// file transfer in upload request
	public static void uploadFile(InputStream is, File file) {
		try {
			// use a loop to read from input stream and write to the file
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
			int count;
			byte[] buffer = new byte[8192];
			System.out.println("Starting to receive file...");
			while ((count = is.read(buffer)) > 0) {
				os.write(buffer, 0, count);
			}
			os.close();
			System.out.println("Client upload completed");
		} catch(Exception e) {
			System.out.println(e.getStackTrace());
		}
	}
	
	// use disk ip and port to get the disk
	public static Disk getDisk(String target) {
		String ip = target.split(":")[0];
		int port = Integer.parseInt(target.split(":")[1]);
		
		// compare existing disk with the intended disk
		for(Disk e : listOfDisks) {
			if(ip.equals(e.ip) && port == e.port) { 
				return e;
			}
		}
		
		return null;
	}
	
	// create a message for overwriting an existing file, show both old and new file info
	public static String overwrite(String target, long hashValue) {
		String reply = "";
		ArrayList<FileObj> fileList = mapOfHashValue.get(hashValue);
		//reply = "File " + target + " already exists and is overwritten";
		reply = reply + "\n" + showFileLocation(target, fileList);
		
		// release old partitions
		for(FileObj f : fileList)
			for(Partition p : f.pList)
				p.isOccupied = false;
		
		// delete old file from user map
		ArrayList<ArrayList<FileObj>> listOfFileList = mapOfUser.get(target.split("/")[0]);
		listOfFileList.remove(fileList);
		
		// upload new file, may use different number of partitions
		upload(target, hashValue);
		
		return reply + "\nchanged to: \n" + showFileLocation(target, mapOfHashValue.get(hashValue));
	}
	
	// remove disk
	public static String removeDisk(String target) {
		// find the disk to remove
		Disk disk = getDisk(target);
		if(disk == null) return "Disk already removed";
		
		// get the partition list
		ArrayList<Partition> list = disk.listOfPartitions;
		
		// remove disk from disk list
		listOfDisks.remove(disk);
		
		// move partitions to other disks except the last one
		ArrayList<ArrayList<Partition>> listOfTempList = new ArrayList<ArrayList<Partition>>();
		for(int i = 0; i < listOfDisks.size(); i++)
			listOfTempList.add(new ArrayList<Partition>());
		
		int lIndex = 0; // list index of partition in list of the removed disk
		int dIndex = 0; // disk index
		
		// add the partition to each disk one at a time, then move to next disk
		while(lIndex < list.size()) {
			Partition p = list.get(lIndex);
			int replicaPIndex = 
					(p.index < numOfPartitions/2 ? (p.index+numOfPartitions/2) : (p.index-numOfPartitions/2));
			Partition pReplica = mapOfPartition.get(replicaPIndex);
			Disk toDisk = listOfDisks.get(dIndex);
			ArrayList<Partition> toList = toDisk.listOfPartitions;
			ArrayList<Partition> tempList = listOfTempList.get(dIndex);
			
			if(!toList.contains(pReplica)) { // avoid replica partition in the same disk
				p.disk = toDisk;
				tempList.add(p);
				lIndex++;
				dIndex++;
			}
			else dIndex++;
			
			if(dIndex == listOfDisks.size()) dIndex = 0;
		}
		
		String reply = "List of partitions moved from " + disk + " to other disks: ";
		for(int i = 0; i < listOfTempList.size(); i++) {
			ArrayList<Partition> l = listOfTempList.get(i);
			listOfDisks.get(i).addPartitions(l);
			reply = reply + "\n" + listOfDisks.get(i) + ": " + l.size() + " Partitions";
			for(Partition p : l) {
				reply = reply + "\n\t" + p + "\t" + p.file;
			}
		}
		
		return reply;
		
	}
	
	// add disk
	public static String addDisk(Disk disk) throws Exception {
		// num of partitions not to move to the new disk
		int numRemain = numOfPartitions / (listOfDisks.size() + 1);
		
		ArrayList<Partition> newList = disk.listOfPartitions; // new disk's partition list
		
		// move excessive partition of each disk to the new disk
		for(Disk d : listOfDisks) {
			int count = d.listOfPartitions.size(); // count will decrease to numRemain
			int lIndex = 0; // index the partition list
			ArrayList<Partition> list = d.listOfPartitions;
			ArrayList<Partition> listCopy = new ArrayList<Partition>(list);
			
			while(count > numRemain && lIndex < listCopy.size()) {
				Partition p = listCopy.get(lIndex);
				int replicaPIndex = 
						(p.index < numOfPartitions/2 ? (p.index+numOfPartitions/2) : (p.index-numOfPartitions/2));
				Partition pReplica = mapOfPartition.get(replicaPIndex);
				if(!newList.contains(pReplica)) { // avoid replica partition in same disk
					//System.out.println(p);
					list.remove(p);
					
					// delete file in the partition, if there are any
					if(p.isOccupied) deleteStoredFile(p);
					
					newList.add(p);
					p.disk = disk;
					lIndex++;
					count--;
				}
				else lIndex++;
				
			}
		}
		
		// move files to the new disk(same as recovering lost files)
		for(Partition p : newList) {
			// fetch file(one segment in one partition) from other storage server
			int pReplicaIndex = 
					p.index < numOfPartitions/2 ? p.index+numOfPartitions/2 : p.index-numOfPartitions/2;
			Partition pReplica = mapOfPartition.get(pReplicaIndex);
			
			// replicate the file using the remaining copy
			if(pReplica.isOccupied) {
				fetchFile(pReplica);
				storeFile(p);
			}
		}
		
		listOfDisks.add(disk);
		String reply = newList.size() + " Partitions moved to " + disk;
		for(Partition p : disk.listOfPartitions) {
			reply = reply + "\n\tPartition # -- " + p.index + "\t" + p.file;
		}
		
		return reply;
	}
	
	// list files belonging to user
	public static String listFiles(String owner) {
		String reply = "All files belonging to " + owner + ": ";
		ArrayList<ArrayList<FileObj>> listOfFileList = mapOfUser.get(owner);
		for(ArrayList<FileObj> list : listOfFileList) {
			reply = reply + "\n-- " + list.get(0).name;
			for(int i = 0; i < list.size(); i++) {
				reply = reply + "\n\treplica " + i + ": " + list.get(i).details();
			}
		}
		
		return reply;
	}
	
	// delete file
	public static String deleteFile(String target, long hashValue) throws Exception {
		ArrayList<FileObj> fileList = mapOfHashValue.get(hashValue);
		
		// delete file from server
		for(FileObj f : fileList) {
			for(Partition p : f.pList) {
				deleteStoredFile(p);
			}
		}
		
		// delete file from partition
		for(FileObj f : fileList) {
			for(Partition p : f.pList) {
				p.isOccupied = false;
				p.file = null;
			}
		}
		
		// delete file from user map
		String user = target.split("/")[0];
		ArrayList<ArrayList<FileObj>> listOfFileList = mapOfUser.get(user);
		listOfFileList.remove(fileList);
		
		// delete file from hash value map
		mapOfHashValue.remove(hashValue);
		
		return "File and replicas deleted...";
	}
	
	// delete file from storage server
	public static void deleteStoredFile(Partition p) throws Exception {
		// set up for communication with storage server
		Socket socket = new Socket(p.disk.ip, p.disk.port);
		BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
		
		String request = "delete\t_" + p.file.owner + "_" + p.file.name + "_p" + p.index;
		bos.write(request.getBytes().length);
		bos.flush();
		bos.write(request.getBytes());
		bos.flush();
		socket.close();
	}
	
	// get string showing file infor and location
	public static String showFileLocation(String target, ArrayList<FileObj> fileList) {
		String reply = target.split("/")[0] + "/" + target.split("/")[1] + " (" + fileList.get(0).size + " bytes)" + " is saved in: ";
		for(int i = 0; i < fileList.size(); i++) {
			reply = reply + "\n\treplica " + i + ": " + fileList.get(i);
			for(Partition p :fileList.get(i).pList)
				reply = reply + "\n\t\t" + p;
		}
		return reply;
	}
	
	// create message for upload request, also change file storage info
	public static String upload(String target, long hashValue) {
		String reply = "";
		
		// target format is user/file/size
		String[] strs = target.split("/");
		String owner = strs[0];
		String name = strs[1];
		int size = Integer.parseInt(strs[2].trim());
		
		// get starting partition index to store file, may change if occupied
		int partitionIndex = (int)(hashValue % (long)numOfPartitions);
		System.out.println("hash value: " + hashValue 
			+ ", caluculated partition # (may change if occupied): " + partitionIndex);
			
		// number of partitions needed to store file
		int pNum = size / pSize + (size % pSize == 0? 0 : 1);
		
		// assign partitions to the file
		ArrayList<Partition> pList = new ArrayList<Partition>();
		int count = 0;
		for(int i = partitionIndex; count < pNum; i++) {
			// check if no enough space
			// difference between first and last partition must be less than half # of all partitions
			if(i == numOfPartitions ) i = 0; // wrap up and go to partition 0
			if(i > partitionIndex && i - partitionIndex >= numOfPartitions/2) return "Disk is full";
			if(i < partitionIndex && partitionIndex - i <= numOfPartitions/2) return "Disk is full";
			
			// if occupied, go to next
			Partition p = mapOfPartition.get(i);
			if(!p.isOccupied) {
				p.isOccupied = true;
				pList.add(p);
				count++;
			}
			else continue;
		}		
		
		// create file obj of orignal
		FileObj file = new FileObj(owner, name, size, pList);
		
		// create file replica
		// every partition storing replica is half # of total partitions away in a ring
		ArrayList<Partition> pListReplica = new ArrayList<Partition>();
		for(Partition p : pList) {
			int pReplicaIndex = 
					p.index < numOfPartitions/2 ? p.index+numOfPartitions/2 : p.index-numOfPartitions/2;
			Partition pReplica = mapOfPartition.get(pReplicaIndex);
			pReplica.isOccupied = true;
			pListReplica.add(pReplica);
		}
		
		// create file replica obj
		FileObj fileReplica = new FileObj(owner, name, size, pListReplica);
		
		// create a list to store the file and replica
		ArrayList<FileObj> newList = new ArrayList<FileObj>();
		newList.add(file);
		newList.add(fileReplica);
		
		// update user-list_of_replica_list map
		ArrayList<ArrayList<FileObj>> listOfFileList = mapOfUser.get(owner);
		if(listOfFileList != null) { // if user exists
			listOfFileList.add(newList);
		}
		else {
			listOfFileList = new ArrayList<ArrayList<FileObj>>();
			listOfFileList.add(newList);
			mapOfUser.put(owner, listOfFileList);
		}
				
		// update hash-file_list map
		mapOfHashValue.put(hashValue, newList);
		
		// message to send to client
		reply = showFileLocation(target, newList);
		return reply;
	}
	
	public static long getHashValue(String str) {
		ArrayList<String> listOfChunks = new ArrayList<String>();
		
		// divide the string into chunks of four characters
		int i;
		for(i = 0; 4 * i + 3 < str.length(); i++) {
			listOfChunks.add(str.substring(4*i, 4*i+4));
		}
		if(4 * i + 3 >= str.length() - 1) {
			listOfChunks.add(str.substring(4 * i));
		}
		
		// convert chunks to reversed binary string
		ArrayList<String> listOfBinaries = new ArrayList<String>();
		for(int k = 0; k < listOfChunks.size(); k++) {
			String s = listOfChunks.get(k);
			String temp = "";
			for(int j = 0; j < s.length(); j++){
				String tempChar = Integer.toBinaryString(s.charAt(j));
				while(tempChar.length() < 8) tempChar = "0" + tempChar;
				temp += tempChar;
			}
			while(temp.length() < 32) temp += "0";
			
			// reverse the odd chunk binary strings
			if(k % 2 == 0)
				temp = new StringBuilder(temp).reverse().toString();
			listOfBinaries.add(temp);
		}
		
		// find XOR or all binary strings
		long result = 0;
		for(String s : listOfBinaries) {
			result = result ^ Long.parseLong(s, 2);
		}
		return result;
	}
}
