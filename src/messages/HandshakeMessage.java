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
}
