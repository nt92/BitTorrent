package networking;

import configs.PeerInfoConfig;
import messages.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnection {
    private int peerID;
    private MessageDispatcher dispatcher;

    private Socket clientSocket;
    private DataOutputStream out;

    private ConcurrentLinkedQueue<byte[]> outboundQueue;

    public ClientConnection(int peerID, MessageDispatcher dispatcher) {
        this.peerID = peerID;
        this.dispatcher = dispatcher;
        this.outboundQueue = new ConcurrentLinkedQueue<>();
    }

    public void openConnectionWithConfig(PeerInfoConfig peerInfoConfig) throws Exception {
        clientSocket = new Socket(peerInfoConfig.getHostName(), peerInfoConfig.getListeningPort());
        try {
            out = new DataOutputStream(clientSocket.getOutputStream());
            out.flush();
        } catch(Exception e) {
            e.printStackTrace();
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
        clientSocket.close();
    }
}
