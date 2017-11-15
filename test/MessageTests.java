import messages.Message;
import messages.MessageType;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

/**
 * Created by gonzalonunez on 10/24/17.
 */

public class MessageTests {

    @Test
    public void testToBytes() {
        Message message = new Message(MessageType.REQUEST, new byte[]{ 1, 2 });
        byte[] bytes = message.toBytes();
        Assert.assertArrayEquals(new byte[]{ 6, 1, 2 }, bytes);
    }

    @Test
    public void testFromBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] typeField = new byte[]{ 1 };
        stream.write(typeField, 0, typeField.length);
        byte[] payload = new byte[]{ 0, 1, 2, 3, 4 };
        stream.write(payload, 0, payload.length);
        byte[] bytes = stream.toByteArray();
        Message message = new Message(bytes);
        Assert.assertEquals(1, message.getType().getValue());
        Assert.assertArrayEquals(payload, message.getPayload());
    }

    @Test
    public void testToAndFromBytes() {
        Message message = new Message(MessageType.CHOKE, new byte[]{ 0, 1, 2, 3, 4, 5 });
        byte[] bytes = message.toBytes();
        Message parsed = new Message(bytes);
        Assert.assertEquals(message, parsed);
    }
}