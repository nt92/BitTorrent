package messages;

import networking.ClientConnection;
import networking.ConnectionProvider;
import networking.PeerInfoProvider;

public class MessageDispatcher {

    private ConnectionProvider connectionProvider;
    private PeerInfoProvider infoProvider;

    public MessageDispatcher(ConnectionProvider connectionProvider, PeerInfoProvider infoProvider) {
        this.connectionProvider = connectionProvider;
        this.infoProvider = infoProvider;
    }

    public void dispatchMessage(ActualMessage message, int senderPeerID) {
        ClientConnection connection = connectionProvider.connectionForPeerID(senderPeerID);
        /*
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
        */
    }

    public void dispatchMessage(HandshakeMessage message) throws Exception {
        ClientConnection connection = connectionProvider.connectionForPeerID(message.getPeerID());
        ActualMessage actualMessage = MessageFactory.bitfieldMessage(infoProvider.currentBitfield());
        connection.sendActualMessage(actualMessage);
    }
}
