package files;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by Takashi on 10/24/17.
 */
public class FileHandler {
    private int peerID;
    private String peerDirectory;

    public FileHandler(int peerID){
        this.peerID = peerID;
        this.peerDirectory = "project/peer_" + this.peerID + "/";
    }

    public void savePiece(FilePiece piece){

        FileOutputStream fileOut = null;

        try {
            File file = new File(this.peerDirectory + piece.pieceIndex);
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            if(!file.exists()){
                file.createNewFile();
            }
            fileOut = new FileOutputStream(file);
            fileOut.write(piece.data);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(fileOut != null){
                    fileOut.close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean hasPiece(int pieceIndex){
        File file = new File(this.peerDirectory + pieceIndex);
        if(!file.getParentFile().exists()){
            return false;
        }
        if(file.exists()){
            return true;
        }
        return false;
    }
//
//    public bool hasFilePiece(int pieceIndex){
//
//    }
}
