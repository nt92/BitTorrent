package files;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import configs.CommonConfig;

public class FileChunker{
    private CommonConfig commonConfig;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private List<FilePiece> filePieces;

    public FileChunker(CommonConfig commonConfig){
        this.commonConfig = commonConfig;
        this.fileName = commonConfig.getFileName();
        this.fileSize = commonConfig.getFileSize();
        this.pieceSize = commonConfig.getPieceSize();
        this.chunkFile();
    }

    private void chunkFile(){
        Path path = Paths.get("src/files/SelfPortrait.gif");
        try {
            this.filePieces = new ArrayList<FilePiece>();
            byte[] data = Files.readAllBytes(path);
            int i = 0;

            for( ; i + this.pieceSize <= this.fileSize; i += this.pieceSize){
                this.filePieces.add(new FilePiece(i, Arrays.copyOfRange(data, i, i + this.pieceSize)));
            }
            this.filePieces.add(new FilePiece(i, Arrays.copyOfRange(data, i, this.fileSize - 1)));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public List<FilePiece> getFilePieces(){
        return this.filePieces;
    }
}

class FilePiece{
    private int pieceIndex;
    public byte[] data;

    public FilePiece(int pieceIndex, byte[] data){
        this.pieceIndex = pieceIndex;
        this.data = data;
    }
}