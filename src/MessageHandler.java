/**
 * Created by gonzalonunez on 11/29/17.
 */
public interface MessageHandler {
    public void handleActualMessage(ActualMessage message, int otherPeerID);
    public void handleHandshakeMessage(HandshakeMessage message);
}
