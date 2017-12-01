import java.nio.ByteBuffer;
import java.util.BitSet;

public class MessageFactory {

    static public ActualMessage bitfieldMessage(BitSet bitSet) {
        return new ActualMessage(MessageType.BITFIELD, bitSet.toByteArray());
    }

    static public ActualMessage chokeMessage() {
        return new ActualMessage(MessageType.CHOKE, null);
    }

    static public ActualMessage unchokeMessage() {
        return new ActualMessage(MessageType.UNCHOKE, null);
    }

    static public ActualMessage notInterestedMessage() {
        return new ActualMessage(MessageType.NOT_INTERESTED, null);
    }

    static public ActualMessage interestedMessage() {
        return new ActualMessage(MessageType.INTERESTED, null);
    }

    static public ActualMessage pieceMessage(int pieceIndex, byte[] bytes) {
        byte[] pieceNumberBytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        byte[] payload = new byte[pieceNumberBytes.length + bytes.length];
        System.arraycopy(pieceNumberBytes, 0, payload, 0, pieceNumberBytes.length);
        System.arraycopy(bytes, 0, payload, pieceNumberBytes.length, bytes.length);
        return new ActualMessage(MessageType.PIECE, payload);
    }

    static public ActualMessage requestMessage(int pieceIndex) {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        return new ActualMessage(MessageType.REQUEST, payload);
    }

    static public ActualMessage haveMessage(int pieceIndex) {
        byte[] payload = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        return new ActualMessage(MessageType.HAVE, payload);
    }
}
