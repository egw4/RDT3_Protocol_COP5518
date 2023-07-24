import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Receiver {
    
    private final int    BUFFER_SIZE = 256;
    private DatagramSocket      _socket;
    private int                 _port;
    private boolean             _continueService;
    
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
    public static void printResponse(String response){
        System.out.println("FROM SERVER: " + response);
    }



    public int sendResponse(String response, String hostAddr, int port){
        DatagramPacket newDatagramPacket = this.createDatagramPacket(response, hostAddr, port);

        if (newDatagramPacket != null){
            try {
                this._socket.send(newDatagramPacket);
            } catch (IOException e) {
                System.err.println("Unable to send message to Receiver (server)");
                return -1;
            }
        }

        return 0;
    }



    /**
     * Creates a datagram using the provided destination hostname and port.  Loads in the request message to the buffer where it will wait to be sent
     * 
     * @param request   - Request being sent to Receiver
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
}
