import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

public class Utility {
    public Utility() {

    }

    /**
     * Creates the network header for RDT packet.  Used by Receiver and Sender classes.
     * @param srcIP     - Source IP address
     * @param srcPort   - Source port number
     * @param destIP    - Desitination IP address
     * @param destPort  - Desitination port number
     * @param message   - Message of RDT packet
     * @return          - Network header as a single String
     */
    public String createNetworkHeader(String srcIP, String srcPort, String destIP, String destPort, String message){
        String networkHeader = srcIP + "-" + 
                               srcPort + "-" +
                               destIP + "-" +
                               destPort + "-" +
                               message;

        return networkHeader;
    };

    /**
     * Breaks the network into individual portions (i.e. srcIP, srcPort, destIP, etc.)
     * @param networkHeader - String representing entire network header (including message)
     * @return              - HashMap of network header portions as key-value pairs
     */
    public HashMap<String, String> parseNetworkHeader(String networkHeader){
        String[] portions = networkHeader.split("-");

        HashMap<String, String> networkPortions = new HashMap<String, String>();

        networkPortions.put("srcIP", portions[0]);
        networkPortions.put("srcPort", portions[1]);
        networkPortions.put("destIP", portions[2]);
        networkPortions.put("destPort", portions[3]);
        networkPortions.put("message", portions[4]);

        return networkPortions;
    }

    
    /**
     * Debugging function to quickly print the portions of the network header while testing overall program
     * @param portions - HashMap containing all portions of the network header being passed across the simulated network
     */
    public void printNetworkHeaderPortions(HashMap<String, String> portions){
        System.out.println("srcIP: " + portions.get("srcIP"));
        System.out.println("srcPort: " + portions.get("srcPort"));
        System.out.println("destIP: " + portions.get("destIP"));
        System.out.println("destPort: " + portions.get("destPort"));
        System.out.println("message: " + portions.get("message"));
    }

    /**
     * Constructs the datagram packet (called by Sender and Receiver since they create packets)
     * @param networkHeader - Network header for packet
     * @param hostname      - IP address the packet is being sent to
     * @param port          - Port the packet is being sent to
     * @param bufferSize    - Buffer size of the receiving programs receiveRequest method
     * @return              - The created datagram packet
     */
    public DatagramPacket createDatagramPacket(String networkHeader, String hostname, String port, int bufferSize){
        byte buffer[] = new byte[bufferSize];

        // Initialize buffer with empty message
        for (int i = 0; i < bufferSize; i++) {
            buffer[i] = '\0';
        }

        // copy message into buffer
        byte message[] = networkHeader.getBytes();
        System.arraycopy(message, 0, buffer, 0, Math.min(message.length, buffer.length));

        
        // Try to create the datagram packet
        try {
            return new DatagramPacket(buffer, 
                                      bufferSize, 
                                      InetAddress.getByName(hostname),
                                      Integer.parseInt(port));
        } catch (UnknownHostException ex) {
            System.err.println("Error: Not a valid host address provided");
            return null;
        }

        
    }
}
