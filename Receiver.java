import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

public class Receiver {
    private DatagramSocket      _socket;
    private int                 _port;
    private boolean             _continueService;

    private static final int SEGMENT_SIZE = 10;
    private static final int BUFFER_SIZE = 54;

    // Utility class to create network header for RDT packet
    private Utility utility = new Utility();
    
    /**
     * Receiver constructor
     * 
     * @param port: Port number that Receiver will receive and send messages on
     */
    public Receiver(int port) {
        this._port = port;
    }



    /**
     * Establishes a datagram socket to bind the specified port to
     * 
     * @return - 0 or a negative number describing an error code if the connection could not be established
     */
    public int createSocket() {
        try {
            this._socket = new DatagramSocket(this._port);
        } catch (SocketException e){
            System.err.println("Unable to create and bind to socket");
            return -1;
        }
        return 0;
    }



    /**
     * Closes open datagram socket
     * 
     * @return - 0, if no error; otherwise, a negative number indicating the error
     */
    public int closeSocket() {
        this._socket.close();
        return 0;
    }



    /**
     * Displays the reponse given to the receiver
     * 
     * @param response - the reponse as a string 
     */
    public void printResponse(String response){
        System.out.println("FROM SERVER: " + response);
    }


    public int sendResponse(String srcIP, String srcPort, String destIP, String destPort, String networkIP, String networkPort, String seqNum, String checksum){

        System.out.println("Dest Port: " + destPort);
        // String networkHeader = this.utility.createNetworkHeader(srcIP, srcPort, destIP, destPort, response);
        String networkHeader = this.utility.createNetworkHeader(networkIP, Integer.toString(this._port), destIP, destPort, seqNum + checksum + "ACK" + seqNum);

        DatagramPacket packet = this.utility.createDatagramPacket(networkHeader, networkIP, networkPort, BUFFER_SIZE);

        if(packet != null) {
            try {
                this._socket.send(packet);
                System.out.println("Receiver's Response: " + networkHeader);
            } catch (IOException e) {
                System.err.println("Error: Failed to send message");
                return -1;
            }
            return 0;        
        }
        System.err.println("Error: Failed to create message");
        return -1;
    }



    /**
     * Receives client request by calling upon underlying UDP protocol
     * @return - datagram containing the client request
     */
    public DatagramPacket receiveRequest() {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packetToReceive = new DatagramPacket(buffer, BUFFER_SIZE);

        try {
            // Call to underlying UDP receive method
            this._socket.receive(packetToReceive);
        } catch (IOException e){
            System.err.println("Unable to receive message from client");
            return null;
        }

        return packetToReceive;
    }

    


    /**
     * Server will listen for requests and generate responses until a <shutdown/> message is passed
     */
    public void run() {
        ArrayList<String> messagesBuffer = new ArrayList<String>();        
        this._continueService = true;


        while (this._continueService){
            System.out.println("Receiver listening on port " + this._socket.getLocalPort());

            DatagramPacket newDatagramPacket = this.receiveRequest();
            System.out.println("Host IP: " + newDatagramPacket.getAddress());
            System.out.println("Host Name: " + newDatagramPacket.getAddress().getHostName());

            String request = new String(newDatagramPacket.getData());
            HashMap<String, String> networkHeaderPortions = utility.parseNetworkHeader(request);
            // networkHeaderPortions.put("destPort", Integer.toString(newDatagramPacket.getPort()));

            String message = networkHeaderPortions.get("message").substring(3);

            // Check for stop bit

            System.out.println("Sender IP: " + networkHeaderPortions.get("srcIP"));
            System.out.println("Sender Request: " + message);

            System.out.println("");


            // Message does not exist in message buffer yet, add it
            if (!messagesBuffer.contains(message)){
                messagesBuffer.add(message);
            }

            if (networkHeaderPortions.get("message").charAt(2) == '1'){
                System.out.print("FINAL MESSAGE: ");
                for (String segment : messagesBuffer){
                    System.out.print(segment);
                }
                System.out.println("");
                messagesBuffer = new ArrayList<String>();
            }


            if (request != null) {
                
                if (message == "STOP"){
                    this._continueService = false;
                }

                String response = "<echo>" + message + "</echo>";
                this.utility.printNetworkHeaderPortions(networkHeaderPortions);
                // this.sendResponse(networkHeaderPortions.get("destIP"),
                //                   networkHeaderPortions.get("destPort"),
                //                   networkHeaderPortions.get("srcIP"),
                //                   networkHeaderPortions.get("srcPort"),
                //                   Integer.toString(newDatagramPacket.getPort()),
                //                   response);
                this.sendResponse(networkHeaderPortions.get("destIP"),
                                  networkHeaderPortions.get("destPort"),
                                  networkHeaderPortions.get("srcIP"),
                                  networkHeaderPortions.get("srcPort"),
                                  newDatagramPacket.getAddress().getHostAddress(),
                                  Integer.toString(newDatagramPacket.getPort()),
                                  String.valueOf(networkHeaderPortions.get("message").charAt(0)),
                                  String.valueOf(networkHeaderPortions.get("message").charAt(1)));
            }
        }
        
    }

    

    public static void main(String[] args){
        Receiver server;
        String   serverName;
        String   req;

        

        if (args.length != 1){
            System.err.println("Missing argument.  Usage: Java Receiver <port number>\n");
            return;
        }

        int portNum;

        // Try to parse port number from user argument
        try {
            portNum = Integer.parseInt(args[0]);
        } catch (NumberFormatException e){
            System.err.println("Invalid argument, must be integer.  Usage: Java Receiver <port number>\n");
            return;
        }

        server = new Receiver(portNum);

        // Error while creating socket
        if (server.createSocket() < 0){
            return;
        }

        server.run();
        server.closeSocket();
    }
}
