import configs.CommonConfig;
import configs.PeerInfoConfig;
import files.Logger;
import messages.*;
import networking.ClientConnection;
import networking.ServerConnection;
import util.TimerMethods;

import java.nio.ByteBuffer;
import java.util.*;

public class Peer implements ClientMessageHandler, ServerMessageHandler{
    private int peerID;
    private BitSet bitField;
    private int numPieces;

    // Maps the PeerID to the corresponding PeerInfoConfig
    private Map<Integer, PeerInfoConfig> peerInfoConfigMap;

    // Maps the PeerID to the ClientConnection TCP socket
    private Map<Integer, ClientConnection> connections;

    // Holds the BitSets of other peers for access within the request/have calls
    private Map<Integer, BitSet> otherPeerBitfields;

    // Set of interested peers for the peer to send messages to
    private Set<Integer> interested;

    private CommonConfig commonConfig;

    // TODO: Add actual file data as a wrapper class

    public Peer(int peerID, CommonConfig commonConfig) throws Exception {
        this.peerID = peerID;
        this.commonConfig = commonConfig;

        int fileSize = commonConfig.getFileSize();
        int pieceSize = commonConfig.getPieceSize();

        // Sets number of pieces to be either fileSize / pieceSize if division evenly, otherwise add one
        this.numPieces = fileSize / pieceSize + (fileSize % pieceSize != 0 ? 1 : 0);
        this.bitField = new BitSet(numPieces);

        this.peerInfoConfigMap = new HashMap<>();
        this.connections = new HashMap<>();
        this.otherPeerBitfields = new HashMap<>();

        interested = new HashSet<>();
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

        // Timer methods that run on the unchoking and optimistically unchoking intervals utilizing TimerTask
        Timer getPreferredPeersTimer = new Timer();
        getPreferredPeersTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethods.getPreferredPeers();
            }
        }, 0, 1000 * commonConfig.getUnchokingInterval());

        Timer getOptimisticallyUnchokedPeerTimer = new Timer();
        getOptimisticallyUnchokedPeerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethods.getOptimisticallyUnchokedPeer();
            }
        }, 0, 1000 * commonConfig.getOptimisticUnchokingInterval());
    }

    // Creates a server connection on the given peer that spawns threads for each request
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

    // Creates a client connection between this peer and another peer for data transfer
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
        Logger.logConnectionReceived(peerID, clientPeerID);

        if (!connections.containsKey(clientPeerID)){
            startClientConnection(peerInfoConfigMap.get(clientPeerID));
        }

        // Creates a message using MessageType using peerID as the payload
        return MessageType.HANDSHAKE.createMessageWithPayload(ByteBuffer.allocate(4).putInt(peerID).array());
    }

    @Override
    public Message serverResponseForBitfield(Message message, int clientPeerID) throws Exception {
        // TODO: Log bitfield message

        // If the bitfield is NOT empty, we send back the bitfield of the current peer
        if(!bitField.isEmpty())
            return MessageType.BITFIELD.createMessageWithPayload(bitField.toByteArray());

        return null;
    }

    @Override
    public Message serverResponseForInterested(Message message, int clientPeerID) throws Exception {
        Logger.logReceivedInterestedMessage(peerID, clientPeerID);

        // Add the client peer from the list of interested, don't return anything because
        // choking and unchoking are done via the timers
        interested.add(clientPeerID);

        return null;
    }

    @Override
    public Message serverResponseForUninterested(Message message, int clientPeerID) {
        // TODO: Log Uninterested message

        // Remove the client peer from the list of interested, don't return anything because
        // choking and unchoking are done via the timers
        interested.remove(clientPeerID);

        return null;
    }

    @Override
    public Message serverResponseForRequest(Message message, int clientPeerID) {
        // TODO: Log Request

        // TODO: If the requesting peer is is a neighbor, send it a piece
        // I think this will be done by getting the piece from the request, then getting the piece from the current
        // peer, and then sending it over as a MessageType.PIECE. Don't think anything else would need to be done

        return null;
    }

    // ClientMessageHandler Methods

    @Override
    public Message clientResponseForHandshake(Message message, int serverPeerID) throws Exception {
        Logger.logConnectionMade(peerID, serverPeerID);

        // Sanity check to see if header and peerID are both correct
        if (HandshakeMessage.class.isInstance(message)) {
            if (HandshakeMessage.class.cast(message).getPeerID() != serverPeerID) {
                return null;
            }
        }

        // Add an empty bitfield for the given serverPeerID to the current peer
        otherPeerBitfields.put(serverPeerID, new BitSet(numPieces));

        // Returns a message of type bitfield with the data of the bitfield in a byte array as payload
        return MessageType.BITFIELD.createMessageWithPayload(bitField.toByteArray());
    }

    @Override
    public Message clientResponseForBitfield(Message message, int serverPeerID) {
        // TODO: Log bitfield

        // TODO: Get payload from message and compare to current peer's bitset and determine what we need

        // TODO: Update otherPeerBitfields with current serverPeerID

        // TODO: If we have missing bits the other one has, send interested, otherwise not interested
        return null;
    }

    @Override
    public Message clientResponseForChoke(Message message, int serverPeerID) throws Exception {
        Logger.logChoking(peerID, serverPeerID);

        // Return null message and don't do anything else
        return null;
    }

    @Override
    public Message clientResponseForUnchoke(Message message, int serverPeerID) throws Exception {
        Logger.logUnchoking(peerID, serverPeerID);

        // TODO: Determine missing piece that the server has

        // TODO: Send request message to server for the piece

        return null;
    }

    @Override
    public Message clientResponseForHave(Message message, int serverPeerID) throws Exception {
        int pieceIndex = ByteBuffer.wrap(message.getPayload()).getInt();

        Logger.logReceivedHaveMessage(peerID, serverPeerID, pieceIndex);

        // Set the current peer to know that the other peer has this given bit
        BitSet peerBitSet = otherPeerBitfields.get(serverPeerID);
        peerBitSet.set(pieceIndex);
        otherPeerBitfields.put(serverPeerID, peerBitSet);

        // Send an interested message if current peer does not have this bit
        if(!bitField.get(pieceIndex))
            // Interested messages have empty payload
            return MessageType.INTERESTED.createMessageWithPayload(new byte[] {});

        return null;
    }

    @Override
    public Message clientResponseForPiece(Message message, int serverPeerID) throws Exception {
        // TODO: If file/piece is already in the current peer don't do anything

        // Get the piece index through payload of 0-4 being index and the rest being the actual piece
        byte[] pieceIndexBytes = Arrays.copyOfRange(message.getPayload(), 0, 4);
        int pieceIndex = ByteBuffer.wrap(pieceIndexBytes).getInt();
        byte[] pieceBytes = Arrays.copyOfRange(message.getPayload(), 4, message.getPayload().length);

        Logger.logPieceDownloaded(peerID, serverPeerID, pieceIndex, numPieces);

        // TODO: If peer does not currently have the piece, update the file and the bitset

        // After this, we need to check and see if the file is finished. If so we write the file to actual memory
        // with the file chunker. Then we send a NOT_INTERESTED message to all peers.
        // If the file is not finished, get the next missing piece from the other peer's bitset and request a message
        // Finally, update all the peers that the current peer has the given piece

        return null;
    }
}
