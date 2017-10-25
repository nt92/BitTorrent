package networking;

import configs.PeerInfoConfig;
import messages.ClientMessageHandler;
import messages.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientConnection {
    private int peerID;
    private int serverPeerID;
    private ClientMessageHandler clientMessageHandler;
    Socket socket;
    DataOutputStream out;
    DataInputStream in;

    public ClientConnection(int peerID, ClientMessageHandler clientMessageHandler){
        this.peerID = peerID;
        this.clientMessageHandler = clientMessageHandler;
    }

    public void startConnection(PeerInfoConfig peerInfoConfig) throws Exception {
        serverPeerID = peerInfoConfig.getPeerID();
        socket = new Socket(peerInfoConfig.getHostName(), peerInfoConfig.getListeningPort());

        out = new DataOutputStream(socket.getOutputStream());
        out.flush();
        in = new DataInputStream(socket.getInputStream());

        //TODO - create handshake message and send with outputMessage(Message m)

        while (true) {
            int length = in.readInt();
            byte[] response = new byte[length];
            in.readFully(response);

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
                return clientMessageHandler.responseForServerHandshake(message, serverPeerID);
            case BITFIELD:
                return clientMessageHandler.responseForServerBitfield(message, serverPeerID);
            case CHOKE:
                return clientMessageHandler.responseForChoke(message, serverPeerID);
            case UNCHOKE:
                return clientMessageHandler.responseForUnchoke(message, serverPeerID);
            case INTERESTED:
                return clientMessageHandler.responseForInterested(message, serverPeerID);
            case NOT_INTERESTED:
                return clientMessageHandler.responseForUninterested(message, serverPeerID);
            case HAVE:
                return clientMessageHandler.responseForHave(message, serverPeerID);
            case REQUEST:
                return clientMessageHandler.responseForRequest(message, serverPeerID);
            case PIECE:
                return clientMessageHandler.responseForPiece(message, serverPeerID);
            default:
                return null;
        }
    }

    public void closeConnection() throws Exception {
        in.close();
        out.close();
        socket.close();
    }
}
