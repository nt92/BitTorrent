package messages;

import util.Constants;
import util.Utility;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Message {
    private MessageType messageType;

    // For Handshake header is the constant, type is 1 bits, and payload is the peerID
    // For Actual header is the length, type is the type, and payload is the payload
    private byte[] header;
    private byte[] type;
    private byte[] payload;

    public Message(byte[] data, MessageType messageType) throws Exception {
        this.messageType = messageType;

        if(messageType == MessageType.HANDSHAKE){
            // If not correct length for handshake, throw exception
            if (data.length != 32) {
                throw new Exception("Incorrect message length: the bytes do not correspond to a handshake message.");
            }
            byte[] headerBytes = Arrays.copyOfRange(data, 0, 18);
            this.header = headerBytes;

            // If header is not equal to constant value, throw exception
            String headerField = new String(headerBytes, "ASCII");
            if (!headerField.equals(Constants.HANDSHAKE_HEADER)) {
                throw new Exception("Incorrect header field: the bytes do not correspond to a handshake message.");
            }

            // Constant number of zero bits
            this.type = new byte[Constants.NUM_ZERO_BYTE];

            // Payload for handshake is the peerID
            this.payload = Arrays.copyOfRange(data,28, 32);
        } else {
            // From bytes 0 to 4, we have the length of the message
            byte[] messageLength = Arrays.copyOfRange(data, 0, 4);
            this.header = messageLength;

            // Determine the message type using the fifth byte in the array
            this.type = Arrays.copyOfRange(data, 4, 5);

            // Determine the message Payload with the length given to use previously in 0 to 4
            int messageLengthInt = ByteBuffer.wrap(messageLength).getInt();
            this.payload = Arrays.copyOfRange(data, 5, 5 + messageLengthInt);
        }
    }

    // Uses the method in Utility to concatenate header, type, and payload byte arrays into one large byte array
    public byte[] toByteArray(){
        return Utility.concatAll(header, type, payload);
    }

    // Method to check if two messages are equivalent
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof Message) {
            Message other = (Message)o;
            return this.type == other.type &&
                    Arrays.equals(this.payload, other.payload);
        }
        return false;
    }

    public MessageType getType(){
        return messageType;
    }

    public byte[] getPayload() {
        return payload;
    }
}
