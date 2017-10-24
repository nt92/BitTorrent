import configs.CommonConfig;
import configs.PeerInfoConfig;
import networking.ClientConnection;
import networking.ServerConnection;
import networking.messages.ClientMessageHandler;
import networking.messages.Message;
import networking.messages.ServerMessageHandler;

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

    @Override
    public Message responseForServerHandshake(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message responseForChoke(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message responseForUnchoke(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message responseForInterested(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message responseForUninterested(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message responseForHave(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message responseForClientBitfield(Message message, int clientPeerID) {
        return null;
    }

    @Override
    public Message responseForServerBitfield(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message responseForRequest(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message responseForPiece(Message message, int serverPeerID) {
        return null;
    }

    @Override
    public Message responseForClientHandshake(Message message, int clientPeerID) {
        return null;
    }
}

