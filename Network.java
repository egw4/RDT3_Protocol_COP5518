import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;

public class Network {
    

    private DatagramSocket  _socket;
    private int             _port;
    private boolean         _continueService;

    private static final int BUFFER_SIZE = 54;

    private Utility utility = new Utility();

    public Network(int port) {
        this._port = port;
    }


    public int createSocket() {
        try {
            this._socket = new DatagramSocket(this._port);
        } catch (SocketException e){
            System.err.println("Unable to create and bind socket");
            return -1;
        }

        return 0;
    }

    public int closeSocket() {
        this._socket.close();
        return 0;
    }


    /**
     * Receives requests by calling upon underlying UDP protocol
     * @return - datagram containing the client request
     */
    public DatagramPacket receiveRequest() {
        byte[] buffer = new byte[BUFFER_SIZE];
        DatagramPacket packetToReceive = new DatagramPacket(buffer, BUFFER_SIZE);

        try {
            // Call to underlying UDP receive method
            this._socket.receive(packetToReceive);
            System.out.println(packetToReceive);
        } catch (IOException e){
            System.err.println("Unable to receive message from client");
            return null;
        }

        return packetToReceive;
    }

    /**
     * Sends response to forwardAddress of datagramPacket
     * @param datagramPacket - Packet to send to forward address
     * @return
     */
    public int sendResponse(DatagramPacket datagramPacket){
        if (datagramPacket != null){
            String forwardAddressForPacket = datagramPacket.getAddress().getHostAddress();

            try {
                System.out.println("Packet being sent to: " + forwardAddressForPacket + " Port: " + datagramPacket.getPort());

                // Call underlying UDP method
                this._socket.send(datagramPacket);
            } catch (IOException e){
                System.err.println("Error: Unable to forward packet to " +
                                    forwardAddressForPacket + " Port: " + datagramPacket.getPort());
                return -1;
            }
        }

        return 0;
    }
    


    /**
     * Method that handles most of functionality of this class
     * @param lostPercent       - Percent likelihood of packet being lost 
     * @param delayedPercent    - Percent likelihood of packet being delayed
     * @param errorPercent      - Percent likelihood of packet being error
     */
    public void run(int lostPercent, int delayedPercent, int errorPercent) {
        this._continueService = true;

        // Variables for summary stats while sending network traffic
        int packetsLost = 0;
        int packetsDelayed = 0;
        int packetsCorrupt = 0;
        int packetCountFromSender = 0;
        int packetCountFromReceiver = 0;
        int packetsSent = 0;


        System.out.println("Beginning Network...");

        // Continue to listen for network traffic
        while (this._continueService){

            System.out.println("Listening on port " + this._port);

            if (this._socket.isConnected()) {
                this._socket.disconnect();
            }

            // Receive request
            DatagramPacket packet = this.receiveRequest();
            
            String request = new String(packet.getData());
            
            HashMap<String, String> networkHeaderPortions = utility.parseNetworkHeader(request);

            // Wrap message in StringBuffer to manipulate checksum byte later if error occurs for packet
            StringBuffer message = new StringBuffer(networkHeaderPortions.get("message"));

            System.out.println("Packet received from: " + packet.getAddress().getHostAddress() + " Port: " + packet.getPort());
            System.out.println("Request: " + request);

            // Increment packet counts from receiver if the message conatins a ACK portion
            if(networkHeaderPortions.get("message").contains("ACK")){
                packetCountFromReceiver++;
            } else {
                packetCountFromSender++;
            }

            // Attempt to connect Network to alternate program (i.e. switch connection from sender to receiver and vice versa)
            try {
                this._socket.connect(InetAddress.getByName(networkHeaderPortions.get("destIP")), Integer.parseInt(networkHeaderPortions.get("destPort")));
                

            } catch (UnknownHostException e){
                System.err.println("Error: Unable to connect to host " +
                                    networkHeaderPortions.get("destIP") + " on port " + networkHeaderPortions.get("destPort"));
                return;
            }

            // Attempt to switch destination IP address and port of packet after having just swapped socket connection above
            try {
                packet.setAddress(InetAddress.getByName(networkHeaderPortions.get("destIP")));
                packet.setPort(Integer.parseInt(networkHeaderPortions.get("destPort")));

            }  catch (UnknownHostException e){
                System.err.println("Error: Unable to connect to host " +
                                    networkHeaderPortions.get("destIP") + " on port " + networkHeaderPortions.get("destPort"));
                return;
            }

            
            // Simulate delay of packet
            if (delayedPercent > 0) {
                if (Math.random() * 100 < delayedPercent) {
                    System.out.println("Packet delayed");
                    packetsDelayed++;
                    
                    // Thread to execute sendResponse after delaying 4 seconds
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(4000);
                                // Forward the packet to the destination host and port
                                sendResponse(packet);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                    }).start();

                    continue;
                }
            }

            // simulate lost packets
            if (lostPercent > 0) {
                if (Math.random() * 100 < lostPercent) {
                    System.out.println("Packet lost");
                    packetsLost++;
                }
            }

            // simulate corrupt packet and alter checksum byte of message (Sender will resend packet)
            if (errorPercent > 0) {
                if (Math.random() * 100 < errorPercent) {
                    System.out.println("Packet corrupted");
                    packetsCorrupt++;

                    // Flip checksum byte
                    message.setCharAt(0, '1');
                    
                    String dataString = request.substring(0, request.length() - message.length()) + message;
                    System.out.println(dataString);
                    
                    byte data[] = dataString.getBytes();
                    packet.setData(data);
                    System.out.println(packet.getData().toString());
                }
            }
            
            // No errors occured and packet is sent as expected
            this.sendResponse(packet);
            packetsSent++;
            System.out.println("");
        
            // print stats every 5 frames
            if ((packetCountFromSender + packetCountFromReceiver) % 5 == 0) {
                System.out.println("");
                System.out.println("Total Packets Received From Sender: " + packetCountFromSender);
                System.out.println("Total Packets Received From Receiver: " + packetCountFromReceiver);
                System.out.println("Total Packets Received: " + (packetCountFromSender + packetCountFromReceiver));
                System.out.println("Total Packets Sent: " + packetsSent);
                System.out.println("Lost Packets: " + packetsLost);
                System.out.println("Delayed Packets: " + packetsDelayed);
                System.out.println("Corrupt Packets: " + packetsCorrupt);
                System.out.println("");
            }
        }
    }

    // Just checks that correct command line arguments were passed.  Run() above does most of the classes functionality
    public static void main(String[] args) {
        Network network;
        int lostPercent, delayedPercent, errorPercent;

        if (args.length != 4){
            System.err.println("Usuage: java Network <network_port> <lostPercent> <delayedPercent> <errorPercent>");
            return;
        }

        network = new Network(Integer.parseInt(args[0]));
        lostPercent = Integer.parseInt(args[1]);
        delayedPercent = Integer.parseInt(args[2]);
        errorPercent = Integer.parseInt(args[3]);
        if (network.createSocket() < 0){
            return;
        }
        
        network.run(lostPercent, delayedPercent, errorPercent);
        network.closeSocket();
    }
}
