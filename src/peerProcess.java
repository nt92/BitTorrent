import configs.CommonConfig;
import configs.PeerInfoConfig;

import java.util.List;

import static java.lang.System.exit;

public class peerProcess {

    private static final String COMMON_CONFIG_FILENAME = "configs/Common.cfg";
    private static final String PEER_INFO_FILENAME = "configs/PeerInfo.cfg";

    public static void main(String[] args) throws Exception {
        //Format of the process MUST have the peerID as an argument
        if (args.length != 1){
            System.out.println("Invalid Argument, must have peerID as the main argument");
            exit(1);
        }

        CommonConfig commonConfig = CommonConfig.createConfigFromFile(COMMON_CONFIG_FILENAME);
        List<PeerInfoConfig> peerList = PeerInfoConfig.createPeerListFromFile(PEER_INFO_FILENAME);


    }
}
