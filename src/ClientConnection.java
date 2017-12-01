import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnection {
    private int peerID;
    private ConcurrentLinkedQueue<HandshakeMessage> handshakeQueue;
    private ConcurrentLinkedQueue<ActualMessage> messageQueue;

    private Socket socket;
    private DataOutputStream out;

    public ClientConnection(int peerID) {
        this.peerID = peerID;
        this.handshakeQueue = new ConcurrentLinkedQueue<>();
        this.messageQueue = new ConcurrentLinkedQueue<>();
    }

    public void openConnectionWithConfig(PeerInfoConfig peerInfoConfig) throws Exception {
        socket = new Socket(peerInfoConfig.getHostName(), peerInfoConfig.getListeningPort());
        try {
            out = new DataOutputStream(socket.getOutputStream());
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }

        HandshakeMessage handshakeMessage = new HandshakeMessage(peerID);
        handshakeQueue.add(handshakeMessage);

        while (true) {
            while (!handshakeQueue.isEmpty()) {
                byte[] outBytes = handshakeQueue.poll().toBytes();
                try {
                    out.write(outBytes);
                    out.flush();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

            while (!messageQueue.isEmpty()) {
                ActualMessage message = messageQueue.poll();
                byte[] outBytes = message.toBytes();
                try {
                    out.write(outBytes);
                    out.flush();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void sendHandshakeMessage(HandshakeMessage message) throws Exception {
        handshakeQueue.add(message);
    }

    public void sendActualMessage(ActualMessage message) throws Exception {
        messageQueue.add(message);
    }

    public void closeConnection() throws Exception {
        out.close();
        socket.close();
    }
}
