package messages;

public class MessageDispatcher {

    private MessageHandler handler;

    public MessageDispatcher(MessageHandler handler) {
        this.handler = handler;
    }

    public void dispatchMessage(ActualMessage message, int otherPeerID) {
        handler.handleActualMessage(message, otherPeerID);
    }

    public void dispatchMessage(HandshakeMessage message) {
        handler.handleHandshakeMessage(message);
    }
}
