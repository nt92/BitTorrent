package networking;

import configs.PeerInfoConfig;
import messages.ClientMessageHandler;
import messages.HandshakeMessage;
import messages.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientConnection {
    private int peerID;
    private int serverPeerID;
    private ClientMessageHandler clientMessageHandler;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public ClientConnection(int peerID, ClientMessageHandler clientMessageHandler) {
        this.peerID = peerID;
        this.clientMessageHandler = clientMessageHandler;
    }

    public void startConnection(PeerInfoConfig peerInfoConfig) throws Exception {
        serverPeerID = peerInfoConfig.getPeerID();
        socket = new Socket(peerInfoConfig.getHostName(), peerInfoConfig.getListeningPort());

        out = new DataOutputStream(socket.getOutputStream());
        out.flush();
        in = new DataInputStream(socket.getInputStream());

        //FIXME: Is this the *other* peerID? Aka the peerID that we started a connection with? Or is that serverPeerID?
        HandshakeMessage handshakeMessage = new HandshakeMessage(peerID);
        outputBytes(handshakeMessage.toBytes());

        while (true) {
            int length = in.readInt();
            byte[] bytes = new byte[length];
            in.readFully(bytes);

            byte[] bytesToSend = getResponseBytesFromHandler(bytes);
            if(bytesToSend != null){
                outputBytes(bytesToSend);
            }
        }
    }

    void outputBytes(byte[] bytes) throws Exception {
        //TODO: send bytes
        out.flush();
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
        socket.close();
    }
}
