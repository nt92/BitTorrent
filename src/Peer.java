import configs.CommonConfig;
import configs.PeerInfoConfig;
import files.Logger;
import files.FileHandler;
import messages.*;
import networking.ClientConnection;
import networking.ServerConnection;
import util.MapUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;

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
    private ArrayList<Integer> interested;
    private Map<Integer, Integer> peerDownloadRates;

    private CommonConfig commonConfig;

    private ArrayList<Integer> preferredNeighbors;
    Integer optimisticallyUnchokedNeigbor;

    private ServerConnection serverConnection;

    // Holds the data for the actual file
    private FileHandler fileHandler;

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

        //set the size of preferred neighbors
        this.preferredNeighbors = new ArrayList<>(commonConfig.getNumberOfPreferredNeighbors());
        this.optimisticallyUnchokedNeigbor = -1;

        interested = new ArrayList<>();
        fileHandler = new FileHandler(peerID, commonConfig);
    }

    public void start(List<PeerInfoConfig> peerList) throws Exception{
        // Gets the index of the current peer
        int peerIndex = 0;
        while (peerIndex < peerList.size() && peerList.get(peerIndex).getPeerID() != peerID){
            peerIndex++;
        }

        System.out.println("Starting " + peerID);


        PeerInfoConfig currentPeerInfo = peerList.get(peerIndex);

        // If the current peer has the file, we can set its bitfield
        if(currentPeerInfo.getHasFile()){
            bitField.set(0, numPieces);
            fileHandler.chunkFile();
            System.out.println(peerID + " Has the Complete File");
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
        getPreferredPeers();
        getOptimisticallyUnchokedPeer();
    }

    private void getPreferredPeers(){
        Timer getPreferredPeersTimer = new Timer();
        getPreferredPeersTimer.schedule(new TimerTask() {
            @Override
            public void run() {

                ArrayList<Integer> previousPreferredNeighbors = new ArrayList<Integer>();
                ArrayList<Integer> nextPreferredNeighbors = new ArrayList<Integer>();
                // If we have the whole file, WE SHARE! Select random peeps from interested as preferred
                if(fileHandler.hasAllPieces()){
                    preferredNeighbors.clear();
                    if(interested.size() > 0) {
                        while(true) {
                            //get random interested value
                            Integer newPreferredNeighbor;
                            int randomIndex = new Random().nextInt(interested.size());
                            newPreferredNeighbor = interested.get(randomIndex);

                            //check if it was already added to nextPreferredNeighbors
                            if(!nextPreferredNeighbors.contains(newPreferredNeighbor)){
                                nextPreferredNeighbors.add(newPreferredNeighbor);
                            }

                            if(nextPreferredNeighbors.size() >= commonConfig.getNumberOfPreferredNeighbors()) {
                                break;
                            }
                        }
                    }
                }
                // if NOT, find preferred neighbors based on fastest unchoking speeds (how fast you gave me data when I requested it)
                else {
                    // TODO: find preferredNeighbors.size amount of fastest unchokers
                    peerDownloadRates = MapUtil.sortByValue(peerDownloadRates);
                    for(Map.Entry<Integer, Integer> entry: peerDownloadRates.entrySet()){
                        nextPreferredNeighbors.add(entry.getKey());

                        if(nextPreferredNeighbors.size() >= commonConfig.getNumberOfPreferredNeighbors()) {
                            break;
                        }
                    }
                    peerDownloadRates.clear();I
                }

                //now, we CHANGE our neighbors list and let them know we chose them
                    //(+) if they are a veteran, we don't tell them anything. They know what to do.
                    //(+) if they are a newcomer, we send them the "unchoke" message.
                    //(-) if they are kicked out the neighborhood, we let them know with a "choke" message

                for(Integer newPreferredNeighbor : nextPreferredNeighbors){
                    if(preferredNeighbors.contains(newPreferredNeighbor)){
                        //veteran case
                        continue;
                    } else {
                        //newcomer case
                        serverConnection.sendMessageTo(newPreferredNeighbor, "unchoke");
                    }
                }

                for(Integer oldPreferredNeighbor : previousPreferredNeighbors){
                    if(!nextPreferredNeighbors.contains(oldPreferredNeighbor)){
                        //kicked out scenario
                        serverConnection.sendMessageTo(oldPreferredNeighbor, "choke");
                    }
                }

                preferredNeighbors = nextPreferredNeighbors;
                if(preferredNeighbors.size() != 0){
                    System.out.println(peerID + " loves " + preferredNeighbors);

                }

            }
        }, 0, 1000 * commonConfig.getUnchokingInterval());
    }

    private void getOptimisticallyUnchokedPeer(){
        Timer getOptimisticallyUnchokedPeerTimer = new Timer();
        getOptimisticallyUnchokedPeerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // in selecting the next Optimistically Unchocked, they have to
                    // - not be already unchoked (aka preferred neighbor or current OptimiUnchocked
                    // - be interested
                Integer newOptimisticallyUnchokedNeighbor;
                if(interested.size() > 0){
                    while(true){
                        int randomIndex = new Random().nextInt(interested.size());
                        newOptimisticallyUnchokedNeighbor = interested.get(randomIndex);
                        if(newOptimisticallyUnchokedNeighbor != optimisticallyUnchokedNeigbor && !preferredNeighbors.contains(newOptimisticallyUnchokedNeighbor))
                            //is NOT already unchoked as an optimisitically unchoked neighbor && is NOT already unchoked as a preferred neighbor
                            break;
                    }

                    // now that we know our friend, we let them know with an "unchoke"...
                    serverConnection.sendMessageTo(newOptimisticallyUnchokedNeighbor, "unchoke");
                    // kick out our old friend and choke our old friend (unless they are currently ALSO selected as a preferred neighbor)...
                    if(!preferredNeighbors.contains(optimisticallyUnchokedNeigbor)){
                        serverConnection.sendMessageTo(optimisticallyUnchokedNeigbor, "choke");
                    }

                    // and replace our old friend with the new friend
                    optimisticallyUnchokedNeigbor = newOptimisticallyUnchokedNeighbor;

                    try {
                        Logger.logChangeOptimisticallyUnchokedNeighbor(peerID, optimisticallyUnchokedNeigbor);
                        System.out.println(optimisticallyUnchokedNeigbor);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }



            }
        }, 0, 1000 * commonConfig.getOptimisticUnchokingInterval());
    }

    // Creates a server connection on the given peer that spawns threads for each request
    private void startServerConnection(int serverPort){
        this.serverConnection = new ServerConnection(this);
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







        if(peerInfo == null){
            System.out.println("nooo");
        }


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
    public Message serverResponseForHandshake(Message message, Consumer<Integer> clientPeerIDConsumer) throws Exception {
        // Downcasting message to handshake to get peerID
        int clientPeerID = ((HandshakeMessage)message).getPeerID();

        // Modify client peerID based on the consumer of the server
        clientPeerIDConsumer.accept(clientPeerID);

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
    public Message serverResponseForRequest(Message message, int clientPeerID) throws Exception {
        // TODO: Log Request

        // TODO: If the requesting peer is is a neighbor, send it a piece
        // I think this will be done by getting the piece from the request, then getting the piece from the current
        // peer, and then sending it over as a MessageType.PIECE. Don't think anything else would need to be done

        // Get the index from the payload of the message
        int pieceIndex = ByteBuffer.wrap(message.getPayload()).getInt();

        // Get the piece that's requested in the form of a byte array
        byte[] requestedPieceByteArray = fileHandler.getPieceByteData(pieceIndex);

        // Create a piece message to send
        byte[] pieceNumberBytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
        byte[] piecePayloadBytes = new byte[pieceNumberBytes.length + requestedPieceByteArray.length];
        System.arraycopy(pieceNumberBytes, 0, piecePayloadBytes, 0, pieceNumberBytes.length);
        System.arraycopy(requestedPieceByteArray, 0, piecePayloadBytes, pieceNumberBytes.length, requestedPieceByteArray.length);
        return MessageType.PIECE.createMessageWithPayload(piecePayloadBytes);
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
    public Message clientResponseForBitfield(Message message, int serverPeerID) throws Exception {
        // TODO: Log bitfieldx

        // Get the bitset from the payload of the message and then get the missing bits from the file
        BitSet serverBitSet = BitSet.valueOf(message.getPayload());
        List<Integer> missingBitIndices = fileHandler.getMissingPieces(bitField, serverBitSet);

        // Update our other server bitfields so we know it has that given set
        otherPeerBitfields.put(serverPeerID, serverBitSet);

        // If we have missing bits in the current peer, send an interested message otherwise uninterested
        // Both payloads are empty byte arrays as that's part of the protocol description
        if (missingBitIndices.size() > 0) {
            return MessageType.INTERESTED.createMessageWithPayload(new byte[] {});
        }
        else {
            return MessageType.NOT_INTERESTED.createMessageWithPayload(new byte[] {});
        }
    }

    @Override
    public Message clientResponseForChoke(Message message, int serverPeerID) throws Exception {
        Logger.logChoking(peerID, serverPeerID);
        return null;
    }

    @Override
    public Message clientResponseForUnchoke(Message message, int serverPeerID) throws Exception {
        Logger.logUnchoking(peerID, serverPeerID);
        return requestPieceFrom(serverPeerID);
    }

    @Override
    public Message clientResponseForPiece(Message message, int serverPeerID) throws Exception {
        // Get the piece index through payload of 0-4 being index and the rest being the actual piece
        byte[] pieceIndexBytes = Arrays.copyOfRange(message.getPayload(), 0, 4);
        int pieceIndex = ByteBuffer.wrap(pieceIndexBytes).getInt();
        byte[] pieceBytes = Arrays.copyOfRange(message.getPayload(), 4, message.getPayload().length);
        // If file/piece is already in the current peer, don't do anything
        if(fileHandler.hasPiece(pieceIndex)){
            return null;
        }
        fileHandler.setPiece(pieceIndex, pieceBytes);
        Logger.logPieceDownloaded(peerID, serverPeerID, pieceIndex, numPieces);

        // Update all peers that current peer has new piece
        for(Integer peerID : connections.keySet()){
            connections.get(peerID).sendMessageTo("HAVE");
        }

        // If the file is completed, save the FileHandler pieces to disk and send out NOT_INTERESTED
        if(fileHandler.hasAllPieces()){
            fileHandler.aggregateAllPieces();
            Logger.logCompleteFileDownloaded(peerID);
            // TODO: Send NOT_INTERESTED to all peers
            for(Integer peerID : connections.keySet()){
                connections.get(peerID).sendMessageTo("NOT_INTERESTED");
            }
            return MessageType.NOT_INTERESTED.createMessageWithPayload(new byte[] {});
        }

        // record that we got the piece from the specific peer (for choosing who our best friend is)
        peerDownloadRates.put(serverPeerID, peerDownloadRates.get(serverPeerID) + 1);

        //Now that piece was recieved, we request another piece from the server
        return requestPieceFrom(serverPeerID);
    }

    private Message requestPieceFrom(int serverPeerID) throws Exception{
        // Get a random missing piece from the file given the two bitsets
        int randMissingPieceIndex = fileHandler.getRandomMissingPiece(bitField, otherPeerBitfields.get(serverPeerID));

        // If we are missing anything, send a request message
        if (randMissingPieceIndex >= 0) {
            byte[] requestPayload = ByteBuffer.allocate(4).putInt(randMissingPieceIndex).array();
//            connections.get(serverPeerID).sendMessageTo(MessageType.REQUEST.createMessageWithPayload(requestPayload));
            return MessageType.REQUEST.createMessageWithPayload(requestPayload);
        }
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


}
