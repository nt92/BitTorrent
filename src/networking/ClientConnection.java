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
                //return clientMessageHandler.clientResponseForHandshake(message, serverPeerID);
            case BITFIELD:
                return clientMessageHandler.clientResponseForBitfield(message, serverPeerID);
            case CHOKE:
                return clientMessageHandler.clientResponseForChoke(message, serverPeerID);
            case UNCHOKE:
                return clientMessageHandler.clientResponseForUnchoke(message, serverPeerID);
            case HAVE:
                return clientMessageHandler.clientResponseForHave(message, serverPeerID);
            case PIECE:
                return clientMessageHandler.clientResponseForPiece(message, serverPeerID);
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
