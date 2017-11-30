package networking;

import messages.ActualMessage;
import messages.HandshakeMessage;
import messages.MessageDispatcher;

import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServerConnection {
    private MessageDispatcher dispatcher;
    private List<RequestHandlerThread> requestHandlerThreads;
    private ServerSocket serverSocket;

    public ServerConnection(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        this.requestHandlerThreads = new ArrayList<>();
    }

    public void openPort(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        while (true) {
            ServerConnection.RequestHandlerThread requestHandlerThread = new ServerConnection.RequestHandlerThread(serverSocket.accept(), dispatcher);
            requestHandlerThreads.add(requestHandlerThread);
            requestHandlerThread.start();
        }
    }

    public void close() throws Exception {
        serverSocket.close();
    }

    // A thread that handles responding to a single message
    private static class RequestHandlerThread extends Thread {
        private Socket socket;
        private DataInputStream in;
        private MessageDispatcher dispatcher;
        private Thread listenerThread;
        private int otherPeerID = -1;

        private ConcurrentLinkedQueue<HandshakeMessage> handshakeQueue;
        private ConcurrentLinkedQueue<ActualMessage> messageQueue;

        public RequestHandlerThread(Socket socket, MessageDispatcher dispatcher) {
            this.socket = socket;
            this.dispatcher = dispatcher;
            this.handshakeQueue = new ConcurrentLinkedQueue<>();
            this.messageQueue = new ConcurrentLinkedQueue<>();
            try {
                in = new DataInputStream(socket.getInputStream());
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                while (true) {
                    listen();
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            } finally {
                try {
                    in.close();
                    socket.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }

        public void listen() {
            // Spawn listenerThread if it's null or dead
            if (listenerThread == null || !listenerThread.isAlive()) {
                listenerThread = new Thread(() -> {
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
                        } catch (Exception e1) {
                            try {
                                if (bytes.length == 0) { return; }
                                ActualMessage message = new ActualMessage(bytes);
                                messageQueue.add(message);
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                        }
                    }
                });
                listenerThread.start();
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
    }
}
