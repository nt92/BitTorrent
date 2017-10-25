import junit.framework.TestCase;
import messages.Message;
import messages.MessageType;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Created by gonzalonunez on 10/24/17.
 */

public class MessageTests extends TestCase {

    public void testToBytes() {
        Message message = new Message(11, MessageType.REQUEST, new byte[]{ 1, 2 });
        byte[] bytes = message.toBytes();
        assert(Arrays.equals(bytes, new byte[]{ 0, 0, 0, 11, 6, 1, 2 }));
    }

    public void testFromBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] lengthField = new byte[]{ 0, 0, 0, 5 };
        stream.write(lengthField, 0, lengthField.length);
        byte[] typeField = new byte[]{ 1 };
        stream.write(typeField, 0, typeField.length);
        byte[] payload = new byte[]{ 0, 1, 2, 3, 4 };
        stream.write(payload, 0, payload.length);
        byte[] bytes = stream.toByteArray();
        Message message = new Message(bytes);
        assert(message.getLength() == 5);
        assert(message.getType().getValue() == 1);
        assert(Arrays.equals(message.getPayload(), payload));
    }

    public void testToAndFromBytes() {
        Message message = new Message(11, MessageType.CHOKE, new byte[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 });
        byte[] bytes = message.toBytes();
        assert(bytes.length == message.getLength() + 4);
        Message parsed = new Message(bytes);
        assert(message.equals(parsed));
    }
}