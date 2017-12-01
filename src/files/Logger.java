package files;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by gonzalonunez on 11/15/17.
 */
public class Logger {

    private int peerID;
    private File file;
    private FileOutputStream outputStream;

    public Logger(int peerID) throws Exception {
        this.peerID = peerID;
        this.file = new File(System.getProperty("user.home") + "/project/log_peer_" + this.peerID + ".log");
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        if(file.exists()){
            file.delete();
        }
        file.createNewFile();
        outputStream = new FileOutputStream(file);
    }

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private void append(String string) {
        try {
            String finalString = dateFormat.format(new Date()).concat(": " + string);
            outputStream.write(finalString.getBytes("ASCII"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logConnectionMade(int peerID, int otherPeerID) {
        append("Peer " + peerID + " makes a connection to Peer " + otherPeerID + "\n");
    }

    public void logConnectionReceived(int peerID, int otherPeerID) {
        append("Peer " + peerID + " is connected from Peer " + otherPeerID + "\n");
    }

    public void logChangedPreferredNeighbors(int peerID, Object[] neighbors) {
        StringBuilder builder = new StringBuilder();
        String sep = "";
        for (Object id : neighbors) {
            builder.append(sep);
            builder.append(id);
            sep = ",";
        }
        append("Peer " + peerID + " has the preferred neighbors " + builder.toString() + "\n");
    }

    public void logChangeOptimisticallyUnchokedNeighbor(int peerID, int otherID) {
        append("Peer " + peerID + " has the optimistically unchoked neighbor " + otherID + "\n");
    }

    public void logUnchoking(int peerID, Integer otherID) {
        append("Peer " + peerID + " is unchoked by " + otherID + "\n");
    }

    public void logChoking(int peerID, int otherID) {
        append("Peer " + peerID + " is choked by " + otherID);
    }

    public void logReceivedHaveMessage(int peerID, int otherID, int pieceIndex) {
        append("Peer " + peerID + " received the 'have' message from " + otherID + " for the piece " + pieceIndex + "\n");
    }

    public void logReceivedInterestedMessage(int peerID, int otherID) {
        append("Peer " + peerID + " received the 'interested' message from " + otherID + "\n");
    }

    public void logReceivedNotInterestedMessage(int peerID, int otherID) {
        append("Peer " + peerID + " received the 'not interested' message from " + otherID + "\n");
    }

    public void logPieceDownloaded(int peerID, int otherID, int pieceIndex, int totalPieces) {
        append("Peer " + peerID + " has downloaded the piece " + pieceIndex + " from " + otherID + ". Now the number of pieces it has it " + totalPieces + "."  + "\n");
    }

    public void logCompleteFileDownloaded(int peerID) {
        append("Peer " + peerID + " has downloaded the complete file."  + "\n");
    }
}
