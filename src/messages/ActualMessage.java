package messages;

public class ActualMessage extends Message{

    // Wrapper for ActualMessage so that we can use them under the same umbrella as Message
    public ActualMessage(byte[] data, MessageType type) throws Exception {
        super(data, type);
        if (type == MessageType.HANDSHAKE) {
            throw new Exception("Incorrect message type: can't construct an ActualMessage with MessageType HANDSHAKE.");
        }
    }
}
