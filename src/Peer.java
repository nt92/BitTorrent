import java.nio.ByteBuffer;
import java.util.*;

public class Peer implements MessageHandler {
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
    private Map<Integer, Boolean> sentHandshakeTo;

    private CommonConfig commonConfig;

    private ArrayList<Integer> preferredNeighbors;
    private List<PeerInfoConfig> peerList;
    Integer optimisticallyUnchokedNeighbor;

    private ServerConnection serverConnection;

    // Holds the data for the actual file
    private FileHandler fileHandler;

    // Handles incoming messages
    private MessageDispatcher messageDispatcher;

    private Logger logger;
    private Random random = new Random();

    private Timer getPreferredPeersTimer;
    private Timer getOptimisticallyUnchokedPeerTimer;

    private static BitSet allTrueBitfield;

    public Peer(int peerID, CommonConfig commonConfig) throws Exception {
        this.peerID = peerID;
        this.commonConfig = commonConfig;

        int fileSize = commonConfig.getFileSize();
        int pieceSize = commonConfig.getPieceSize();

        this.numPieces = fileSize / pieceSize + (fileSize % pieceSize != 0 ? 1 : 0);
        this.bitField = new BitSet(numPieces);

        this.peerInfoConfigMap = new HashMap<>();
        this.connections = new HashMap<>();
        this.otherPeerBitfields = new HashMap<>();

        //set the size of preferred neighbors
        this.preferredNeighbors = new ArrayList<>(commonConfig.getNumberOfPreferredNeighbors());
        this.optimisticallyUnchokedNeighbor = -1;
        this.peerDownloadRates = new HashMap<>();
        this.sentHandshakeTo = new HashMap<>();

        interested = new ArrayList<>();
        fileHandler = new FileHandler(peerID, commonConfig);
        messageDispatcher = new MessageDispatcher(this);
        logger = new Logger(peerID);
    }

    public void start(List<PeerInfoConfig> peerList) throws Exception {
        // Gets the index of the current peer
        this.peerList = peerList;
        int peerIndex = 0;
        while (peerIndex < peerList.size() && peerList.get(peerIndex).getPeerID() != peerID) {
            peerIndex++;
        }

        PeerInfoConfig currentPeerInfo = peerList.get(peerIndex);

        // Update the current peers initial bitfield, based on whether it has the file or not
        bitField.set(0, numPieces, currentPeerInfo.getHasFile());

        allTrueBitfield = new BitSet();
        allTrueBitfield.set(0, numPieces, true);

        // Set all other bitfields to empty ones by default
        for (int i = 0; i < peerList.size(); i++) {
            int otherID = peerList.get(i).getPeerID();
            if (otherID != peerID) {
                otherPeerBitfields.put(otherID, new BitSet());
            }
        }

        // If the current peer has the file, chunk it
        if (currentPeerInfo.getHasFile()) {
            fileHandler.chunkFile();
            fileHandler.aggregateAllPieces();
        }

        // Start the server in order to being receiving messages
        startServer(currentPeerInfo.getListeningPort());

        // Start TCP connections with the peers in the list before the given one to start server transferring messages/data
        for (int i = 0; i < peerIndex; i++) {
            startClientConnection(peerList.get(i));
        }

        // Map that holds all the peerIDs mapped to their corresponding configs
        for (PeerInfoConfig peerInfoConfig : peerList){
            this.peerInfoConfigMap.put(peerInfoConfig.getPeerID(), peerInfoConfig);
        }

        while (connections.size() < peerIndex) {
            Thread.sleep(1000);
        }

        // Timer methods that run on the unchoking and optimistically unchoking intervals utilizing TimerTask
        getPreferredPeers();
        getOptimisticallyUnchokedPeer();
    }

