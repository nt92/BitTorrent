import messages.HandshakeMessage;
import org.junit.Assert;
import org.junit.Test;
import util.Constants;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by gonzalonunez on 11/29/17.
 */
public class MessageTests {

    @Test
    public void testHandshakeHeaderFromBytes() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(Constants.HANDSHAKE_HEADER.getBytes("ASCII"));
        byte[] zeroes = new byte[10];
        Arrays.fill(zeroes, (byte)0);
        stream.write(zeroes);
        stream.write(ByteBuffer.allocate(4).putInt(1001).array());
        byte[] bytes = stream.toByteArray();
        HandshakeMessage message = new HandshakeMessage(bytes);
        Assert.assertEquals(1001, message.getPeerID());
    }

    @Test
    public void testHandshakeHeaderToBytes() throws Exception {
        HandshakeMessage message = new HandshakeMessage(1001);
        byte[] bytes = message.toBytes();
        Assert.assertArrayEquals(new byte[] { 80, 50, 80, 70, 73, 76, 69, 83, 72, 65, 82, 73, 78, 71, 80, 82, 79, 74 }, Arrays.copyOfRange(bytes, 0, 18));
        Assert.assertArrayEquals(new byte[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, Arrays.copyOfRange(bytes, 18, 28));
        Assert.assertArrayEquals(ByteBuffer.allocate(4).putInt(1001).array(), Arrays.copyOfRange(bytes, 28, 32));
    }


}
