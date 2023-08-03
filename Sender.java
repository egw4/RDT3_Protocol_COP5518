import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class Sender {

    private static final int BUFFER_SIZE = 54;
    private static final int HEADER_SIZE = 44;
    private static final int SEGMENT_SIZE = 10;
    private DatagramSocket socket;
    private InetAddress address;
    private int port;
    private byte seqNo = 0;
    private boolean continueService; 
    private final int TIMEOUT = 5000; // 5 seconds
    private int sequenceNumber = 0; // Initial sequence number
    private final int ackNumber = 0; // Initial ack number
    private Scanner scanner;
    private byte[] data;
    private byte[] header;
    private String srcIP;  // Source IP address
    private String destIP; // Destination IP address
    private String srcPort;  // Source port number
    private String destPort; // Destination port number

  
    /**
     * Constructs a Sender object.
     */
    public Sender() {
        this.port = port;
    }

    
    /**
     * Creates a datagram socket and binds it to a free port.
     * @return - 0 or a negative number describing an error code if the connection could not be established
     */
    public int createSocket() {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException ex) {
            System.err.println("unable to create and bind socket");
            return -1;
        }
        return 0;
    }

        public Sender(String srcIP, String srcPort, String destIP, String destPort) throws SocketException, UnknownHostException {
        this.srcIP = srcIP;
        this.destIP = destIP;
        this.srcPort = srcPort;
        this.destPort = destPort;
        }

    private byte[] createPacket(String message) {
        // Create the network header
        byte[] networkHeader = new byte[44];
        System.arraycopy(srcIP.getBytes(), 0, networkHeader, 0, 16);
        System.arraycopy(srcPort.getBytes(), 0, networkHeader, 16, 6);
        System.arraycopy(destIP.getBytes(), 0, networkHeader, 22, 16);
        System.arraycopy(destPort.getBytes(), 0, networkHeader, 38, 6);

        // Create the payload
        byte[] payload = message.getBytes();

        // Combine the network header and payload to create the packet
        ByteBuffer packetBuffer = ByteBuffer.allocate(networkHeader.length + payload.length);
        packetBuffer.put(networkHeader);
        packetBuffer.put(payload);

        return packetBuffer.array();
    }

        public static void main(String[] args) {
        Sender sender = new Sender();
        sender.run();
    }

    public void run() {
        try {
            // Initialize scanner and socket
            scanner = new Scanner(System.in);
            socket = new DatagramSocket();

            while (true) {
                // Take input from the user
                System.out.println("Enter the message to send: ");
                String message = scanner.nextLine();

                // If message is empty, stop the sender
                if (message.isEmpty()) break;

                // Split message into segments and send each segment one by one
                for (int i = 0; i < message.length(); i += (SEGMENT_SIZE - HEADER_SIZE)) {
                    String segment = message.substring(i, Math.min(i + SEGMENT_SIZE - HEADER_SIZE, message.length()));
                    sendSegment(segment);
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }

    private void sendSegment(String segment) {
        try {
            // Create header (assuming all fields are sequence number)
            String header = String.format("%1$" + HEADER_SIZE + "s", sequenceNumber).replace(' ', '0');

            // Create packet (header + segment)
            String packet = header + segment;
            byte[] buffer = packet.getBytes();

            // Send packet to the network
            DatagramPacket datagramPacket = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(datagramPacket);
            System.out.println("Sent packet with sequence number: " + sequenceNumber);

            // Start timer
            socket.setSoTimeout(TIMEOUT);

            // Wait for acknowledgment
            while (true) {
                try {
                    DatagramPacket receivedPacket = new DatagramPacket(new byte[SEGMENT_SIZE], SEGMENT_SIZE);
                    socket.receive(receivedPacket);

                    String receivedData = new String(receivedPacket.getData()).trim();
                    int ackNumber = Integer.parseInt(receivedData.substring(0, HEADER_SIZE).trim());

                    // If correct acknowledgment is received, break the loop
                    if (ackNumber == sequenceNumber) {
                        System.out.println("Received acknowledgment for sequence number: " + sequenceNumber);
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    // If timeout occurs, resend the packet
                    socket.send(datagramPacket);
                    System.out.println("Resent packet with sequence number: " + sequenceNumber);
                }
            }

            // Toggle the sequence number (0 becomes 1, 1 becomes 0)
            sequenceNumber = 1 - sequenceNumber;
        } catch (IOException e) {
            e.printStackTrace();
        }
        scanner.close();
    }
}







/**
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Sender {
    private static final int SRC_IP_SIZE = 16;
    private static final int SRC_PORT_SIZE = 6;
    private static final int DEST_IP_SIZE = 16;
    private static final int DEST_PORT_SIZE = 6;
    private static final int SEGMENT_SIZE = 10;
    private static final int BUFFER_SIZE = 54; // BUFFER SIZE = SRC_IP_SIZE + SRC_PORT_SIZE + DEST_IP_SIZE + DEST_PORT_SIZE + SEGMENT_SIZE

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter message: ");
        String message = scanner.nextLine();

        /** Assigning values for source and destination IP and port. IP addresses are 16 bytes and port numbers are 6 bytes. 
        *   IP addresses and port numbers use generic numbers but a scanner can also be used to get user input. 
        */
    /**
        String sourceIP = "127.0.0.1";
        String destIP = "127.0.0.1";
        int sourcePort = 8000;
        int destPort = 8001;

        byte[] packet = new byte[BUFFER_SIZE];
        byte[] messageBytes = message.getBytes();

        // Create a segment
        byte[] segment = new byte[SEGMENT_SIZE];

        // Create source and destination IP and port.
        byte[] srcIPBytes = sourceIP.getBytes();
        byte[] destIPBytes = destIP.getBytes();
        byte[] srcPortBytes = String.valueOf(sourcePort).getBytes();
        byte[] destPortBytes = String.valueOf(destPort).getBytes();

        // Put values into packet.
        System.arraycopy(srcIPBytes, 0, packet, 0, srcIPBytes.length);
        System.arraycopy(srcPortBytes, 0, packet, SRC_IP_SIZE, srcPortBytes.length);
        System.arraycopy(destIPBytes, 0, packet, SRC_IP_SIZE + SRC_PORT_SIZE, destIPBytes.length);
        System.arraycopy(destPortBytes, 0, packet, SRC_IP_SIZE + SRC_PORT_SIZE + DEST_IP_SIZE, destPortBytes.length);

        // Send each segment of data.
        for (int i = 0; i < messageBytes.length; i += SEGMENT_SIZE) {
            int len = Math.min(SEGMENT_SIZE, messageBytes.length - i);
            System.arraycopy(messageBytes, i, segment, 0, len);

            // Put the segment into the packet
            System.arraycopy(segment, 0, packet, SRC_IP_SIZE + SRC_PORT_SIZE + DEST_IP_SIZE + DEST_PORT_SIZE, len);

            try {
                DatagramSocket socket = new DatagramSocket();
                DatagramPacket packetToSend = new DatagramPacket(packet, packet.length, InetAddress.getByName(destIP), destPort);
                socket.send(packetToSend);

                DatagramPacket packetToReceive = new DatagramPacket(new byte[SEGMENT_SIZE], SEGMENT_SIZE);
                socket.receive(packetToReceive);

                System.out.println("Received ACK: " + new String(packetToReceive.getData()));

                socket.close();
                
            } catch (SocketException se) {
                System.out.println("Error creating DatagramSocket: " + se.getMessage());
            } catch (UnknownHostException uhe) {
                System.out.println("Unknown host: " + destIP);
            } catch (IOException ioe) {
                System.out.println("IOException: " + ioe.getMessage());
            }
        }

        scanner.close();
    }
   
}
*/