    private void getPreferredPeers() {
        getPreferredPeersTimer = new Timer();
        getPreferredPeersTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ArrayList<Integer> previousPreferredNeighbors = new ArrayList<Integer>();
                ArrayList<Integer> nextPreferredNeighbors = new ArrayList<Integer>();
                // If we have the whole file, WE SHARE! Select random peeps from interested as preferred
                if (fileHandler.hasAllPieces()) {
                    preferredNeighbors.clear();
                    if (interested.size() > 0) {
                        while (true) {
                            //get random interested value
                            if (interested.isEmpty()) { break; }
                            Integer newPreferredNeighbor;
                            int randomIndex = interested.size() > 1 ? random.nextInt(interested.size()) : 0;
                            newPreferredNeighbor = interested.get(randomIndex);
                            //check if it was already added to nextPreferredNeighbors
                            if (!nextPreferredNeighbors.contains(newPreferredNeighbor)){
                                nextPreferredNeighbors.add(newPreferredNeighbor);
                            }
                            if (nextPreferredNeighbors.size() >= commonConfig.getNumberOfPreferredNeighbors()) {
                                break;
                            }
                        }
                    }
                }
                // if NOT, find preferred neighbors based on fastest unchoking speeds (how fast you gave me data when I requested it)
                else {
                    if (!peerDownloadRates.isEmpty()) {
                        peerDownloadRates = MapUtil.sortByValue(peerDownloadRates);
                        for (Map.Entry<Integer, Integer> entry: peerDownloadRates.entrySet()) {
                            nextPreferredNeighbors.add(entry.getKey());
                            if (nextPreferredNeighbors.size() >= commonConfig.getNumberOfPreferredNeighbors()) {
                                break;
                            }
                        }
                        peerDownloadRates.clear();
                    }
                }

                //now, we CHANGE our neighbors list and let them know we chose them
                    //(+) if they are a veteran, we don't tell them anything. They know what to do.
                    //(+) if they are a newcomer, we send them the "unchoke" message.
                    //(-) if they are kicked out the neighborhood, we let them know with a "choke" message

                for (Integer newPreferredNeighbor : nextPreferredNeighbors) {
                    if (preferredNeighbors.contains(newPreferredNeighbor)) {
                        //veteran case
                        continue;
                    } else {
                        //newcomer case
                        try {
                            ClientConnection connection = connectionForPeerID(newPreferredNeighbor);
                            if (connection != null) {
                                connection.sendActualMessage(MessageFactory.unchokeMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                for (Integer oldPreferredNeighbor : previousPreferredNeighbors) {
                    if (!nextPreferredNeighbors.contains(oldPreferredNeighbor)) {
                        //kicked out scenario
                        try {
                            ClientConnection connection = connectionForPeerID(oldPreferredNeighbor);
                            if (connection != null) {
                                connection.sendActualMessage(MessageFactory.chokeMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                preferredNeighbors = nextPreferredNeighbors;
                if (!preferredNeighbors.isEmpty()) {
                    logger.logChangedPreferredNeighbors(peerID, preferredNeighbors);
                }

            }
        }, 0, 1000 * commonConfig.getUnchokingInterval());
    }

    private void getOptimisticallyUnchokedPeer() {
        getOptimisticallyUnchokedPeerTimer = new Timer();
        getOptimisticallyUnchokedPeerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // in selecting the next Optimistically Unchocked, they have to
                    // - not be already unchoked (aka preferred neighbor or current OptimiUnchocked
                    // - be interested
                Integer newOptimisticallyUnchokedNeighbor = -1;
                if (interested.size() > 0) {
                    while (true) {
                        if (interested.isEmpty()) { break; }
                        int randomIndex = interested.size() > 1 ? new Random().nextInt(interested.size()) : 0;
                        newOptimisticallyUnchokedNeighbor = interested.get(randomIndex);
                        if (newOptimisticallyUnchokedNeighbor != optimisticallyUnchokedNeighbor &&
                                !preferredNeighbors.contains(newOptimisticallyUnchokedNeighbor))
                        {
                            // is NOT already unchoked as an optimisitically unchoked neighbor && is NOT already unchoked as a preferred neighbor
                            break;
                        }
                    }

                    try {
                        // now that we know our friend, we let them know with an "unchoke"...
                        ClientConnection connection = connectionForPeerID(newOptimisticallyUnchokedNeighbor);
                        if (connection != null) {
                            connection.sendActualMessage(MessageFactory.unchokeMessage());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // kick out our old friend and choke our old friend (unless they are currently ALSO selected as a preferred neighbor)...
                    if (!preferredNeighbors.contains(optimisticallyUnchokedNeighbor)) {
                        try {
                            ClientConnection connection = connectionForPeerID(optimisticallyUnchokedNeighbor);
                            if (connection != null) {
                                connection.sendActualMessage(MessageFactory.chokeMessage());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    // and replace our old friend with the new friend
                    optimisticallyUnchokedNeighbor = newOptimisticallyUnchokedNeighbor;

                    try {
                        logger.logChangeOptimisticallyUnchokedNeighbor(peerID, optimisticallyUnchokedNeighbor);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 1000 * commonConfig.getOptimisticUnchokingInterval());
    }

    public ClientConnection connectionForPeerID(int peerID) {
        if (peerID == -1) {
            return null;
        }
        if (!connections.containsKey(peerID)) {
            startClientConnection(peerInfoConfigMap.get(peerID));
        }
        return connections.get(peerID);
    }

    // Starts server in order to receive messages
    private void startServer(int serverPort) {
        ServerConnection serverConnection = new ServerConnection(peerID, logger, messageDispatcher);
        new Thread(() -> {
            try {
                serverConnection.openPort(serverPort);
            } catch (Exception e1) {
                e1.printStackTrace();
            } finally {
                try {
                    serverConnection.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }).start();
    }

    // Starts connection to another client in order to send messages
    private void startClientConnection(PeerInfoConfig peerInfo) {
        ClientConnection clientConnection = new ClientConnection(peerID);
        connections.put(peerInfo.getPeerID(), clientConnection);
        sentHandshakeTo.put(peerInfo.getPeerID(), true);
        new Thread(() -> {
            try {
                clientConnection.openConnectionWithConfig(peerInfo);
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
        logger.logConnectionMade(peerID, peerInfo.getPeerID());
    }

    // MessageHandler

    public void handleActualMessage(ActualMessage message, int otherPeerID) {
        ClientConnection connection = connectionForPeerID(otherPeerID);
        ActualMessage actualMessage;
        switch (message.getType()) {
            case BITFIELD:
                actualMessage = responseForBitfield(message, otherPeerID);
                break;
            case INTERESTED:
                actualMessage = responseForInterested(message, otherPeerID);
                break;
            case NOT_INTERESTED:
                actualMessage = serverResponseForUninterested(message, otherPeerID);
                break;
            case REQUEST:
                actualMessage = responseForRequest(message, otherPeerID);
                break;
            case CHOKE:
                actualMessage = responseForChoke(message, otherPeerID);
                break;
            case UNCHOKE:
                actualMessage = responseForUnchoke(message, otherPeerID);
                break;
            case PIECE:
                actualMessage = responseForPiece(message, otherPeerID);
                break;
            case HAVE:
                actualMessage = responseForHave(message, otherPeerID);
                break;
            default:
                actualMessage = null;
        }
        if (actualMessage != null) {
            try {
                if (connection != null) {
                    connection.sendActualMessage(actualMessage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void handleHandshakeMessage(HandshakeMessage message) {
        ClientConnection connection = connectionForPeerID(message.getPeerID());

        if (!sentHandshakeTo.containsKey(message.getPeerID())) {
            HandshakeMessage handshakeMessage = new HandshakeMessage(peerID);
            try {
                if (connection != null) {
                    connection.sendHandshakeMessage(handshakeMessage);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (bitField.isEmpty()) {
            return;
        }

        ActualMessage actualMessage = MessageFactory.bitfieldMessage(bitField);
        try {
            if (connection != null) {
                connection.sendActualMessage(actualMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Message Responses

    public ActualMessage responseForBitfield(ActualMessage message, int otherPeerID) {
        BitSet otherBitfield = BitSet.valueOf(message.getPayload());
        List missingPieces = fileHandler.getMissingPieces(bitField, otherBitfield);
        otherPeerBitfields.put(otherPeerID, otherBitfield);
        if (missingPieces.isEmpty()) {
            return MessageFactory.notInterestedMessage();
        } else {
            return MessageFactory.interestedMessage();
        }
    }

    public ActualMessage responseForInterested(ActualMessage message, int otherPeerID) {
        logger.logReceivedInterestedMessage(peerID, otherPeerID);
        if (!interested.contains(otherPeerID)) {
            interested.add(otherPeerID);
        }
        return null;
    }

    public ActualMessage serverResponseForUninterested(ActualMessage message, int otherPeerID) {
        logger.logReceivedNotInterestedMessage(peerID, otherPeerID);
        if (interested.indexOf(otherPeerID) != -1) {
            interested.remove(interested.indexOf(otherPeerID));
        }
        return null;
    }

    public ActualMessage responseForRequest(ActualMessage message, int otherPeerID) {
        if (!preferredNeighbors.contains(otherPeerID) && optimisticallyUnchokedNeighbor != otherPeerID) {
            return null;
        }
        // Get the index from the payload of the message
        int pieceIndex = ByteBuffer.wrap(message.getPayload()).getInt();
        // Get the piece that's requested in the form of a byte array
        byte[] bytes = fileHandler.getPieceByteData(pieceIndex);
        // Return a piece message
        return MessageFactory.pieceMessage(pieceIndex, bytes);
    }

    public ActualMessage responseForChoke(ActualMessage message, int otherPeerID) {
        logger.logChoking(peerID, otherPeerID);
        return null;
    }

    public ActualMessage responseForUnchoke(ActualMessage message, int otherPeerID) {
        logger.logUnchoking(peerID, otherPeerID);
        return requestPieceFrom(otherPeerID);
    }

    public ActualMessage responseForPiece(ActualMessage message, int otherPeerID) {
        // Get the piece index through payload of 0-4 being index and the rest being the actual piece
        byte[] pieceIndexBytes = Arrays.copyOfRange(message.getPayload(), 0, 4);
        int pieceIndex = ByteBuffer.wrap(pieceIndexBytes).getInt();

        byte[] pieceBytes = Arrays.copyOfRange(message.getPayload(), 4, message.getPayload().length);

        // If file/piece is already in the current peer, don't do anything
        if(fileHandler.hasPiece(pieceIndex)){
            return null;
        }

        fileHandler.setPiece(pieceIndex, pieceBytes);
        bitField.set(pieceIndex);
        logger.logPieceDownloaded(peerID, otherPeerID, pieceIndex, fileHandler.getPiecesCount());

        // If the file is completed, save the FileHandler pieces to disk and send out NOT_INTERESTED
        if (fileHandler.hasAllPieces()) {
            fileHandler.aggregateAllPieces();
            logger.logCompleteFileDownloaded(peerID);
            try {
                for (Integer peerID : connections.keySet()) {
                    connections.get(peerID).sendActualMessage(MessageFactory.notInterestedMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            terminateIfNeeded();
        }

        // Update all peers that current peer has new piece
        for (Integer peerID : connections.keySet()) {
            try {
                connections.get(peerID).sendActualMessage(MessageFactory.haveMessage(pieceIndex));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Record that we got the piece from the specific peer (for choosing who our best friend is)
        peerDownloadRates.put(otherPeerID, peerDownloadRates.containsKey(otherPeerID) ? peerDownloadRates.get(otherPeerID) + 1 : 1);

        // Now that piece was recieved, we request another piece from the server
        return !fileHandler.hasAllPieces() ? requestPieceFrom(otherPeerID) : null;
    }

    private ActualMessage requestPieceFrom(int otherPeerID) {
        // Get a random missing piece from the file given the two BitSets
        int randMissingPieceIndex = fileHandler.getRandomMissingPiece(bitField, otherPeerBitfields.get(otherPeerID));
        // If the index is -1, we are not missing anything
        if (randMissingPieceIndex == -1) {
            return null;
        }
        return MessageFactory.requestMessage(randMissingPieceIndex);
    }

    private ActualMessage responseForHave(ActualMessage message, int otherPeerID) {
        int pieceIndex = ByteBuffer.wrap(message.getPayload()).getInt();
        logger.logReceivedHaveMessage(peerID, otherPeerID, pieceIndex);

        BitSet peerBitSet = otherPeerBitfields.containsKey(otherPeerID) ? otherPeerBitfields.get(otherPeerID) : new BitSet();
        peerBitSet.set(pieceIndex);
        otherPeerBitfields.put(otherPeerID, peerBitSet);

        terminateIfNeeded();

        // If we already have this piece, no need to do anything
        if (bitField.get(pieceIndex)) {
            return null;
        }
        return MessageFactory.interestedMessage();
    }

    private void terminateIfNeeded() {
        boolean everybodyHasAllPieces = fileHandler.hasAllPieces();
        for (Integer peerID : otherPeerBitfields.keySet()) {
            BitSet bitfield = otherPeerBitfields.get(peerID);
            boolean hasAllPieces = bitfield.cardinality() == numPieces;
            everybodyHasAllPieces = everybodyHasAllPieces && hasAllPieces;
        }
        if (everybodyHasAllPieces) {
            System.exit(1);
        }
    }
}
