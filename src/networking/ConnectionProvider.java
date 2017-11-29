package networking;

/**
 * Created by gonzalonunez on 11/28/17.
 */
public interface ConnectionProvider {
    ClientConnection connectionForPeerID(int peerID);
}
