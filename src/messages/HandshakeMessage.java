package messages;

import util.Constants;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HandshakeMessage extends Message{
    private static String HEADER_FIELD = Constants.HANDSHAKE_HEADER;
    int peerID;

    public HandshakeMessage(int peerID) {
        super(peerID);
    }

    public HandshakeMessage(byte[] bytes) throws Exception {
        super(bytes);
        if (bytes.length != 32) {
            throw new Exception("Incorrect message length: the bytes do not correspond to a handshake message.");
        }
        byte[] headerBytes = Arrays.copyOfRange(bytes, 0, 18);
        String headerField = new String(headerBytes, "ASCII");
        if (!headerField.equals(HEADER_FIELD)) {
            throw new Exception("Incorrect header field: the bytes do not correspond to a handshake message.");
        }
        peerID = ByteBuffer.wrap(Arrays.copyOfRange(bytes,28, 32)).getInt();
    }

    public int getPeerID() {
        return this.peerID;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(HEADER_FIELD.getBytes("ASCII"));
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
