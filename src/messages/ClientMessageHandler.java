package messages;

public interface ClientMessageHandler {
    Message clientResponseForHandshake(Message message, int serverPeerID) throws Exception;
    Message clientResponseForChoke(Message message, int serverPeerID) throws Exception;
    Message clientResponseForUnchoke(Message message, int serverPeerID) throws Exception;
    Message clientResponseForHave(Message message, int serverPeerID) throws Exception;
    Message clientResponseForBitfield(Message message, int serverPeerID);
    Message clientResponseForPiece(Message message, int serverPeerID) throws Exception;
}