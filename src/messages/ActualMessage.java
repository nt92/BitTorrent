package messages;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;

public class ActualMessage extends Message{
    public ActualMessage(byte[] data, MessageType type) throws Exception {
        super(data, type);
    }
}
