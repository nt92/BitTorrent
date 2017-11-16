package messages;

public interface ClientMessageHandler {
    Message clientResponseForHandshake(HandshakeMessage message, int serverPeerID);
    Message clientResponseForChoke(Message message, int serverPeerID);
    Message clientResponseForUnchoke(Message message, int serverPeerID);
    Message clientResponseForHave(Message message, int serverPeerID);
    Message clientResponseForBitfield(Message message, int serverPeerID);
    Message clientResponseForPiece(Message message, int serverPeerID);
}