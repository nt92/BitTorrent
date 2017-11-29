package networking;

import messages.HandshakeMessage;
import messages.MessageDispatcher;
import util.Constants;

import java.io.DataInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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

        public RequestHandlerThread(Socket socket, MessageDispatcher dispatcher) {
            this.socket = socket;
            this.dispatcher = dispatcher;
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
                    try {
                        byte[] bytes = new byte[Constants.HANDSHAKE_MESSAGE_SIZE_BYTES];
                        in.readFully(bytes);
                        HandshakeMessage handshakeMessage = new HandshakeMessage(bytes);
                        dispatcher.dispatchMessage(handshakeMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                listenerThread.start();
            }
        }
    }
}
