import configs.CommonConfig;
import configs.PeerInfoConfig;
import messages.HandshakeMessage;
import networking.ClientConnection;
import networking.ServerConnection;
import messages.ClientMessageHandler;
import messages.Message;
import messages.ServerMessageHandler;

import java.util.*;

public class Peer implements ClientMessageHandler, ServerMessageHandler{
    private int peerID;
    private BitSet bitField;
    private int numPieces;
    private Map<Integer, ClientConnection> connections;
    private CommonConfig commonConfig;
    // TODO: Add actual file data as a wrapper class

    public Peer(int peerID, CommonConfig commonConfig){
        this.peerID = peerID;
        this.commonConfig = commonConfig;

        int fileSize = commonConfig.getFileSize();
        int pieceSize = commonConfig.getPieceSize();

        // Sets number of pieces to be either fileSize / pieceSize if division evenly, otherwise add one
        this.numPieces = fileSize / pieceSize + (fileSize % pieceSize != 0 ? 1 : 0);
        this.bitField = new BitSet(numPieces);

        this.connections = new HashMap<>();
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

        // Start TCP connections with the peers in the list before the given one to startServer transferring messages/data
        for (int i = 0; i < peerIndex; i++){
            startClientConnection(peerList.get(i));
        }

        // TODO: Create timers for optimistically choking/unchoking peers
    }

    private void startServerConnection(int port){
        ServerConnection serverConnection = new ServerConnection(this);
        new Thread(() -> {
            try {
                serverConnection.startServer(port);
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

    private void startClientConnection(PeerInfoConfig serverInfo){
        ClientConnection clientConnection = new ClientConnection(peerID, this);

        // Add the connection to the other peer to the current peer's list of connections
        connections.put(serverInfo.getPeerID(), clientConnection);
        new Thread(() -> {
            try {
                clientConnection.startConnection(serverInfo);
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
    public Message serverResponseForHandshake(HandshakeMessage message, int clientPeerID) {
        return null;
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
    public Message clientResponseForHandshake(HandshakeMessage message, int serverPeerID) {
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
