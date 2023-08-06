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

    private static final int SEGMENT_SIZE = 10;
    private static final int BUFFER_SIZE = 54;

    private static final String SOURCE_IP = "127.0.0.1";
    private static final String DEST_IP = "127.0.0.1";

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

    public int sendResponse(DatagramPacket datagramPacket){
        if (datagramPacket != null){
            String forwardAddressForPacket = datagramPacket.getAddress().getHostAddress();

            try {
                System.out.println("Packet being sent to: " + forwardAddressForPacket + " Port: " + datagramPacket.getPort());

                this._socket.send(datagramPacket);
            } catch (IOException e){
                System.err.println("Error: Unable to forward packet to " +
                                    forwardAddressForPacket + " Port: " + datagramPacket.getPort());
                return -1;
            }
        }

        return 0;
    }
    


    public void run(int percLost, int percDelayed, int percCorrupt) {
        this._continueService = true;

        int packetsLost = 0;
        int packetsDelayed = 0;
        int packetsCorrupt = 0;
        int packetCountFromSender = 0;
        int packetCountFromReceiver = 0;
        int packetsSent = 0;


        System.out.println("Beginning Network...");
        int count = 0;
        while (this._continueService){

            System.out.println("Listening on port " + this._port);

            if (this._socket.isConnected()) {
                this._socket.disconnect();
            }

            DatagramPacket packet = this.receiveRequest();

            String request = new String(packet.getData()).trim();
            
            HashMap<String, String> networkHeaderPortions = utility.parseNetworkHeader(request);
            StringBuffer message = new StringBuffer(networkHeaderPortions.get("message"));

            System.out.println("Packet received from: " + packet.getAddress().getHostAddress() + " Port: " + packet.getPort());
            System.out.println("Request: " + request);

            try {
                this._socket.connect(InetAddress.getByName(networkHeaderPortions.get("destIP")), Integer.parseInt(networkHeaderPortions.get("destPort")));
                

            } catch (UnknownHostException e){
                System.err.println("Error: Unable to connect to host " +
                                    networkHeaderPortions.get("destIP") + " on port " + networkHeaderPortions.get("destPort"));
                return;
            }

            try {
                packet.setAddress(InetAddress.getByName(networkHeaderPortions.get("destIP")));
                packet.setPort(Integer.parseInt(networkHeaderPortions.get("destPort")));

            }  catch (UnknownHostException e){
                System.err.println("Error: Unable to connect to host " +
                                    networkHeaderPortions.get("destIP") + " on port " + networkHeaderPortions.get("destPort"));
                return;
            }

            boolean stop = false;

            while (!stop){
                if (percDelayed > 0) {
                    if (Math.random() * 100 < percDelayed) {
                        System.out.println("Packet delayed");
                        packetsDelayed++;

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(3000);
                                    // Forward the packet to the destination host and port
                                    sendResponse(packet);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                            }
                        }).start();

                        stop = true;
                        continue;
                    }
                }

                // Use the lostPercent to determine if the packet should be lost
                if (percLost > 0) {
                    if (Math.random() * 100 < percLost) {
                        System.out.println("Packet lost");
                        packetsLost++;
                        stop = true;
                    }
                }

                // Use the errorPercent to determine if the packet should be corrupted, change checksum byte in message
                if (percCorrupt > 0) {
                    if (Math.random() * 100 < percCorrupt) {
                        System.out.println("Packet corrupted");
                        packetsCorrupt++;

                        // Flip checksum byte
                        message.setCharAt(0, '1');

                    
                        String dataString = message.toString();
                        byte data[] = dataString.getBytes();
                        packet.setData(data);
                    }
                }
            
                this.sendResponse(packet);
                packetsSent++;
                stop = true;
                System.out.println("");
            
                // Every 5 packets print the network statistics
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
            if (count > 1){
                this._continueService = false;
            }
            // count++;
        }

        
        
        
    }

    public static void main(String[] args) {
        Network network;
        int percLost, percDelayed, percCorrupt;

        if (args.length != 4){
            System.err.println("Usuage: java Network <network_port> <%_lost> <%_delayed> <%_corrupt>");
            return;
        }

        network = new Network(Integer.parseInt(args[0]));
        percLost = Integer.parseInt(args[1]);
        percDelayed = Integer.parseInt(args[2]);
        percCorrupt = Integer.parseInt(args[3]);
        if (network.createSocket() < 0){
            return;
        }
        
        network.run(percLost, percDelayed, percCorrupt);
        network.closeSocket();
    }
}
