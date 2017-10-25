package messages;

public interface ServerMessageHandler {
    Message responseForClientHandshake(Message message, int clientPeerID);

    Message responseForChoke(Message message, int clientPeerID);

    Message responseForUnchoke(Message message, int clientPeerID);

    Message responseForInterested(Message message, int clientPeerID);

    Message responseForUninterested(Message message, int clientPeerID);

    Message responseForHave(Message message, int clientPeerID);

    Message responseForClientBitfield(Message message, int clientPeerID);

    Message responseForRequest(Message message, int clientPeerID);

    Message responseForPiece(Message message, int clientPeerID);
}