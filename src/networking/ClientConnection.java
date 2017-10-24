package networking;

import configs.PeerInfoConfig;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientConnection {
    private int peerID;
    private int serverPeerID;
    Socket requestSocket;
    DataOutputStream out;
    DataInputStream in;

    public ClientConnection(int peerID){
        this.peerID = peerID;
    }

    public void startConnection(PeerInfoConfig peerInfo) throws Exception {
        serverPeerID = peerInfo.getPeerID();
        requestSocket = new Socket(peerInfo.getHostName(), peerInfo.getListeningPort());

        out = new DataOutputStream(requestSocket.getOutputStream());
        out.flush();
        in = new DataInputStream(requestSocket.getInputStream());

        while (true) {
            int length = in.readInt();
            byte[] response = new byte[length];
            in.readFully(response);
            System.out.println(response);
        }
    }

    public void closeConnection() throws Exception {
        in.close();
        out.close();
        requestSocket.close();
    }
}
