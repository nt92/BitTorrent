package networking;

import configs.PeerInfoConfig;
import messages.*;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnection {
    private int peerID;
    private ConcurrentLinkedQueue<byte[]> outboundQueue;

    private Socket socket;
    private DataOutputStream out;

    public ClientConnection(int peerID) {
        this.peerID = peerID;
        this.outboundQueue = new ConcurrentLinkedQueue<>();
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
        outboundQueue.add(handshakeMessage.toBytes());

        while (true) {
            // Take care of messages in outboundQueue
            while (!outboundQueue.isEmpty()) {
                byte[] outBytes = outboundQueue.poll();
                try {
                    out.write(outBytes);
                    out.flush();
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendActualMessage(ActualMessage message) throws Exception {
        outboundQueue.add(message.toBytes());
    }

    public void closeConnection() throws Exception {
        out.close();
        socket.close();
    }
}
