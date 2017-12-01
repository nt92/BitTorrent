import java.io.DataInputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SocketServer extends Thread {
    private Socket socket;
    private int peerID;
    private Logger logger;
    private MessageDispatcher dispatcher;
    private ConcurrentLinkedQueue<HandshakeMessage> handshakeQueue;
    private ConcurrentLinkedQueue<ActualMessage> messageQueue;
    private int otherPeerID = -1;

    public SocketServer(Socket socket, int peerID, Logger logger, MessageDispatcher dispatcher) {
        this.socket = socket;
        this.peerID = peerID;
        this.logger = logger;
        this.dispatcher = dispatcher;
        this.handshakeQueue = new ConcurrentLinkedQueue<>();
        this.messageQueue = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void run() {
        DataInputStream in = null;
        try {
            in = new DataInputStream(socket.getInputStream());
            while (true) {
                byte[] bytes = new byte[]{  };
                try {
                    int length = in.readInt();
                    bytes = new byte[length];
                    in.readFully(bytes);
                } catch (Exception e1) {
                    e1.printStackTrace();
                } finally {
                    try {
                        HandshakeMessage handshakeMessage = new HandshakeMessage(bytes);
                        otherPeerID = handshakeMessage.getPeerID();
                        handshakeQueue.add(handshakeMessage);
                        logger.logConnectionReceived(peerID, otherPeerID);
                    } catch (Exception e1) {
                        try {
                            if (bytes.length != 0) {
                                ActualMessage message = new ActualMessage(bytes);
                                messageQueue.add(message);
                            }
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                }

                while (!handshakeQueue.isEmpty()) {
                    HandshakeMessage message = handshakeQueue.poll();
                    dispatcher.dispatchMessage(message);
                }

                while (!messageQueue.isEmpty()) {
                    ActualMessage message = messageQueue.poll();
                    dispatcher.dispatchMessage(message, otherPeerID);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}