package messages;

import util.Constants;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static messages.MessageType.HANDSHAKE;

public class HandshakeMessage extends Message {
    int peerID;

    public HandshakeMessage(byte[] bytes) throws Exception {
        super(bytes, HANDSHAKE);
        peerID = ByteBuffer.wrap(Arrays.copyOfRange(bytes,28, 32)).getInt();
    }

    public int getPeerID() {
        return this.peerID;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(Constants.HANDSHAKE_HEADER.getBytes("ASCII"));
            byte[] zeroes = new byte[10];
            Arrays.fill(zeroes, (byte)0);
            stream.write(zeroes);
            stream.write(ByteBuffer.allocate(4).putInt(peerID).array());
            return stream.toByteArray();
        } catch (Exception e) {
            System.out.println("ERROR: " + e);
            return null;
        }
    }
}
