import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Receiver {
    
    private DatagramSocket _socket;
    private int            _port;
    private boolean        _continueService;
    
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
}
