import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;

public class Sender {
    private DatagramSocket      _socket;
    private int                 _port;
    private boolean             _continueService;

    private String receiverIP;
    private String receiverPort;
    private String destIP;
    private String destPort;
    private static final int SEGMENT_SIZE = 10;
    private static final int BUFFER_SIZE = 54;

    private static final String SOURCE_IP = "127.0.0.1";
    private static final String DEST_IP = "127.0.0.1";

    // Utility class to create network header for RDT packet
    private Utility utility = new Utility();

    public Sender(int port, String receiverIP, String receiverPort, String destIP, String destPort){
        this._port = port;
        this.receiverIP = receiverIP;
        this.receiverPort = receiverPort;
        this.destIP = destIP;
        this.destPort = destPort;
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
     * Closes open datagram socket
     * 
     * @return - 0, if no error; otherwise, a negative number indicating the error
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
        System.out.println("FROM RECEIVER: " + response);
    }
    
    public int sendRequest(String segment, String senderIP, String senderPort, String networkIP, String networkPort, String receiverIP, String receiverPort){
        String networkHeader = this.utility.createNetworkHeader(senderIP, senderPort, receiverIP, receiverPort, segment);

        DatagramPacket packet = this.utility.createDatagramPacket(networkHeader, networkIP, networkPort, BUFFER_SIZE);

        if(packet != null) {
            try {
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

    public String receiveResponse(char sequenceNum){
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, BUFFER_SIZE);
        String message;

        try {
            // Call to underlying UDP receive method
            this._socket.receive(receivedPacket);

            HashMap<String, String> networkPortions = this.utility.parseNetworkHeader(new String(receivedPacket.getData()).trim());

            message = networkPortions.get("message");

            char receivedAckByte = message.charAt(6);
            char checksumByte = message.charAt(7);
            System.out.println("Message: " + message + " |RACK: " + receivedAckByte + " |SEQ: " + sequenceNum + " |CSUM: " + checksumByte);
            if (receivedAckByte != sequenceNum || checksumByte != '0'){
                System.err.println("Error: Incorrect ACK byte received or corruption of packet detected by non-zero checksum");
                return "ACK||CHECK";
            }

        } catch (SocketTimeoutException e){
            System.err.println("Socket Timeout Occured");
            return "TIMEOUT";
        } catch (IOException e) {
            System.err.println("Unable to receive message from client");
            return null;
        }

        //return buffer.toString().trim();
        return message;
    }

    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        Sender sender;
        String message;

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
            
        } catch (NullPointerException e) {
            System.err.println("Usuage: java Sender <sender_port> <receiver_IP> <receiver_port> <network_IP> <network_Port>");
            return;
        }

        if (sender.createSocket() < 0){
            System.err.println("Error: Failed to create socket at port " + args[0]);
            return;
        }

        System.out.println("Beginning Sender." + " Port: " + args[0]);
        System.out.print("Enter message: ");
        message = scan.nextLine();

        // Create segments.  Messages will only be 7 bytes with 3 bytes for SEQ#, checksum, term bit followed by 7 byte message
        String[] segments = new String[(int) Math.ceil(message.length() / 7.0)];

        int sequenceNum = 0;
        int messageStartIDX = 0;
        int messageEndIDX = 0;

        for (int i = 0; i < segments.length; i++){
            // End of message.  There is less than 7 bytes of data left of message
            if (message.length() - messageEndIDX < 7){
                messageEndIDX = message.length();

            // Move to next 7 bytes of message
            } else {
                messageEndIDX += 7;
            }

            String temp = String.valueOf(sequenceNum) + "0" + 
                          (i == segments.length - 1 ? "1" : "0") +
                          message.substring(messageStartIDX, messageEndIDX);
            segments[i] = temp;
            messageStartIDX = messageEndIDX;
            sequenceNum ^= 1;
        }

        if (sender.connectSocket(args[3], args[4]) < 0){
            return;
        }

        
        String response = "";
        boolean packetsDelivered = false;
        

        while (!packetsDelivered){
            for (int i = 0; i < segments.length; i++){
                System.out.println("Packet: " + (i + 1) + " out of " + segments.length);

                if (sender.sendRequest(segments[i], SOURCE_IP, args[0], args[3], args[4], args[1], args[2]) < 0){
                    sender.closeSocket();
                    return;
                }

                boolean ackResponse = false;
                char seqNumChar;
                while (!ackResponse){
                    seqNumChar = segments[i].charAt(0);
                    response = sender.receiveResponse(seqNumChar); 
                    
                    if (response != null){
                        if (response == "ACK||CHECK"){
                            
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
