package files;

import java.io.FileOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import configs.CommonConfig;

public class FileChunker{
    private int peerID;
    private CommonConfig commonConfig;
    private String fileName;
    private int fileSize;
    private int pieceSize;

    public FileChunker(int peerID, CommonConfig commonConfig){
        this.peerID = peerID;
        this.commonConfig = commonConfig;
        this.fileName = commonConfig.getFileName();
        this.fileSize = commonConfig.getFileSize();
        this.pieceSize = commonConfig.getPieceSize();
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

            FileHandler fh = new FileHandler(this.peerID);
            for(FilePiece piece : filePieces){
                fh.savePiece(piece);
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