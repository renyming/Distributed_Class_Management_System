import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UDPListener extends Thread {

    private int port;
    private CenterServer server;

    public UDPListener(CenterServer server, int port) {
        this.port = port;
        this.server=server;
    }

    @Override
    public void run() {
        DatagramSocket socket = null;
        DatagramPacket packet = null;
        byte[] buffer = null;

        try {
            socket = new DatagramSocket(port);
            buffer = new byte[256];
            packet = new DatagramPacket(buffer, buffer.length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            while (true) {
                socket.receive(packet);
                String request = new String(packet.getData(), 0, packet.getLength());
                if (request.equals("getSize")) {
                    int size = server.getSize();
                    InetAddress ip = packet.getAddress();
                    int clientPort = packet.getPort();
                    byte[] replyBytes = Integer.toString(size).getBytes();
                    DatagramPacket reply = new DatagramPacket(replyBytes, replyBytes.length, ip, clientPort);
                    socket.send(reply);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null)	socket.close();
        }

    }

}