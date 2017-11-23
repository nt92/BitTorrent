package messages;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import util.Utility;
import util.Constants;

public enum MessageType {
    HANDSHAKE(-1),
    CHOKE(0),
    UNCHOKE(1),
    INTERESTED(2),
    NOT_INTERESTED(3),
    HAVE(4),
    BITFIELD(5),
    REQUEST(6),
    PIECE(7);

    private int value;
    private static Map map = new HashMap<>();

    MessageType(int value) {
        this.value = value;
    }

    static {
        for (MessageType messageType : MessageType.values()) {
            map.put(messageType.value, messageType);
        }
    }

    public static MessageType valueOf(int messageType) {
        return (MessageType)map.get(messageType);
    }

    public int getValue() {
        return value;
    }

    // We create a message given a byte[] payload to create messages when sending
    public Message createMessageWithPayload(byte[] payload) throws Exception {
        if (this == MessageType.HANDSHAKE){
            byte[] header = Constants.HANDSHAKE_HEADER.getBytes();
            byte[] zeroBytes = new byte[Constants.NUM_ZERO_BYTE];

            // Concats headers, unmber of zeroBytes, and payload for handshake
            return new HandshakeMessage(Utility.concatAll(header, zeroBytes, payload));

        } else{
            // TODO: Create a message for ActualMessage based on payload as well
            return null;
        }
    }

    // TODO: Create a function for creating a message through a byte array for processing a message in byte[] format
}
