package messages;

public interface ServerMessageHandler {
    Message serverResponseForHandshake(Message message, int clientPeerID);
    Message serverResponseForChoke(Message message, int clientPeerID);
    Message serverResponseForUnchoke(Message message, int clientPeerID);
    Message serverResponseForInterested(Message message, int clientPeerID);
    Message serverResponseForUninterested(Message message, int clientPeerID);
    Message serverResponseForHave(Message message, int clientPeerID);
    Message serverResponseForBitfield(Message message, int clientPeerID);
    Message serverResponseForRequest(Message message, int clientPeerID);
    Message serverResponseForPiece(Message message, int clientPeerID);
}