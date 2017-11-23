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
            if (data.length != 32) {
                throw new Exception("Incorrect message length: the bytes do not correspond to a handshake message.");
            }
            byte[] headerBytes = Arrays.copyOfRange(data, 0, 18);
            this.header = headerBytes;

            String headerField = new String(headerBytes, "ASCII");
            if (!headerField.equals(Constants.HANDSHAKE_HEADER)) {
                throw new Exception("Incorrect header field: the bytes do not correspond to a handshake message.");
            }

            byte[] zeroBits = new byte[Constants.NUM_ZERO_BYTE];
            this.type = zeroBits;

            byte[] peerIDBytes = Arrays.copyOfRange(data,28, 32);
            this.payload = peerIDBytes;
        } else {
            byte[] messageLength = Arrays.copyOfRange(data, 0, 4);
            this.header = messageLength;

            // Determine the message type
            byte[] messageTypeBytes = Arrays.copyOfRange(data, 4, 5);
            this.type = messageTypeBytes;

            // Determine the message Payload
            int messageLengthInt = ByteBuffer.wrap(messageLength).getInt();
            byte[] payloadBytes = Arrays.copyOfRange(data, 5, messageLengthInt);
            this.payload = payloadBytes;
        }
    }

    public byte[] toByteArray(){
        return Utility.concatAll(header, type, payload);
    }

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
