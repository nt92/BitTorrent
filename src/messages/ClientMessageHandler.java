package messages;

public interface ClientMessageHandler {
    Message responseForServerHandshake(Message message, int serverPeerID);

    Message responseForChoke(Message message, int serverPeerID);

    Message responseForUnchoke(Message message, int serverPeerID);

    Message responseForInterested(Message message, int serverPeerID);

    Message responseForUninterested(Message message, int serverPeerID);

    Message responseForHave(Message message, int serverPeerID);

    Message responseForServerBitfield(Message message, int serverPeerID);

    Message responseForRequest(Message message, int serverPeerID);

    Message responseForPiece(Message message, int serverPeerID);
}