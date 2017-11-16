import messages.HandshakeMessage;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by gonzalonunez on 11/15/17.
 */
public class HandshakeMessageTests {

    @Test
    public void testFromBytes() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write("P2PFILESHARINGPROJ".getBytes("ASCII"));
        byte[] zeroes = new byte[10];
        Arrays.fill(zeroes, (byte)0);
        stream.write(zeroes);
        stream.write(ByteBuffer.allocate(4).putInt(5).array());
        byte[] bytes = stream.toByteArray();
        HandshakeMessage handshakeMessage = new HandshakeMessage(bytes);
        Assert.assertEquals(5, handshakeMessage.getPeerID());
    }

    @Test(expected = Exception.class)
    public void testFromBytesWithBadHeaderThrows() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write("THISISABADHEADER12".getBytes("ASCII"));
        byte[] zeroes = new byte[10];
        Arrays.fill(zeroes, (byte)0);
        stream.write(zeroes);
        stream.write(ByteBuffer.allocate(4).putInt(5).array());
        byte[] bytes = stream.toByteArray();
        HandshakeMessage handshakeMessage = new HandshakeMessage(bytes);
    }

    @Test(expected = Exception.class)
    public void testFromBytesWithBadHeaderLengthThrows() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write("THISISASHORTHEADER".getBytes("ASCII"));
        byte[] zeroes = new byte[10];
        Arrays.fill(zeroes, (byte)0);
        stream.write(zeroes);
        stream.write(ByteBuffer.allocate(4).putInt(5).array());
        byte[] bytes = stream.toByteArray();
        HandshakeMessage handshakeMessage = new HandshakeMessage(bytes);
    }

    @Test(expected = Exception.class)
    public void testFromBytesWithNotEnoughZeroesThrows() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write("P2PFILESHARINGPROJ".getBytes("ASCII"));
        byte[] zeroes = new byte[5];
        Arrays.fill(zeroes, (byte)0);
        stream.write(zeroes);
        stream.write(ByteBuffer.allocate(4).putInt(5).array());
        byte[] bytes = stream.toByteArray();
        HandshakeMessage handshakeMessage = new HandshakeMessage(bytes);
    }

    @Test
    public void testToBytes() throws Exception {
        HandshakeMessage handshakeMessage = new HandshakeMessage(5);
        assert(handshakeMessage.getPeerID() == 5);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write("P2PFILESHARINGPROJ".getBytes("ASCII"));
        byte[] zeroes = new byte[10];
        Arrays.fill(zeroes, (byte)0);
        stream.write(zeroes);
        stream.write(ByteBuffer.allocate(4).putInt(5).array());
        byte[] expected = stream.toByteArray();
        byte[] actual = handshakeMessage.toBytes();
        assert(actual.length == 32);
        assert(actual.equals(expected));
    }
}
