package networking;

import configs.PeerInfoConfig;
import messages.ClientMessageHandler;
import messages.HandshakeMessage;
import messages.Message;
import messages.MessageType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class ClientConnection {
    private Socket clientSocket;
    private DataOutputStream out;
    private DataInputStream in;

    // Concurrent queues to handle incoming and outcoming messages in a thread-safe manner
    private ConcurrentLinkedQueue<Message> outMessageQueue;
    private ConcurrentLinkedQueue<Message> inMessageQueue;

    private int peerID;
    private int serverPeerID;
    private ClientMessageHandler clientMessageHandler;

    private Thread listenerThread;

    public ClientConnection(int peerID, ClientMessageHandler clientMessageHandler) {
        this.peerID = peerID;
        this.clientMessageHandler = clientMessageHandler;

        try{
            out = new DataOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new DataInputStream(clientSocket.getInputStream());
        } catch(Exception e){
            e.printStackTrace();
        }

        this.outMessageQueue = new ConcurrentLinkedQueue<>();
        this.inMessageQueue = new ConcurrentLinkedQueue<>();
    }

    public void startConnection(PeerInfoConfig peerInfoConfig) throws Exception {
        serverPeerID = peerInfoConfig.getPeerID();
        clientSocket = new Socket(peerInfoConfig.getHostName(), peerInfoConfig.getListeningPort());

        // Sends handshake message initially
        sendHandshakeMessage();

        while (true) {
            // First we will listen for the incoming messages and if the thread does not exist or is dead
            // we will create a new thread and do so
            if (listenerThread == null || !listenerThread.isAlive()){
                listenerThread = new Thread(() -> {
                    try{
                        int length = in.readInt();
                        byte[] requestMessage = new byte[length];
                        in.readFully(requestMessage);
                        // TODO: Parse requestMessage and get the appropriate response
                        // TODO: Add the new message to the inMessageQueue
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

            // Finally we need to be able to send outgoing mnessages
            while(!outMessageQueue.isEmpty()){

                Message outMessage = outMessageQueue.poll();
                try{
                    // Output the length of the message followed by the actual bytes of it
                    out.writeInt(outMessage.toBytes().length);
                    out.write(outMessage.toBytes());
                    out.flush();
                }
                catch(IOException ioException){
                    ioException.printStackTrace();
                }
            }
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
        switch(inMessage.getType()){
            case BITFIELD:
                outMessage = clientMessageHandler.clientResponseForBitfield(inMessage, serverPeerID);
                break;
            case CHOKE:
                outMessage = clientMessageHandler.clientResponseForChoke(inMessage, serverPeerID);
                break;
            case UNCHOKE:
                outMessage = clientMessageHandler.clientResponseForUnchoke(inMessage, serverPeerID);
                break;
            case HAVE:
                outMessage = clientMessageHandler.clientResponseForHave(inMessage, serverPeerID);
                break;
            case PIECE:
                outMessage = clientMessageHandler.clientResponseForPiece(inMessage, serverPeerID);
                break;
            case HANDSHAKE:
                outMessage =  clientMessageHandler.clientResponseForHandshake(inMessage, serverPeerID);
                break;
            default:
                outMessage = null;
        }

        // If it is a valid message, we can add it to the outgoing queue
        if (outMessage != null){
            outMessageQueue.add(outMessage);
        }
    }

    public void sendHandshakeMessage(){
        // TODO: Create a handshake message
        // TODO: Add handshake message to the out queue
    }

    private byte[] getResponseBytesFromHandler(byte[] bytes) {
        try {
            HandshakeMessage handshakeMessage = new HandshakeMessage(bytes);
            return clientMessageHandler.clientResponseForHandshake(handshakeMessage, serverPeerID).toBytes();
        } catch (Exception e) {
            Message message = new Message(bytes);
            switch (message.getType()) {
                case BITFIELD:
                    return clientMessageHandler.clientResponseForBitfield(message, serverPeerID).toBytes();
                case CHOKE:
                    return clientMessageHandler.clientResponseForChoke(message, serverPeerID).toBytes();
                case UNCHOKE:
                    return clientMessageHandler.clientResponseForUnchoke(message, serverPeerID).toBytes();
                case HAVE:
                    return clientMessageHandler.clientResponseForHave(message, serverPeerID).toBytes();
                case PIECE:
                    return clientMessageHandler.clientResponseForPiece(message, serverPeerID).toBytes();
                default:
                    return null;
            }
        }
    }

    public void closeConnection() throws Exception {
        in.close();
        out.close();
        clientSocket.close();
    }
}
