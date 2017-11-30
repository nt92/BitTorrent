package messages;

import java.util.BitSet;

public class MessageFactory {

    static ActualMessage bitfieldMessage(BitSet bitSet) {
        return new ActualMessage(MessageType.BITFIELD, bitSet.toByteArray());
    }
}
