import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class HandshakeMessage {
    private int peerID;

    public HandshakeMessage(int peerID) {
        this.peerID = peerID;
    }

    public HandshakeMessage(byte[] bytes) throws Exception {
        if (bytes.length != 32) {
            throw new Exception("Incorrect message length: the bytes do not correspond to a handshake message.");
        }
        byte[] headerBytes = Arrays.copyOfRange(bytes, 0, 18);
        String headerField = new String(headerBytes, "ASCII");
        if (!headerField.equals(Constants.HANDSHAKE_HEADER)) {
            throw new Exception("Incorrect header field: " + headerField + " does not equal " + Constants.HANDSHAKE_HEADER);
        }
        peerID = ByteBuffer.wrap(Arrays.copyOfRange(bytes, 28, 32)).getInt();
    }

    public int getPeerID() {
        return peerID;
    }

    public byte[] toBytes() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(ByteBuffer.allocate(4).putInt(32).array());
        stream.write(Constants.HANDSHAKE_HEADER.getBytes("ASCII"));
        byte[] zeroes = new byte[10];
        Arrays.fill(zeroes, (byte)0);
        stream.write(zeroes);
        stream.write(ByteBuffer.allocate(4).putInt(peerID).array());
        return stream.toByteArray();
    }
}
