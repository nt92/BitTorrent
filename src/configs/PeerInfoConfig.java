package configs;

import files.ConfigParser;

import java.util.ArrayList;
import java.util.List;

public class PeerInfoConfig {
    private int peerID;
    private String hostName;
    private int listeningPort;
    private boolean hasFile;

    public PeerInfoConfig(int peerID, String hostName, int listeningPort, boolean hasFile){
        this.peerID = peerID;
        this.hostName = hostName;
        this.listeningPort = listeningPort;
        this.hasFile = hasFile;
    }

    public static List<PeerInfoConfig> createPeerListFromFile(String fileDir) throws Exception{
        List<PeerInfoConfig> peerList = new ArrayList<>();
        try {
            ConfigParser parser = new ConfigParser();
            peerList = parser.parsePeerInfo(fileDir);
        } catch (Exception e){
            e.printStackTrace();
        }

        return peerList;
    }

    public int getPeerID() {
        return peerID;
    }

    public String getHostName() {
        return hostName;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public boolean getHasFile() {
        return hasFile;
    }
}
