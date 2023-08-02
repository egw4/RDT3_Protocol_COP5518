import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Network {
    
    private final int       BUFFER_SIZE = 256;
    private DatagramSocket  _socket;
    private int             _port;
    private boolean         _continueService;

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

    public int sendDatagramPacket(String packet, String hostAddr, int port){
        DatagramPacket newDatagramPacket = this.createDatagramPacket(packet, hostAddr, port);

        if (newDatagramPacket != null){
            try {
                this._socket.send(newDatagramPacket);
            } catch (IOException e) {
                System.err.println("Unable to send message to Receiver (server)");
                return -1;
            }
            return 0;
        }

        System.err.println("Unable to create message");
        return -1; 
    }

    public DatagramPacket receiveDatagramPacket(){
        byte[] buffer = new byte[this.BUFFER_SIZE];

        DatagramPacket newDatagramPacket = new DatagramPacket(buffer, BUFFER_SIZE);

        try {
            this._socket.receive(newDatagramPacket);
        } catch (IOException e) {
            System.err.println("NETWORK: Unable to receive message");
            return null;
        }

        return newDatagramPacket;
    }

    /**
     * Creates a datagram using the provided destination hostname and port.  Loads in the request message to the buffer where it will wait to be sent
     * 
     * @param request   - Request datagramPacket is being created for
     * @param hostname  - Hostname that is used to search for IP address to send datagram
     * @param port      - Port number the host is listening on for the datagram
     * 
     * @return          - A "datagram" or "null" if an error occured during the cration process
     */
    private DatagramPacket createDatagramPacket(String request, String hostname, int port){
        byte buffer[] = new byte[this.BUFFER_SIZE];

        // Initiallize buffer with empty values for all byte elements
        for (int i = 0; i < this.BUFFER_SIZE; i++){
            buffer[i] = '\0';
        }

        /*
         * Convert request String to byte array
         * Copy contents of converted array to buffer
         * 
         * Math.min will determine smaller array (data or buffer) to handle 
         *   indexing byte arrays (i.e. avoid IndexOutOfBoundsException)
         */
        byte data[] = request.getBytes();
        System.arraycopy(data, 0, buffer, 0, Math.min(data.length, buffer.length));


        /* Search for IP address of given hostname.  If none exist then no     
         *   DatagramPacket can be created
         */
        InetAddress hostAddr;
        try {
            hostAddr = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            System.err.println("Invalid host address");
            return null;
        }

        return new DatagramPacket(buffer, this.BUFFER_SIZE, hostAddr, port);
    }


    public void run() {
        this._continueService = true;

        Scanner scan = new Scanner(System.in);
        String request = "";

        while (!request.equals("<echo>q</echo>")){

            System.out.println("Enter Message: ");
            String response = scan.nextLine();
            this.sendDatagramPacket(response, "localhost", 60000);

            DatagramPacket newDatagramPacket = this.receiveDatagramPacket();

            System.out.println("Host IP: " + newDatagramPacket.getAddress());
            System.out.println("Host Name: " + newDatagramPacket.getAddress().getHostName());

            request = new String(newDatagramPacket.getData()).trim();

            System.out.println("Sender IP: " + newDatagramPacket.getAddress().getHostAddress());
            System.out.println("Sender Request: " + request);
            System.out.println("");
        }
        scan.close();
        // while (this._continueService){
        //     String response = "<echo>Hello</echo>";

          
        // }

        
    }

    public static void main(String[] args) {
        Network network;

        network = new Network(Integer.parseInt(args[0]));
        network.createSocket();
        network.run();
        network.closeSocket();
    }
}
