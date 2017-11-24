package messages;

import java.util.Arrays;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;

public class ActualMessage extends Message{

    // Wrapper for ActualMessage so that we can use them under the same umbrella as Message
    public ActualMessage(byte[] data, MessageType type) throws Exception {
        super(data, type);
    }
}
