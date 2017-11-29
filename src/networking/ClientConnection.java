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
    private DataInputStream in;

    private int otherPeerID;
    private ConcurrentLinkedQueue<ActualMessage> inboundQueue;
    private ConcurrentLinkedQueue<byte[]> outboundQueue;

    private Thread listenerThread;

    public ClientConnection(int peerID, MessageDispatcher dispatcher) {
        this.peerID = peerID;
        this.dispatcher = dispatcher;
        this.inboundQueue = new ConcurrentLinkedQueue<>();
        this.outboundQueue = new ConcurrentLinkedQueue<>();
    }

    public void openConnectionWithConfig(PeerInfoConfig peerInfoConfig) throws Exception {
        otherPeerID = peerInfoConfig.getPeerID();
        clientSocket = new Socket(peerInfoConfig.getHostName(), peerInfoConfig.getListeningPort());
        try {
            out = new DataOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new DataInputStream(clientSocket.getInputStream());
        } catch(Exception e) {
            e.printStackTrace();
        }

        HandshakeMessage handshakeMessage = new HandshakeMessage(peerID);
        outboundQueue.add(handshakeMessage.toBytes());

        while (true) {
            // Spawn listenerThread if it's null or dead
            if (listenerThread == null || !listenerThread.isAlive()){
                listenerThread = new Thread(() -> {
                    try {
                        int length = in.readInt();
                        byte[] bytes = new byte[length];
                        in.readFully(bytes);
                        ActualMessage message = new ActualMessage(bytes);
                        inboundQueue.add(message);
                    } catch (Exception e2) {
                        e2.printStackTrace();
                    }
                });
                listenerThread.start();
            }

            // Take care of messages in inboundQueue
            while(!inboundQueue.isEmpty()){
                ActualMessage message = inboundQueue.poll();
                dispatcher.dispatchMessage(message, otherPeerID);
            }

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
