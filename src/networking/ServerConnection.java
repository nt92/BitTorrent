package networking;

import messages.Message;
import messages.MessageType;
import messages.ServerMessageHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class ServerConnection {
    private ServerSocket serverSocket;
    private ServerMessageHandler serverMessageHandler;

    // For sending every peer a message if need be
    private List<RequestHandlerThread> requestHandlerThreads;

    public ServerConnection(ServerMessageHandler serverMessageHandler){
        this.serverMessageHandler = serverMessageHandler;
        this.requestHandlerThreads = new ArrayList<>();
    }

    public void startServer(int port) throws Exception{
        serverSocket = new ServerSocket(port);
        while (true) {
            RequestHandlerThread requestHandlerThread = new RequestHandlerThread(serverSocket.accept(), serverMessageHandler);
            requestHandlerThreads.add(requestHandlerThread);
            requestHandlerThread.start();
        }
    }

    public void closeConnection() throws Exception{
        serverSocket.close();
    }

    // A new thread is created for each request sent to the server
    private static class RequestHandlerThread extends Thread {
        // Socket for connection and input and output streams for i/o
        private Socket connection;
        private DataInputStream in;
        private DataOutputStream out;

        // Concurrent queues to handle incoming and outcoming messages in a thread-safe manner
        private ConcurrentLinkedQueue<Message> outMessageQueue;
        private ConcurrentLinkedQueue<Message> inMessageQueue;

        private int clientPeerID;
        ServerMessageHandler serverMessageHandler;

        // Thread to handle incoming requests
        private Thread listenerThread;

        public RequestHandlerThread(Socket connection, ServerMessageHandler serverMessageHandler) {
            this.connection = connection;
            this.serverMessageHandler = serverMessageHandler;
            this.clientPeerID = 0;
            outMessageQueue = new ConcurrentLinkedQueue<>();
            inMessageQueue = new ConcurrentLinkedQueue<>();

            // Set up the in and out data streams
            try{
                out = new DataOutputStream(connection.getOutputStream());
                out.flush();
                in = new DataInputStream(connection.getInputStream());
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        public void run() {
            try {
                handleRequest();
            } catch (Exception e1) {
                e1.printStackTrace();
            } finally {
                try {
                    closeConnection();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }

        public void handleRequest() throws Exception{
            while (true) {
                // First we will listen for the incoming messages and if the thread does not exist or is dead
                // we will create a new thread and do so
                if (listenerThread == null || !listenerThread.isAlive()){
                    listenerThread = new Thread(() -> {
                        try{
                            int length = in.readInt();
                            byte[] requestMessage = new byte[length];
                            in.readFully(requestMessage);
                            Message incomingMessage = MessageType.createMessageWithBytes(requestMessage);
                            inMessageQueue.add(incomingMessage);
                        } catch (Exception e){
                    e.printStackTrace();
                        }
                    });
                    listenerThread.start();
                }

                // Next we will need to actually receive incoming messages based on the handler utilizing consumer
                handleIncomingMessages((inMessage) -> {
                    try {
                        notifyHandler(inMessage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

                // Finally we need to be able to send outgoing messages
                while(!outMessageQueue.isEmpty()){

                    Message outMessage = outMessageQueue.poll();
                    try{
                        // Output the length of the message followed by the actual bytes of it
                        out.writeInt(outMessage.toByteArray().length);
                        out.write(outMessage.toByteArray());
                        out.flush();
                    }
                    catch(IOException ioException){
                        ioException.printStackTrace();
                    }
                }
//                int length = in.readInt();
//                byte[] bytes = new byte[length];
//                in.readFully(bytes);
//
//                byte[] bytesToSend = getResponseBytesFromHandler(bytes);
//                if (bytesToSend != null) {
//                    outputBytes(bytesToSend);
//                }
            }
        }

        // Method to basically take the message and pass it through as a consumer to the handler
        public void handleIncomingMessages(Consumer<Message> messageConsumer){
            while(!inMessageQueue.isEmpty()){
                messageConsumer.accept(inMessageQueue.poll());
            }
        }

        private void notifyHandler(Message inMessage) throws Exception {

            Message outMessage;
            switch(inMessage.getMessageType()){
                case BITFIELD:
                    outMessage = serverMessageHandler.serverResponseForBitfield(inMessage, clientPeerID);
                    break;
                case INTERESTED:
                    outMessage = serverMessageHandler.serverResponseForInterested(inMessage, clientPeerID);
                    break;
                case NOT_INTERESTED:
                    outMessage = serverMessageHandler.serverResponseForUninterested(inMessage, clientPeerID);
                    break;
                case REQUEST:
                    outMessage = serverMessageHandler.serverResponseForRequest(inMessage, clientPeerID);
                    break;
                case HANDSHAKE:
                    outMessage =  serverMessageHandler.serverResponseForHandshake(inMessage, this::clientPeerIDConsumer);
                    break;
                default:
                    outMessage = null;
            }

            // If it is a valid message, we can add it to the outgoing queue
            if (outMessage != null){
                outMessageQueue.add(outMessage);
            }
        }

        // Function to set the peer id after handshake
        public void clientPeerIDConsumer(int clientPeerID){
            this.clientPeerID = clientPeerID;
        }

        public void closeConnection() throws Exception{
            in.close();
            out.close();
            connection.close();
        }
    }
}
