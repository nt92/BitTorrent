import configs.CommonConfig;
import configs.PeerInfoConfig;
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
    private Set<Integer> connections;
    private CommonConfig commonConfig;

    public Peer(int peerID, CommonConfig commonConfig){
        this.peerID = peerID;
        this.commonConfig = commonConfig;

        int fileSize = commonConfig.getFileSize();
        int pieceSize = commonConfig.getPieceSize();
        this.numPieces = fileSize / pieceSize + (fileSize % pieceSize != 0 ? 1 : 0);
        this.bitField = new BitSet(numPieces);

        this.connections = new HashSet<>();
    }

    public void start(List<PeerInfoConfig> peerList) throws Exception{
        int peerIndex = 0;
        while (peerIndex < peerList.size() && peerList.get(peerIndex).getPeerID() != peerID){
            peerIndex++;
        }

        if (peerIndex == peerList.size()){
            throw new Exception("PeerID: " + peerID + " not found in peerList.");
        }

        PeerInfoConfig myInfo = peerList.get(peerIndex);

        startServerConnection(myInfo.getListeningPort());

        for (int i = 0; i < peerIndex; i++){
            startClientConnection(peerList.get(i));
        }
    }

    private void startServerConnection(int port){
        ServerConnection serverConnection = new ServerConnection(this);
        new Thread(() -> {
            try {
                serverConnection.start(port);
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

        connections.add(serverInfo.getPeerID());
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

    // ClientMessageHandler Methods

    @Override
    public Message clientResponseForHandshake(Message message, int serverPeerID) {
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

    // ServerMessageHandler Methods
    @Override
    public Message serverResponseForHandshake(Message message, int clientPeerID) {
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
}

