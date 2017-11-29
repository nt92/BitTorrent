package messages;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;

public class ActualMessage implements Message {
    private MessageType type;
    private byte[] payload;

    public ActualMessage(MessageType type, byte[] payload) {
        this.type = type;
        this.payload = payload;
    }

    public ActualMessage(byte[] bytes) {
        byte[] typeField = Arrays.copyOfRange(bytes, 0, 1);
        byte typeNum = ByteBuffer.wrap(typeField).get();
        this.type = MessageType.valueOf((int)typeNum);
        this.payload = Arrays.copyOfRange(bytes, 1, bytes.length);
    }

    public MessageType getType() {
        return this.type;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public byte[] toBytes() throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        stream.write(ByteBuffer.allocate(4).putInt(this.payload.length).array());
        stream.write(this.type.getValue());
        stream.write(this.payload, 0, this.payload.length);
        return stream.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof ActualMessage) {
            ActualMessage other = (ActualMessage)o;
            return this.type == other.type && Arrays.equals(this.payload, other.payload);
        }
        return false;
    }
}
