import com.sun.deploy.util.SessionState;
import configs.CommonConfig;
import configs.PeerInfoConfig;
import files.FileHandler;
import files.Logger;
import messages.*;
import networking.ClientConnection;
import networking.ConnectionProvider;
import networking.ServerConnection;
import util.MapUtil;

import java.nio.ByteBuffer;
import java.util.*;

public class Peer implements ConnectionProvider, MessageHandler {
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
    Integer optimisticallyUnchokedNeighbor;

    private ServerConnection serverConnection;

    // Holds the data for the actual file
    private FileHandler fileHandler;

    // Handles incoming messages
    private MessageDispatcher messageDispatcher;

    private Logger logger;
    private Random random = new Random();

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

        interested = new ArrayList<>();
        fileHandler = new FileHandler(peerID, commonConfig);
        messageDispatcher = new MessageDispatcher(this);
        logger = new Logger(peerID);
    }

    public void start(List<PeerInfoConfig> peerList) throws Exception {
        // Gets the index of the current peer
        int peerIndex = 0;
        while (peerIndex < peerList.size() && peerList.get(peerIndex).getPeerID() != peerID) {
            peerIndex++;
        }

        System.out.println("Starting " + peerID);


        PeerInfoConfig currentPeerInfo = peerList.get(peerIndex);

        // If the current peer has the file, we can set its bitfield
        if (currentPeerInfo.getHasFile()) {
            bitField.set(0, numPieces);
            fileHandler.chunkFile();
            System.out.println(peerID + " Has the Complete File");
        }

        // Start the server in order to being receiving messages
        startServer(currentPeerInfo.getListeningPort());

        // Start TCP connections with the peers in the list before the given one to start server transferring messages/data
        for (int i = 0; i < peerIndex; i++){
            startClientConnection(peerList.get(i));
        }

        // Map that holds all the peerIDs mapped to their corresponding configs
        for (PeerInfoConfig peerInfoConfig : peerList){
            this.peerInfoConfigMap.put(peerInfoConfig.getPeerID(), peerInfoConfig);
        }

        while (connections.size() < peerIndex) {
            Thread.sleep(1000);
        }

        System.out.println("CONNECTIONS from " + peerID + " are " + connections);
        // Timer methods that run on the unchoking and optimistically unchoking intervals utilizing TimerTask
        getPreferredPeers();
        getOptimisticallyUnchokedPeer();
    }

    private void getPreferredPeers() {
        Timer getPreferredPeersTimer = new Timer();
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
                            Integer newPreferredNeighbor;
                            int randomIndex = new Random().nextInt(interested.size());
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

            }
        }, 0, 1000 * commonConfig.getUnchokingInterval());
    }

    private void getOptimisticallyUnchokedPeer() {
        Timer getOptimisticallyUnchokedPeerTimer = new Timer();
        getOptimisticallyUnchokedPeerTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // in selecting the next Optimistically Unchocked, they have to
                    // - not be already unchoked (aka preferred neighbor or current OptimiUnchocked
                    // - be interested
                Integer newOptimisticallyUnchokedNeighbor;
                if (interested.size() > 0) {
                    while (true) {
                        int randomIndex = random.nextInt(interested.size());
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
                        System.out.println(optimisticallyUnchokedNeighbor);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 0, 1000 * commonConfig.getOptimisticUnchokingInterval());
    }

    public ClientConnection connectionForPeerID(int peerID) {
        if(peerID == -1){
            return null;
        }
        if (!connections.containsKey(peerID)){
            startClientConnection(peerInfoConfigMap.get(peerID));
        }
        return connections.get(peerID);
    }

    // Starts server in order to receive messages
    private void startServer(int serverPort) {
        ServerConnection serverConnection = new ServerConnection(messageDispatcher);
        new Thread(() -> {
            try {
                serverConnection.openPort(serverPort);
            } catch (Exception e1) {
                e1.printStackTrace();
            } finally{
                try {
                    serverConnection.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }).start();
    }

    // Starts connection to another client in order to send the messages
    private void startClientConnection(PeerInfoConfig peerInfo) {
        ClientConnection clientConnection = new ClientConnection(peerID, messageDispatcher);
        connections.put(peerInfo.getPeerID(), clientConnection);
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
        if (bitField.isEmpty()) {
            return;
        }
        ClientConnection connection = connectionForPeerID(message.getPeerID());
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
        interested.add(otherPeerID);
        return null;
    }

    public ActualMessage serverResponseForUninterested(ActualMessage message, int otherPeerID) {
        logger.logReceivedNotInterestedMessage(peerID, otherPeerID);
        interested.remove(otherPeerID);
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
        logger.logPieceDownloaded(peerID, otherPeerID, pieceIndex, numPieces);

        // Update all peers that current peer has new piece
        for (Integer peerID : connections.keySet()) {
            ActualMessage haveMessage = MessageFactory.haveMessage(pieceIndex);
            try {
                connections.get(peerID).sendActualMessage(haveMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // If the file is completed, save the FileHandler pieces to disk and send out NOT_INTERESTED
        if (fileHandler.hasAllPieces()) {
            fileHandler.aggregateAllPieces();
            try {
                logger.logCompleteFileDownloaded(peerID);
                for (Integer peerID : connections.keySet()) {
                    connections.get(peerID).sendActualMessage(MessageFactory.notInterestedMessage());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        // Record that we got the piece from the specific peer (for choosing who our best friend is)
        peerDownloadRates.put(otherPeerID, peerDownloadRates.get(otherPeerID) + 1);

        // Now that piece was recieved, we request another piece from the server
        return requestPieceFrom(otherPeerID);
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

        // Set the current peer to know that the other peer has this given bit
        BitSet peerBitSet = otherPeerBitfields.get(otherPeerID);
        peerBitSet.set(pieceIndex);
        otherPeerBitfields.put(otherPeerID, peerBitSet);

        // If we already have this piece, no need to do anything
        if (bitField.get(pieceIndex)) {
            return null;
        }

        return MessageFactory.interestedMessage();
    }
}
