/** 
* This is a sender program that implements RDT 3.0 protocol using UDP. 
* The sender sends a message to a receiver through a network. 
* The message is broken into packets and sent one by one. 
* Sender handles packets loss and corruption by retransmitting the packets until it receives a valid ACK from receiver.
  
* @authors:   Ben Yanick and Gina  Wittman
* @date:      08/08/2023
   
* COP5518 Project2
* File name: Sender.java
*/


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;

//Sender class representing the sender application in RDT 3.0 protocol using UDP
public class Sender {
    private DatagramSocket      _socket;  // UDP socket used for communication
    private int                 _port;    // Sender's port number
    private String receiverIP;
    private String receiverPort;
    private String destIP;
    private String destPort;
    private static final int SEGMENT_SIZE = 10;   // Fixed size for the segment
    private static final int BUFFER_SIZE = 54;    // Fixed size for the buffer used in DatagramPacket

    private static final String SOURCE_IP = "127.0.0.1";
    private static final String DEST_IP = "127.0.0.1";

    // Utility class to create network headers for RDT packets
    private Utility utility = new Utility();

    /**
     * Constructor for the Sender class.
     * 
     * @param port         The sender's port number.
     * @param receiverIP   The IP address of the receiver.
     * @param receiverPort The port number of the receiver.
     * @param destIP       The destination IP address (for network).
     * @param destPort     The destination port number (for network).
     */
    public Sender(int port, String receiverIP, String receiverPort, String destIP, String destPort){
        this._port = port;
        this.receiverIP = receiverIP;
        this.receiverPort = receiverPort;
        this.destIP = destIP;
        this.destPort = destPort;
    }

    /**
     * Creates socket to bind the specified port to
     *
     * @return - 0, if no error; otherwise, a negative number indicates an error
     */
    public int createSocket() {
        try {
            this._socket = new DatagramSocket(this._port);
            this._socket.setSoTimeout(4000);
        } catch (SocketException e){
            System.err.println("Unable to create and bind to socket");
            return -1;
        }
        return 0;
    }

    /**
     * Connects and binds to the specified port
     * 
     * @param hostname - IP address that port where located
     * @param port     - Port to bind to
     * @return         - If there is an error, will return non-zero value
     */
    public int connectSocket(String hostname, String port) {
        try {
            this._socket.connect(InetAddress.getByName(hostname), Integer.parseInt(port));
        } catch (UnknownHostException ex) {
            System.err.println("Error: Unable to connect to host");
            return -1;
        } catch (NumberFormatException ex) {
            System.err.println("Error: Unable to connect to host. Invalid port number");
            return -2;
        }

        return 0;
    }

    /**
     * Closes open socket
     * 
     */
    public int closeSocket() {
        this._socket.close();
        return 0;
    }
    
    /**
     * Displays the reponse given to the Sender
     * 
     * @param response - the reponse as a string 
     */
    public void printResponse(String response){
        System.out.println("FROM RECEIVER: " + response + "\n");
    }
    
