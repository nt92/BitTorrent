package networking;

import files.Logger;
import messages.MessageDispatcher;

import java.net.ServerSocket;

public class ServerConnection {
    private int peerID;
    private Logger logger;
    private MessageDispatcher dispatcher;
    private ServerSocket serverSocket;

    public ServerConnection(int peerID, Logger logger, MessageDispatcher dispatcher) {
        this.peerID = peerID;
        this.logger = logger;
        this.dispatcher = dispatcher;
    }

    public void openPort(int port) throws Exception {
        serverSocket = new ServerSocket(port);
        while (true) {
            SocketServer server = new SocketServer(serverSocket.accept(), peerID, logger, dispatcher);
            server.start();
        }
    }

    public void close() throws Exception {
        serverSocket.close();
    }
}

