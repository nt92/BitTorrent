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
        this.file = new File("~/project/log_peer_" + this.peerID + ".log");
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        if(!file.exists()){
            file.createNewFile();
        }
        outputStream = new FileOutputStream(file);
    }

    private static Logger sharedInstance;
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private static synchronized Logger getInstance(int peerID) throws Exception {
        if (sharedInstance == null) {
            sharedInstance = new Logger(peerID);
        }
        if (sharedInstance.peerID != peerID) {
            throw new Exception("ERROR: Tried to create a Logger with a difference peerID after already having created one.");
        }
        return sharedInstance;
    }

    private void append(String string) {
        try {
            String finalString = dateFormat.format(new Date()).concat(": " + string);
            outputStream.write(string.getBytes("ASCII"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void logConnectionMade(int peerID, int otherPeerID) throws Exception {
        Logger logger = getInstance(peerID);
        logger.append("Peer " + peerID + " makes a connection to Peer " + otherPeerID + "\n");
    }

    public static void logConnectionReceived(int peerID, int otherPeerID) throws Exception {
        Logger logger = getInstance(peerID);
        logger.append("Peer " + peerID + " is connected from Peer " + otherPeerID + "\n");
    }

    public static void logChangedPreferredNeighbors(int peerID, int[] neighbors) throws Exception {
        StringBuilder builder = new StringBuilder();
        String sep = "";
        for (int id : neighbors) {
            builder.append(sep);
            builder.append(id);
            sep = ",";
        }
        Logger logger = getInstance(peerID);
        logger.append("Peer " + peerID + " has the preferred neighbors " + builder.toString() + "\n");
    }

    public static void logChangeOptimisticallyUnchokedNeighbor(int peerID, int otherID) throws Exception {
        Logger logger = getInstance(peerID);
        logger.append("Peer " + peerID + " has the optimistically unchoked neighbor " + otherID + "\n");
    }

    public static void logUnchoking(int peerID, int otherID) throws Exception {
        Logger logger = getInstance(peerID);
        logger.append("Peer " + peerID + " is unchoked by " + otherID + "\n");
    }

    public static void logChoking(int peerID, int otherID) throws Exception {
        Logger logger = getInstance(peerID);
        logger.append("Peer " + peerID + " is choked by " + otherID);
    }

    public static void logReceivedHaveMessage(int peerID, int otherID, int pieceIndex) throws Exception {
        Logger logger = getInstance(peerID);
        logger.append("Peer " + peerID + " received the 'have' message from " + otherID + " for the piece " + pieceIndex + "\n");
    }

    public static void logReceivedInterestedMessage(int peerID, int otherID) throws Exception {
        Logger logger = getInstance(peerID);
        logger.append("Peer " + peerID + " received the 'interested' message from " + otherID + "\n");
    }

    public static void logPieceDownloaded(int peerID, int otherID, int pieceIndex, int totalPieces) throws Exception {
        Logger logger = getInstance(peerID);
        logger.append("Peer " + peerID + " has downloaded the piece " + pieceIndex + " from " + otherID + ". Now the number of pieces it has it " + totalPieces + "."  + "\n");
    }

    public static void logCompleteFileDownloaded(int peerID) throws Exception {
        Logger logger = getInstance(peerID);
        logger.append("Peer " + peerID + " has downloaded the complete file."  + "\n");
    }
}
