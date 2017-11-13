package messages;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;

/**
 * Created by gonzalonunez on 10/24/17.
 */

public class Message {
    private MessageType type;
    private byte[] payload;

    public Message(MessageType type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }

    public Message(byte[] bytes) {
        byte[] typeField = Arrays.copyOfRange(bytes, 0, 1);
        byte typeNum = ByteBuffer.wrap(typeField).get();
        this.type = MessageType.valueOf((int)typeNum);
        this.payload = Arrays.copyOfRange(bytes, 1, bytes.length);
    }

    public MessageType getType() {
        return this.getType();
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(this.type.getValue());
        stream.write(this.payload, 0, this.payload.length);
        return stream.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof Message) {
            Message other = (Message)o;
            return this.type == other.type &&
                    Arrays.equals(this.payload, other.payload);
        }
        return false;
    }
}
