package networking;

import messages.Message;
import messages.ServerMessageHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerConnection {
    private ServerSocket serverSocket;
    private ServerMessageHandler serverMessageHandler;

    public ServerConnection(ServerMessageHandler serverMessageHandler){
        this.serverMessageHandler = serverMessageHandler;
    }

    public void start(int port) throws Exception{

        serverSocket = new ServerSocket(port);

        while (true) {
            new HandlerThread(serverSocket.accept(), serverMessageHandler).start();
        }
    }

    public void closeConnection() throws Exception{
        serverSocket.close();
    }

    private static class HandlerThread extends Thread {
        private Socket connection;
        private DataInputStream in;
        private DataOutputStream out;

        private int clientPeerID;
        ServerMessageHandler serverMessageHandler;

        private Map handshakeMap = new HashMap<>();

        public HandlerThread(Socket connection, ServerMessageHandler serverMessageHandler) {
            this.connection = connection;
            this.serverMessageHandler = serverMessageHandler;
        }

        public void run() {
            try{
                handleRequest();
            } catch (Exception e) {
                e.printStackTrace();
            } finally{
                try {
                    closeConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void handleRequest() throws Exception{
            out = new DataOutputStream(connection.getOutputStream());
            out.flush();
            in = new DataInputStream(connection.getInputStream());

            while (true) {
                int length = in.readInt();
                byte[] bytes = new byte[length];
                in.readFully(bytes);

                Message messageResponse = new Message(bytes);
                Message messageToSend = getMessageFromHandler(messageResponse);

                if(messageToSend != null){
                    outputMessage(messageToSend);
                }
            }
        }

        void outputMessage(Message message)
        {
            try{
                byte[] bytes = message.toBytes();
                //TODO - send bytes
                out.flush();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        private Message getMessageFromHandler(Message message){
            switch(message.getType()){
                //case HANDSHAKE:
                    //return serverMessageHandler.serverResponseForHandshake(message, clientPeerID);
                case BITFIELD:
                    return serverMessageHandler.serverResponseForBitfield(message, clientPeerID);
                case INTERESTED:
                    return serverMessageHandler.serverResponseForInterested(message, clientPeerID);
                case NOT_INTERESTED:
                    return serverMessageHandler.serverResponseForUninterested(message, clientPeerID);
                case REQUEST:
                    return serverMessageHandler.serverResponseForRequest(message, clientPeerID);
                default:
                    return null;
            }
        }

        public void closeConnection() throws Exception{
            in.close();
            out.close();
            connection.close();
        }
    }
}
