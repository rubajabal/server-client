import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


public class Xserver {

	public static void main(String[] args){

		if (args.length != 2) {
			System.err.println("Usage: java Xserver serverPort rootDir");
			System.exit(1);
		}
       
		int serverPort = Integer.parseInt(args[0]);
		String rootDir = args[1];
		//boolean activeConnection = true;
		//int counter = 0;

		try (ServerSocket serverSocket = new ServerSocket(serverPort, 10);) { // Port Number and the size of the pending connections queue
			System.out.println("Server started");
			System.out.println("Waiting for a client ...");

			while (true) {
				try {
					//Accept incoming network connections
					Socket clientSocket = serverSocket.accept();				
					System.out.println("Client Connection Accepted");

					//call handleConnection Method 
					handleConnection(clientSocket, rootDir);	
				}		
				catch (IOException e) {
					System.out.println(e.getMessage());
					System.out.println("Exception caught when trying to listen on port " + serverPort);
					System.exit(1);
				}
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.out.println("Exception caught when trying to listen on port " + serverPort);
			System.exit(1);

		}
	}

	private static void handleConnection(Socket clientSocket, String rootDir) {
		
		try {
			Runnable runnable=new SingleThread(clientSocket,rootDir);
			Thread thread=new Thread(runnable);
			try {				
				clientSocket.setSoTimeout(5000);				
			}catch(SocketException e) {
				e.printStackTrace();
			}
			
			thread.start();
			thread.join();
			
		}catch( InterruptedException e) {
			e.printStackTrace();
		}		
	}
}

class SingleThread implements Runnable {
	
	private Socket socket;
	private String rootDir;
	
	public SingleThread(Socket clientSocket, String rootDir) {
		this.socket=clientSocket;
		this.rootDir=rootDir;		
	}

	@Override
	public void run() {
		
		try(
			// read input from client
			BufferedReader in = new BufferedReader( new InputStreamReader(socket.getInputStream()));
			// writes to the client
			PrintWriter output = new PrintWriter(socket.getOutputStream(), true);			
		){
	         
	         String RequestLine;
	         StringBuilder headerRequest = new StringBuilder();
	         
	         while(!(RequestLine = in.readLine()).isEmpty()) {
	        	 headerRequest.append(RequestLine).append("\n");  
	         }	
	         System.out.println(headerRequest); 
	         
        	 int status=HttpRequest.handleRequest(headerRequest);
           	 System.out.println("the return code is: " + status); 
        	 
        	 if (status==1) {
        		//Send Bad Request
        		 HttpResponse.BadRequest(output, "Bad Request!");   		 
        	 }
        	 else if (status==2) {
        		//Send OK Request       		 
        		 HttpResponse.StatusOK(output, "Welcome to Our World!");     		 
        	 }
        	 else if (status==3) {
        		//Send File Request
        		 String fullPath=rootDir+HttpRequest.filePath;
        		 
        		// Attempt to read and send the file content	                 
                 try {
                	 File File = new File(fullPath);
                	 HttpResponse.RequestedFile(output, File);
                 }
                 
                 catch(FileNotFoundException e) {
                     HttpResponse.FileNotFound(output, "File is Not Found");                   	 
                 }       		 		 
        	 }
        	 
         	output.write("\r\n");
        	output.flush();
	     }	
			
			catch (IOException e) {
				System.out.println(e.getMessage());
				System.err.println("Exception caught when trying to handle Connection" );
				System.exit(1);
		}		
	}	
}

class HttpRequest {
    public static String filePath;

	public static int handleRequest(StringBuilder Request) {
		
		//get the Request Lines      
		String[] requestHeader = Request.toString().split("\n");
		if (requestHeader.length!=2 || !requestHeader[0].startsWith("GET") || !requestHeader[1].startsWith("Host:")) {
			return 1;
		}
		else {
			String method = (requestHeader[0].split(" "))[0];
            filePath = (requestHeader[0].split(" "))[1];
            String httpv = (requestHeader[0].split(" "))[2];
            //String host_port = (requestHeader[1].split(" "))[1];	     
                      
            if(!method.equals("GET") || !httpv.equals("HTTP/1.1")) {	
	            return 1;	
             }  
             else if(filePath.equals("/")) {
             	return 2;
             }
             else if (filePath.length()>1) {
             	return 3;           	
             } 			
		}                     
			return 0;   	 
     }	
}

class HttpResponse {
    public static void BadRequest(PrintWriter output, String content) {
    	output.write("HTTP/1.1 400 Bad Request\r\n");
    	output.write("Content-Length: " + content.length());
    	output.write("\r\n");
    	output.write(content);
    }
    
    public static void StatusOK(PrintWriter output, String content) {     	
    	output.write("HTTP/1.1 200 OK\r\n");
    	output.write("Content-Length: " + content.length());
    	output.write("\r\n");
    	output.write(content);
    	System.out.println("Response has been sent");
    }
    
    public static void RequestedFile(PrintWriter output, File file) throws FileNotFoundException{   	  	
		String FileContent;		
		long fileSize = file.length();
		output.write("HTTP/1.1 200 File Found\r\n");		
		output.write("Content-Length: " + fileSize);
		output.write("\r\n");
		
		try (BufferedReader FileReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));){		
			while ((FileContent = FileReader.readLine()) != null) {			
				output.write(FileContent);				
			}

		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
    }
    
    public static void FileNotFound(PrintWriter output, String content) {
    	output.write("HTTP/1.1 404 File Not Found\r\n");
    	output.write("Content-Length: " + content.length());
    	output.write("\r\n");
    	output.write(content);
    }
}