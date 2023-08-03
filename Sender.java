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
         IP addresses and port numbers use generic numbers, but a scanner can also be used to get user input. */
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
