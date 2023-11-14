
import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//References: https://www.w3schools.com/java/ref_string_lastindexof.asp

public class Xurl {
	
	public static void main(String[] args){
        
        if (args.length != 1) {
            System.err.println(
                "Usage: java Xurl <url>");
            System.exit(1);
        }
        
        MyURL myURLInstance;         
        String hostName=null;
        int portNumber=0;
        String fileName=null;
             
        try {       	
        	//Get the URL Elements
            myURLInstance=new MyURL(args[0]);         
            hostName=myURLInstance.getHost();
            portNumber = myURLInstance.getPort() != -1 ? myURLInstance.getPort() : 80;
            fileName= getFileName(args[0]);
        	
        	//Establishes a connection to the server 
        	Socket Socket = new Socket(hostName, portNumber);
        	Socket.setKeepAlive(false);      	  
        	Socket.setSoTimeout(5000);
        	
			//Create OutputStream to Send Request to the Server  
        	OutputStream outputStream=Socket.getOutputStream();
			PrintWriter out = new PrintWriter(outputStream, true);
        		
        	//Create InputStream To Get Response from the server 
        	InputStream inputStream = Socket.getInputStream(); //binary data
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream)); //text data
            
        	//Create OutputStream to Store Response Content to a local file    
        	PrintWriter outputFile = new PrintWriter(fileName);
        		          
            //Send Request to the Server 
        	String HostReq=portNumber != 80 ? ( hostName + ':' + portNumber) : hostName;      			   	
        	out.write("GET " + myURLInstance.getPath() + " HTTP/1.1\r\n");
        	out.write("Host: " + HostReq + "\r\n" );        	
        	out.write("\r\n");
        	out.flush();
        	      	       	     	
            String responseLine;
            int ContLen = 0; //to store Content Length
            int statusCode=0; //to store StatusCode
            String newLocation = null; // to Handle redirection
            int bytesRead=0; //to terminate the connection when bytesRead=ContLen
            
            // System.out.println(responseLine);     
            //Create a StringBuilder to store the header           
            //StringBuilder headerResponse = new StringBuilder();
            
            
            //Retrieve Status Code, Content Length, & Redirection Location if any
            while ((responseLine = in.readLine())!= null) {  
            	System.out.println(responseLine);   
            	
	            if (responseLine.startsWith("HTTP")) {
	            	statusCode=getStatusCode(responseLine);		
	            }          	
	            if (responseLine.startsWith("Content-Length: ")) {
	            		ContLen=getContentLen(responseLine);		
	            } 
	            if (responseLine.startsWith("Location: ")) {
	                newLocation = responseLine.substring("Location: ".length());
	                break;
	            }
	            
	            if(responseLine.isEmpty()) {       	     
	                break; // End of headers
	            }
	            //headerResponse.append(responseLine).append("\n");
	            //System.out.println(headerResponse);           
            }
                                       	
			if (statusCode >= 200 && statusCode < 300) { 		
			//PrintWriter outputFile = new PrintWriter(fileName);
		 
	           while ((responseLine = in.readLine())!= null) {      
	            //System.out.println("received: " + responseLine);           	
	            bytesRead=bytesRead+(responseLine +System.lineSeparator()).length();    
	            outputFile.println(responseLine);   
	            if(bytesRead==ContLen) {
	            	break;          	
	            }         	                    
	        }
	            System.out.println("File downloaded successfully.");
	            
	            
	        } else if (statusCode>=300 && statusCode<400) {
	            System.out.println(newLocation);
	            if (newLocation != null) {
	                System.out.println("The file has moved to a new location: " + newLocation);
	                // Recursively call the program with the new URL
	                main(new String[]{ newLocation});
	            } else {
	                System.err.println("Error: Redirected, but no new location found.");
	            }
	        } 
			
	        else if (statusCode>=400 || statusCode<500) {
	            System.err.println("Error: HTTP response Code " + statusCode);
	            System.out.println("No File Found or Bad Request");
	            System.exit(1);
	        }
	        
	        else {	         
	            System.err.println("Error: HTTP response Code " + statusCode);
	        }
			
			//close the Resources 
			in.close();
			outputFile.close();
			Socket.close();                          
            
        } 
		  catch (IllegalArgumentException e) {
			    System.out.println(e.getMessage());
			    System.err.println("Invalid URL " + args[0]);
			    System.exit(1);
		} 
          catch (UnknownHostException e) {
            System.err.println("Unknown Host " + hostName);
            System.exit(1);
        } catch (IOException e) {
        	System.out.println(e.getMessage());
            System.err.println("Couldn't get I/O for the connection to " + args[0]);
            System.exit(1);
        } 
    }
	
//-------------------------------------------------------------//
	
	//Return File Name Method 	
	public static String getFileName(String url) {
		String fileName=null;
		
        //split the file name from the path
        int i = url.lastIndexOf('/'); 
        
        //create file with the name index if no file name specified after '/'        
        if(i>=i && (url.substring(i+1)).isEmpty()) {
        	fileName="index";
        }
        
        // Use substring to get the file name after the last '/'
        else if (i >= 0 && !(url.substring(i+1)).isEmpty() ) {         
        	fileName =  url.substring(i + 1);       
        } 
        //System.out.print(fileName);
		return fileName;		
	}
	
	//Return Status Code Method 	
	public static int getStatusCode(String Response) {
		
    	//define the regex reads the HTTP Header Response 
        final String CodeRegex = "HTTP/1.\\d (\\d+) .*";
        final Pattern pattern1 = Pattern.compile(CodeRegex);
        final Matcher matcher1 = pattern1.matcher(Response);
        
        int statusCode;
        if (matcher1.matches()) {
            statusCode = Integer.parseInt(matcher1.group(1));
           // System.out.println(statusCode);
            return statusCode;
        }
        
        else {
        	throw new IllegalArgumentException("ERROR: Failed to Retrieve the Status Code");
        }      
	}
	
	
	//Return Content Length Method 
	public static int getContentLen(String Response) {
		
    	final String Len_Regex = "Content-Length: ([0-9]+)";	       
        final Pattern pattern2 = Pattern.compile(Len_Regex);      
        final Matcher matcher2 = pattern2.matcher(Response);
        
        int ContLen;
        if (matcher2.matches()) {
        	ContLen = Integer.parseInt(matcher2.group(1));
            //System.out.println(ContLen);
            return ContLen;
        }
        
        else {
        	throw new IllegalArgumentException("ERROR: Failed to Retrieve the Content Length");
        }		
	}
}
