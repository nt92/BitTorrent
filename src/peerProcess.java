import configs.CommonConfig;
import configs.PeerInfoConfig;
import files.FileHandler;

import java.util.List;

public class peerProcess {

    private static final String COMMON_CONFIG_FILENAME = "src/configs/Common.cfg";
    private static final String PEER_INFO_FILENAME = "src/configs/PeerInfo.cfg";

    public static void main(String[] args) throws Exception {
        //Format of the process MUST have the peerID as an argument
        if (args.length != 1){
            System.out.println("Invalid Argument, must have peerID as the main argument");
        }

        CommonConfig commonConfig = CommonConfig.createConfigFromFile(COMMON_CONFIG_FILENAME);
        List<PeerInfoConfig> peerList = PeerInfoConfig.createPeerListFromFile(PEER_INFO_FILENAME);

        int peerID = Integer.parseInt(args[0]);
        Peer peer = new Peer(peerID, commonConfig);

        try{
            peer.start(peerList);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
