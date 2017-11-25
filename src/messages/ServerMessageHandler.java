package messages;

public interface ServerMessageHandler {
    Message serverResponseForHandshake(Message message, int clientPeerID) throws Exception;
    Message serverResponseForInterested(Message message, int clientPeerID) throws Exception;
    Message serverResponseForUninterested(Message message, int clientPeerID);
    Message serverResponseForBitfield(Message message, int clientPeerID) throws Exception;
    Message serverResponseForRequest(Message message, int clientPeerID);
}