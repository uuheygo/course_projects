import java.io.*;
import java.net.*;
import java.util.*;


public class Client {

	public static void main(String[] args) {
		// read command line for server info
		String ipOrName = args[0];
		String port = args[1];
		
		Socket socket = null;
		BufferedOutputStream os = null;
		BufferedInputStream is = null;
		Scanner scanner = null;
		InputStream input = null;
		OutputStream output = null;
		
		// connect to server by name of ip
		try {
			socket = new Socket(ipOrName, Integer.parseInt(port));
			input = socket.getInputStream();
			output = socket.getOutputStream();
			is = new BufferedInputStream(input);
			os = new BufferedOutputStream(output);
		} catch(Exception e) {
			System.out.println("Cannot connect to server\nExiting...");
			System.exit(0);
		}
		
		// process user request
			try {
				// read request from stdin
				System.out.print("Enter a request or exit (press ctrl+c or enter exit): ");
				scanner = new Scanner(System.in);
				String request = "";
				request = scanner.nextLine();
				System.out.println("Request entered: " + request);
				
				// client exit
				if(request.trim().equalsIgnoreCase("exit")) {
					os.close();
					is.close();
					scanner.close();
					socket.close();
					System.exit(0);
				}
				
				// parse request: operation type and target
				String[] strs = request.split(" ");
				if(strs.length != 2) {
					System.out.println("Invalid request");
					System.exit(0);
				}
				String operation = strs[0];
				String target = strs[1];
				
				// check and covert request
				// 0 -- download user/file
				// 1 -- upload user/file
				// 2 -- delete user/file
				// 3 -- list user
				// 4 -- add disk
				// 5 -- remove disk
				String outRequest = "";
				switch(operation) {
					case "download": 
						outRequest = "0\t" + target;
						break;
					case "upload":
						outRequest = "1\t" + target;
						break;
					case "delete":
						outRequest = "2\t" + target;
						break;
					case "list":
						outRequest = "3\t" + target;
						break;
					case "add":
						String diskAddr = formatDisk(target); // convert host name to ip
						if(diskAddr != null) outRequest = "4\t" + diskAddr;
						else {
							System.out.println("Invalid disk name/address");
							System.exit(0);
						}
						break;
					case "remove":
						String addr = formatDisk(target); // convert host name to ip
						if(addr != null) outRequest = "5\t" + addr;
						else {
							System.out.println("Invalid disk name/address");
							System.exit(0);
						}
						break;
					case "show":
						outRequest = "11\tdisks"; // show disk info
						break;
					default:
						System.out.println("Invalid request");
						System.exit(0);
				}
				
				// send request to server, set up for file transfer
				File file = null;
				
				if(operation.equals("upload")) { // upload file
					file = new File(target.split("/")[1]);
					if(!file.exists()) {
						System.out.println("File does not exist\nExiting");
						System.exit(0);
					}
					int size = (int)file.length();
					outRequest = outRequest + "/" + size;
				}
				else if(operation.equals("download")) { // download file
					file = new File(target.split("/")[1]);
				}
				os.write(outRequest.getBytes());
				os.flush();
				
				// read reply length from server
				byte[] bytes = new byte[8192];				
				int count;
				byte[] copy;
				String out = "";
				count = is.read(bytes, 0, bytes.length);
				copy = new byte[count];
				System.arraycopy(bytes, 0, copy, 0, count);
				out += new String(copy, "UTF-8");
				int len = Integer.parseInt(out);
				os.write(0);
				os.flush();
				
				// read reply from server
				out = "";
				int total = 0;
				while(total < len) {
					count = is.read(bytes, 0, bytes.length);
					total += count;
					copy = new byte[count];
					System.arraycopy(bytes, 0, copy, 0, count);
					out += new String(copy, "UTF-8");
				}
				System.out.println(out);
				
				// do file transfer
				if(operation.equals("upload") && !out.equals("Disk is full")) {
					uploadFile(os, file);
				}
				if(operation.equals("download") && !out.contains("No file")) {
					os.write(1); // send response to server
					os.flush();
					
					// get file type from server
					count = is.read();
					os.write(1);
					os.flush();
					is.read(bytes, 0, count);
					os.write(1);
					os.flush();
					copy = new byte[count];
					System.arraycopy(bytes, 0, copy, 0, count);
					String fileType = new String(copy);
					
					downloadFile(is, file, fileType);
				}
				
			} catch(Exception e) {
				System.out.println(e.getStackTrace());
			}

	}
	
	// upload file to server
	public static void uploadFile(OutputStream os, File file) {
		try {
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(file)); // is to read file
			System.out.println("Starting to upload file...");
			int count;
			byte[] buffer = new byte[8192];
			
			while ((count = is.read(buffer)) > 0) {
				os.write(buffer, 0, count);
			}
			
			os.flush();
			os.close();
			is.close();
			System.out.println("File upload completed");
		} catch(Exception e) {
			System.out.println(e.getStackTrace());
		}
	}
	
	// download file from server
	public static void downloadFile(InputStream is, File file, String fileType) {
		try {
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file)); // is to read file
			
			
			System.out.println("Starting to download...\n");
			System.out.println(file.getName() + ", " + fileType);
			boolean isText = fileType.equals("text/plain")? true : false;
			
			System.out.println("------------ File content ----------");
			if(!isText) 
				System.out.println("Not a text/plain file");
			
			int count;
			byte[] buffer = new byte[8192];
			while ((count = is.read(buffer)) > 0) {
				// display file content
				byte[] copy = new byte[count];
				System.arraycopy(buffer, 0, copy, 0, count);
				
				if(isText) // print content if text file
					System.out.print(new String(copy));
				
				os.write(buffer, 0, count);
			}
			System.out.println("---------- File content end --------\n");
			os.close();
			System.out.println("File download completed");
		} catch(Exception e) {
			System.out.println(e.getStackTrace());
		}
	}
	
	public static boolean hasTwoTokens(String target) {
		if(target.split("/").length != 2) {
			System.out.println("Invalid user/file name");
			return false;
		}
		return true;
	}
	
	// convert host name to ip
	public static String formatDisk(String target) {
		String ip = "";
		int port = 0;
		
		String[] strs = target.split(":");
		if(strs.length != 2) { // must have two tokens
			return null;
		}
		
		// If host name is used, convert it to IP
		try {
			InetAddress address = InetAddress.getByName(strs[0]);
			ip = address.getHostAddress();
		} catch(Exception e) {
			return null;
		}
		
		// assign port number
		try {
			port = Integer.parseInt(strs[1]);
		} catch(Exception e) {
			return null;
		}
		if(port < 1024 || port > 66535) {
			return null;
		}
		
		return ip + ":" + port;
	}

}