    /**
     * Calls on the utility class to create the network header and datagram packet to send to the Network
     * 
     * @param segment       - Message segment to send
     * @param senderIP      - IP address of Sender
     * @param senderPort    - Port number of Sender
     * @param networkIP     - IP address of Network
     * @param networkPort   - Port number of Network
     * @param receiverIP    - IP address of Receiver
     * @param receiverPort  - Port number of Receiver
     * @return              - 0, if no errors; otherwise, non-zero value indicates error
     */
    public int sendRequest(String segment, String senderIP, String senderPort, String networkIP, String networkPort, String receiverIP, String receiverPort){
        String networkHeader = this.utility.createNetworkHeader(senderIP, senderPort, receiverIP, receiverPort, segment);

        DatagramPacket packet = this.utility.createDatagramPacket(networkHeader, networkIP, networkPort, BUFFER_SIZE);

        //  If the packet is created, send it through the datagram socket
        if(packet != null) {
            try {

                // Call underlying UDP sending method
                this._socket.send(packet);
                System.out.println("Senders's Request: " + networkHeader);
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
     * Receive the response from the Network (indirectly the Receiver) and return as string
     * @param sequenceNum - Sequence number to check against ACK byte
     * @return            - String representing response message
     */
    public String receiveResponse(char sequenceNum){
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, BUFFER_SIZE);
        String message;

        try {
            // Call to underlying UDP receive method
            this._socket.receive(receivedPacket);

            HashMap<String, String> networkPortions = this.utility.parseNetworkHeader(new String(receivedPacket.getData()));

            message = networkPortions.get("message");

            char receivedAckByte = message.charAt(0);
            char checksumByte = message.charAt(1);
            
            // Check if there are any errors indicated by ACK or checksum bytes
            if (receivedAckByte != sequenceNum || checksumByte != '0'){
                System.err.println("Error: Incorrect ACK byte received or corruption of packet detected by non-zero checksum");
                return "ACK||CHECK";
            }

        // Timeout occured while waiting for response
        } catch (SocketTimeoutException e){
            System.err.println("Socket Timeout Occured");
            return "TIMEOUT";

        } catch (IOException e) {
            System.err.println("Unable to receive message from client");
            return null;
        }

        return message;
    }

    /**
     * The main method for the Sender application.
     * 
     * @param args Command-line arguments: <sender_port> <receiver_IP> <receiver_port> <network_IP> <network_Port>
     */
    public static void main(String[] args) {
        Sender sender;
        String message;

        // Make sure proper amount of command line arguments are passed in
        if (args.length != 5){
            System.err.println("Usuage: java Sender <sender_port> <receiver_IP> <receiver_port> <network_IP> <network_Port>");
            return;
        }

        try {
            sender = new Sender(Integer.parseInt(args[0]),
                                args[1],
                                args[2],
                                args[3],
                                args[4]);
        
        // Error occured when providing arguments to Sender constructor
        } catch (NullPointerException e) {
            System.err.println("Usuage: java Sender <sender_port> <receiver_IP> <receiver_port> <network_IP> <network_Port>");
            return;
        }

        if (sender.createSocket() < 0){
            System.err.println("Error: Failed to create socket at port " + args[0]);
            return;
        }

        // Read in user provided message
        Scanner scan = new Scanner(System.in);
        System.out.println("Beginning Sender." + " Port: " + args[0]);
        System.out.print("Enter message: ");
        message = scan.nextLine();

        // Scanner is no longer needed so close it
        scan.close();

        // Create segments array.  Messages will only be 7 bytes with 3 bytes for SEQ#, checksum, term bit followed by 7 byte message
        String[] segments = new String[(int) Math.ceil(message.length() / 7.0)];

        int sequenceNum = 0;
        int messageStartIDX = 0;
        int messageEndIDX = 0;

        // Segment message and store in segments array
        for (int i = 0; i < segments.length; i++){
            // End of message.  There is less than 7 bytes of data left of message
            if (message.length() - messageEndIDX < 7){
                messageEndIDX = message.length();

            // Move to next 7 bytes of message
            } else {
                messageEndIDX += 7;
            }

            // Construct SeqNum + checksum + term byte and prepend it to 7-byte message.  Then store in segments array
            String temp = String.valueOf(sequenceNum) + "0" + 
                          (i == segments.length - 1 ? "1" : "0") +
                          message.substring(messageStartIDX, messageEndIDX);
            segments[i] = temp;
            messageStartIDX = messageEndIDX;

            // Alternate sequence number
            sequenceNum ^= 1;
        }

        if (sender.connectSocket(args[3], args[4]) < 0){
            return;
        }

        
        String response = "";
        boolean packetsDelivered = false;
        
        // Loop until all packets have been delivered
        while (!packetsDelivered){
            for (int i = 0; i < segments.length; i++){
                System.out.println("Packet: " + (i + 1) + " out of " + segments.length);

                // Send request.  If negative value than a crictical error occured and close the socket
                if (sender.sendRequest(segments[i], SOURCE_IP, args[0], args[3], args[4], args[1], args[2]) < 0){
                    sender.closeSocket();
                    return;
                }

                boolean ackResponse = false;
                char seqNumChar;

                //  Loop until an ACK reponse is acheived
                while (!ackResponse){
                    seqNumChar = segments[i].charAt(0);
                    response = sender.receiveResponse(seqNumChar); 
                    
                    if (response != null){

                        // Response indicated a checksum or ACK issue
                        if (response == "ACK||CHECK"){

                            // Attempt to resend packet until successful resending
                            while(true){
                                System.out.println("\nFAILED TO SEND PACKET.  RESENDING PACKET...");
                                sender.sendRequest(segments[i], SOURCE_IP, args[0], args[3], args[4], args[1], args[2]);
                                response = sender.receiveResponse(seqNumChar).trim();
                                if (!response.equals("ACK||CHECK") && !response.equals("TIMEOUT") && response != null){
                                    System.out.println("PACKET RESENT PROPERLY");
                                    sender.printResponse(response);
                                    ackResponse = true;
                                    break;
                                }
                            }

                        // Response timout and should attempt to resend packet
                        } else if (response == "TIMEOUT"){
                            System.out.println("Error: Exeeced time to wait for response from Receiver.\n Sending packet again");
                                if (sender.sendRequest(segments[i], SOURCE_IP, args[0], args[3], args[4], args[1], args[2]) < 0){
                                    sender.closeSocket();
                                    return;
                                }
                        } else {
                            sender.printResponse(response);
                            ackResponse = true;
                        }

                    }
                }
            }
            packetsDelivered = true;
        }

        sender.closeSocket();
        
    }
}
