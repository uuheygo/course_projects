import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;

// storage server interact with proxy server by upload and download operations
// upload: store new file, overwrite old file, replace corrupted file, backup remove file replica
// download: send to client, used by proxy server to replace corrupted file or removed file replica

public class Sserver {
	public static final int PORT = 0; // port on storage server is fixed
	public static final int PORT_START = 1024; // port # to start searching at
	public static final int PORT_END = 65535; // port # to end searching at
	public static HashMap<String, String> map = new HashMap<String, String>(); // filename - checksum
	
	public static void main(String[] args) {
		// bind storage server to a random available port
		ServerSocket serverSocket = null;
		
		for(int i = 0; i < 1000; i++) {
			try{
				serverSocket = new ServerSocket(0);
				System.out.println("Storage server: " + InetAddress.getLocalHost().getHostName() + " " 
						+ InetAddress.getLocalHost().getHostAddress() 
						+ ", listening on port " + serverSocket.getLocalPort() + "...");
				break;
			} catch(Exception e) {
				e.printStackTrace();
				continue;
			}
		}
		
		// server request from proxy server
		while(true) {
			try {
				// Accept requests from proxy server
				Socket socket = serverSocket.accept();
				System.out.println("Accepted connection: " + socket);
				
				// Get input and output stream
				InputStream input = socket.getInputStream();
				BufferedInputStream is = new BufferedInputStream(input);
				OutputStream output = socket.getOutputStream();
				BufferedOutputStream os = new BufferedOutputStream(output);
				
				// read request
				int count = is.read(); // request length
				byte[] bytes = new byte[count];
				String inputStr = "";
				is.read(bytes, 0, count); // num of bytes in request
				inputStr = new String(bytes, "UTF-8"); // convert bytes to string
				System.out.println("request received: " + count + " bytes, " + inputStr);
				
				// process request
				String[] request = inputStr.split("\t"); // split operation and target in request
				String operation = request[0].trim();
				String target = request[1].trim();
				
				// file operation, located in /tmp/
				File file = new File("/tmp/" + target);
				
				// upload
				if(operation.equals("upload")) {
					String checksum = doUpload(is, file); // calculate checksum while passing file
					map.put(target, checksum); // insert or update file-checksum pair
				}
				
				else if(operation.equals("download")) {
					doDownload(os, file);
				}
				else if(operation.equals("check")) {
					// if file is good, return 1 to proxy
					// if file is removed or corrupted, return 0 to proxy
					if(!file.exists() || !getChecksum(file).equals(map.get(target))) {
						System.out.println("file removed/corrupted");
						os.write(0);
					}
					else os.write(1);
					os.flush();
				}
				else if(operation.equals("delete")) {
					file.delete();
					map.remove(target);
				}
				else if(operation.equals("disk")) { // check if disk online
					os.write(1);
					os.flush();
				}
				os.close();
				System.out.println("\n");
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	// use md5 to calculate checksum
	public static String getChecksum(File file) throws Exception {
		String r = "";
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		FileInputStream fis = new FileInputStream(file);
		byte[] dataBytes = new byte[8192];
		
		// read file and update md
		int nread = 0; 
		while ((nread = fis.read(dataBytes)) != -1) {
			md.update(dataBytes, 0, nread);
		};
		fis.close();

		byte[] mdbytes = md.digest();

		// convert the byte to hex format
		StringBuffer sb = new StringBuffer("");
		for (int i = 0; i < mdbytes.length; i++) {
			sb.append(Integer.toHexString((mdbytes[i] & 0xff) + 0x100).substring(1));
		}
		r = sb.toString();
		System.out.println("Checksum in hex format: " + file.getName() + " ~ " + r);
		return r;
	}
	
	
	// separate checksum verification and download into two operation
	// proxy server download
	public static void doDownload(OutputStream os, File file) {
		try {
			// use a loop to read file into a byte array and send to client
			System.out.println("Starting to transfer file to proxy server...");
			BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
			int count;
			byte[] buffer = new byte[8192];
			while ((count = is.read(buffer)) > 0) {
				os.write(buffer, 0, count);
			}
			is.close();
			
			System.out.println("Proxy server download completed");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	// accept upload file from proxy server and return checksum of the file
	public static String doUpload(InputStream is, File file) {
		String r = "";
		
		// new file or overwrite
		if(!map.containsKey(file.getName())) {
			System.out.println("Receive a new file");
		}
		else {
			System.out.println("Overwrite an existing file");
		}
		
		try {
			// use a loop to read from input stream and write to the file
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
			MessageDigest md = MessageDigest.getInstance("MD5");
			int count;
			byte[] buffer = new byte[8192];
			System.out.println("Starting to receive file...");
			while ((count = is.read(buffer)) > 0) {
				os.write(buffer, 0, count);
				md.update(buffer, 0, count);
			}
			os.close();
			
			// convert the byte to hex format
			byte[] mdbytes = md.digest();
			StringBuffer sb = new StringBuffer("");
			for (int i = 0; i < mdbytes.length; i++) {
				sb.append(Integer.toHexString((mdbytes[i] & 0xff) + 0x100).substring(1));
			}
			
			r = sb.toString();
			System.out.println("Checksum in hex format: " + r);
			
			System.out.println("Upload completed");
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return r;
	}
}
