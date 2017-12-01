import configs.CommonConfig;
import configs.PeerInfoConfig;
import util.Constants;

import java.util.List;

public class PeerProcess {
    public static void main(String[] args) throws Exception {
        //Format of the process MUST have the peerID as an argument
        if (args.length != 1){
            System.out.println("Invalid Argument, must have peerID as the main argument");
        }
        CommonConfig commonConfig = CommonConfig.createConfigFromFile(Constants.COMMON_CONFIG_FILENAME);
        List<PeerInfoConfig> peerList = PeerInfoConfig.createPeerListFromFile(Constants.PEER_INFO_FILENAME);
        int peerID = Integer.parseInt(args[0]);
        Peer peer = new Peer(peerID, commonConfig);
        try {
            peer.start(peerList);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
