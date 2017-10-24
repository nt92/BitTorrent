package files;

import configs.CommonConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Takashi on 10/24/17.
 */
public class FileHandler {
    private int peerID;
    private String peerDirectory;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private int piecesCount;

    public FileHandler(int peerID, CommonConfig config){
        this.peerID = peerID;
        this.peerDirectory = "project/peer_" + this.peerID + "/";
        this.fileName = config.getFileName();
        this.fileSize = config.getFileSize();
        this.pieceSize = config.getPieceSize();
        this.piecesCount = fileSize / pieceSize + (fileSize % pieceSize != 0 ? 1 : 0);
    }

    public void savePiece(FilePiece piece){
        saveByteArrayTo(Integer.toString(piece.pieceIndex), piece.data);
    }

    public void saveByteArrayTo(String fileName, byte[] data){
        FileOutputStream fileOut = null;

        try {
            File file = new File(this.peerDirectory + fileName);
            if(!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }
            if(!file.exists()){
                file.createNewFile();
            }
            fileOut = new FileOutputStream(file);
            fileOut.write(data);
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

    public boolean hasAllPieces(){
        for(int i = 0; i < this.piecesCount; i++){
            if(!hasPiece(i)){
                return false;
            }
        }
        return true;
    }

    public void aggregateAllPieces(){
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

        if(hasAllPieces()){
            for(int i = 0; i < this.piecesCount; i++){
                Path path = Paths.get(this.peerDirectory + i);
                try {
                    byte[] data = Files.readAllBytes(path);
                    byteStream.write(data);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }

            byte[] completeData = byteStream.toByteArray();
            saveByteArrayTo(this.fileName, completeData);

        }
    }

    public void chunkFile(){
        Path path = Paths.get("src/files/SelfPortrait.gif");
        try {
            List<FilePiece> filePieces = new ArrayList<FilePiece>();
            byte[] data = Files.readAllBytes(path);
            int i = 0;

            for( ; i + this.pieceSize <= this.fileSize; i += this.pieceSize){
                filePieces.add(new FilePiece(i/this.pieceSize, Arrays.copyOfRange(data, i, i + this.pieceSize)));
            }
            filePieces.add(new FilePiece(i/this.pieceSize, Arrays.copyOfRange(data, i, this.fileSize - 1)));

            for(FilePiece piece : filePieces){
                this.savePiece(piece);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}

class FilePiece{
    public int pieceIndex;
    public byte[] data;

    public FilePiece(int pieceIndex, byte[] data){
        this.pieceIndex = pieceIndex;
        this.data = data;
    }
}
