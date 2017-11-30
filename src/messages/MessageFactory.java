package messages;

import java.util.BitSet;

public class MessageFactory {

    static public ActualMessage bitfieldMessage(BitSet bitSet) {
        return new ActualMessage(MessageType.BITFIELD, bitSet.toByteArray());
    }

    static public ActualMessage notInterestedMessage() {
        return new ActualMessage(MessageType.NOT_INTERESTED, null);
    }

    static public ActualMessage interestedMessage() {
        return new ActualMessage(MessageType.INTERESTED, null);
    }
}
