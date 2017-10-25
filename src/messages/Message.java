package messages;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;

/**
 * Created by gonzalonunez on 10/24/17.
 */

public class Message {
    private int length;
    private MessageType type;
    private byte[] payload;

    public Message(int length, MessageType type, byte[] payload) {
        assert(length == 1 + payload.length);
        this.length = length;
        this.type = type;
        this.payload = payload;
    }

    public Message(byte[] bytes) {
        byte[] lengthField = Arrays.copyOfRange(bytes, 0, 4);
        this.length = ByteBuffer.wrap(lengthField).getInt();

        byte[] typeField = Arrays.copyOfRange(bytes, 4, 5);
        byte typeNum = ByteBuffer.wrap(typeField).get();
        this.type = MessageType.valueOf((int)typeNum);

        this.payload = Arrays.copyOfRange(bytes, 5, bytes.length);
    }

    public int getLength() {
        return this.length;
    }

    public MessageType getType() {
        return this.getType();
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] bytes = ByteBuffer.allocate(4).putInt(this.length).array();
        stream.write(bytes, 0, bytes.length);
        stream.write(this.type.getValue());
        stream.write(this.payload, 0, this.payload.length);
        return stream.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof Message) {
            Message other = (Message)o;
            return this.length == other.length &&
                    this.type == other.type &&
                    Arrays.equals(this.payload, other.payload);
        }
        return false;
    }
}
