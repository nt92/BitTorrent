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

            // Concats headers, number of zeroBytes, and payload for handshake
            return new HandshakeMessage(Utility.concatAll(header, zeroBytes, payload));

        } else{
            byte[] header = ByteBuffer.allocate(4).putInt(payload.length).array();
            byte[] type = new byte[Constants.MESSAGE_TYPE_SIZE];

            // Gets the last bit in the byte representation of the int
            type[0] = ByteBuffer.allocate(4).putInt(this.getValue()).array()[3];

            return new ActualMessage(Utility.concatAll(header, type, payload), this);
        }
    }

    public Message createMessageWithBytes(byte[] data) throws Exception {
        if (this == MessageType.HANDSHAKE){
            return new HandshakeMessage(data);
        } else{
            return new ActualMessage(data, this);
        }
    }
}
