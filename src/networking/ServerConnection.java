package networking;

import networking.messages.Message;
import networking.messages.ServerMessageHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

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
        private int clientPeerID;
        private Socket connection;
        ServerMessageHandler serverMessageHandler;
        private DataInputStream in;
        private DataOutputStream out;

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
                byte[] request = new byte[length];
                in.readFully(request);

                //TODO - create function that generates a message object given a byte array
                Message messageResponse = null;
                Message messageToSend = getMessageFromHandler(messageResponse);

                if(messageToSend != null){
                    outputMessage(messageToSend);
                }
            }
        }

        void outputMessage(Message message)
        {
            try{
                //TODO - create function that converts a message to a byte array and sends
                out.flush();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        private Message getMessageFromHandler(Message message){
            switch(message.getType()){
                case HANDSHAKE:
                    return serverMessageHandler.responseForClientBitfield(message, clientPeerID);
                case BITFIELD:
                    return serverMessageHandler.responseForClientBitfield(message, clientPeerID);
                case CHOKE:
                    return serverMessageHandler.responseForChoke(message, clientPeerID);
                case UNCHOKE:
                    return serverMessageHandler.responseForUnchoke(message, clientPeerID);
                case INTERESTED:
                    return serverMessageHandler.responseForInterested(message, clientPeerID);
                case NOT_INTERESTED:
                    return serverMessageHandler.responseForUninterested(message, clientPeerID);
                case HAVE:
                    return serverMessageHandler.responseForHave(message, clientPeerID);
                case REQUEST:
                    return serverMessageHandler.responseForRequest(message, clientPeerID);
                case PIECE:
                    return serverMessageHandler.responseForPiece(message, clientPeerID);
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
