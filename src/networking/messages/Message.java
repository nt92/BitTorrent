package networking.messages;

/**
 * Created by gonzalonunez on 10/24/17.
 */

public class Message {
    int length;
    MessageType type;
    byte[] payload;

    public Message(byte[] bytes) {
        
    }

    public MessageType getType(){
        return type;
    }
}
