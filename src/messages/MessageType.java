package messages;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gonzalonunez on 10/24/17.
 */

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
}
