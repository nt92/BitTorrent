import configs.CommonConfig;
import configs.PeerInfoConfig;
import files.Logger;
import messages.*;
import networking.ClientConnection;
import networking.ServerConnection;

import java.nio.ByteBuffer;
import java.util.*;

public class Peer implements ClientMessageHandler, ServerMessageHandler{
    private Logger logger;
    private int peerID;
    private BitSet bitField;
    private int numPieces;

    private Map<Integer, PeerInfoConfig> peerInfoConfigMap;
    private Map<Integer, ClientConnection> connections;
    private Map<Integer, BitSet> serverBitfields;

    private CommonConfig commonConfig;

    // TODO: Add actual file data as a wrapper class

    public Peer(int peerID, CommonConfig commonConfig) throws Exception {
        this.peerID = peerID;
        logger = new Logger(peerID);
        this.commonConfig = commonConfig;

        int fileSize = commonConfig.getFileSize();
        int pieceSize = commonConfig.getPieceSize();

        // Sets number of pieces to be either fileSize / pieceSize if division evenly, otherwise add one
        this.numPieces = fileSize / pieceSize + (fileSize % pieceSize != 0 ? 1 : 0);
        this.bitField = new BitSet(numPieces);

        this.peerInfoConfigMap = new HashMap<>();
        this.connections = new HashMap<>();
        this.serverBitfields = new HashMap<>();
    }

    public void start(List<PeerInfoConfig> peerList) throws Exception{
        // Gets the index of the current peer
        int peerIndex = 0;
        while (peerIndex < peerList.size() && peerList.get(peerIndex).getPeerID() != peerID){
            peerIndex++;
        }

        PeerInfoConfig currentPeerInfo = peerList.get(peerIndex);

        // If the current peer has the file, we can set its bitfield
        if(currentPeerInfo.getHasFile()){
            bitField.set(0, numPieces);

            // TODO: Load file from file path into memory for the peer
        }

        // Start the server connection for the peer to begin receiving messages
        startServerConnection(currentPeerInfo.getListeningPort());

        // Start TCP connections with the peers in the list before the given one to start server transferring messages/data
        for (int i = 0; i < peerIndex; i++){
            startClientConnection(peerList.get(i));
        }

        // Map that holds all the peerIDs mapped to their corresponding configs
        for (PeerInfoConfig peerInfoConfig : peerList){
            this.peerInfoConfigMap.put(peerInfoConfig.getPeerID(), peerInfoConfig);
        }

        // TODO: Create timers for optimistically choking/unchoking peers
    }

    private void startServerConnection(int serverPort){
        ServerConnection serverConnection = new ServerConnection(this);
        new Thread(() -> {
            try {
                serverConnection.startServer(serverPort);
            } catch (Exception e1) {
                e1.printStackTrace();
            } finally{
                try {
                    serverConnection.closeConnection();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }).start();
    }

    private void startClientConnection(PeerInfoConfig peerInfo){
        ClientConnection clientConnection = new ClientConnection(peerID, this);

        // Add the connection to the other peer to the current peer's map of connections
        connections.put(peerInfo.getPeerID(), clientConnection);
        new Thread(() -> {
            try {
                clientConnection.startConnection(peerInfo);
            } catch (Exception e1) {
                e1.printStackTrace();
            } finally {
                try {
                    clientConnection.closeConnection();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }).start();
    }

    // ServerMessageHandler Methods

    @Override
    public Message serverResponseForHandshake(Message message, int clientPeerID) throws Exception {
        logger.logConnectionReceived(peerID, clientPeerID);

        if (!connections.containsKey(clientPeerID)){
            startClientConnection(peerInfoConfigMap.get(clientPeerID));
        }

        // Creates a message using MessageType using peerID as the payload
        return MessageType.HANDSHAKE.createMessageWithPayload(ByteBuffer.allocate(4).putInt(peerID).array());
    }

    @Override
    public Message serverResponseForInterested(Message message, int clientPeerID) {
        return null;
    }

    @Override
    public Message serverResponseForUninterested(Message message, int clientPeerID) {
        return null;
    }

    @Override
    public Message serverResponseForBitfield(Message message, int clientPeerID) {
        return null;
    }

    @Override
    public Message serverResponseForRequest(Message message, int clientPeerID) {
        return null;
    }

    // ClientMessageHandler Methods

    @Override
    public Message clientResponseForHandshake(Message message, int serverPeerID) throws Exception {
        logger.logConnectionMade(peerID, serverPeerID);

        // Sanity check to see if header and peerID are both correct
        if (HandshakeMessage.class.isInstance(message)) {
            if (HandshakeMessage.class.cast(message).getPeerID() != serverPeerID) {
                return null;
            }
        }

        // Add an empty bitfield for the given serverPeerID to the current peer
        serverBitfields.put(serverPeerID, new BitSet(numPieces));

        // TODO: Create function that returns a BITFIELD based on the current bitField in the peer
        return null;
    }

    @Override
    public Message clientResponseForChoke(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message clientResponseForUnchoke(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message clientResponseForHave(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message clientResponseForBitfield(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message clientResponseForPiece(Message message, int serverPeerID) {
        return null;
    }
}
