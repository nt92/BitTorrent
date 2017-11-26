package messages;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import util.Utility;
import util.Constants;

public enum MessageType {
    // Enum to represent each type of message with a value (-1 being not represented but used for Handshake)
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
    // Map to map the MessageType to the value in directions
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
        // If the given message is a handshake, create one with the constant header, constant zero bytes, and the peerID as payload
        if (this == MessageType.HANDSHAKE){
            byte[] header = Constants.HANDSHAKE_HEADER.getBytes();
            byte[] zeroBytes = new byte[Constants.NUM_ZERO_BYTE];

            // Concats headers, number of zeroBytes, and payload for handshake
            return new HandshakeMessage(Utility.concatAll(header, zeroBytes, payload));

        }
        // else, get length of payload as header, type as the given value, and then the payload itself to an actual message
        else{
            byte[] header = ByteBuffer.allocate(4).putInt(payload.length).array();
            byte[] type = new byte[Constants.MESSAGE_TYPE_SIZE];

            // Gets the last bit in the byte representation of the int
            type[0] = ByteBuffer.allocate(4).putInt(this.getValue()).array()[3];

            return new ActualMessage(Utility.concatAll(header, type, payload), this);
        }
    }

    public static Message createMessageWithBytes(byte[] bytes) throws Exception {
        if (bytes.length >= Constants.HANDSHAKE_HEADER.length() &&
                Constants.HANDSHAKE_HEADER.equals(new String(bytes, 0, Constants.HANDSHAKE_HEADER.length()))){
            return new HandshakeMessage(bytes);
        } else{
            // Converted to Byte object for the type to get the MessageType
            Byte messageTypeByte = Arrays.copyOfRange(bytes, 4, 5)[0];
            int messageTypeInt = messageTypeByte.intValue();
            return new ActualMessage(bytes, valueOf(messageTypeInt));
        }
    }
}
