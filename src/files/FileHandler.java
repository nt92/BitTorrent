package files;

import configs.CommonConfig;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class FileHandler {
    private int peerID;
    private String peerDirectory;
    private String fileName;
    private int fileSize;
    private int pieceSize;
    private int piecesCount;
    private Random random;
    private HashMap<Integer, byte[]> pieces = new HashMap<>();

    public FileHandler(int peerID, CommonConfig config) {
        this.peerID = peerID;
        this.peerDirectory = "~/project/peer_" + this.peerID + "/";
        this.fileName = config.getFileName();
        this.fileSize = config.getFileSize();
        this.pieceSize = config.getPieceSize();
        this.piecesCount = fileSize / pieceSize + (fileSize % pieceSize != 0 ? 1 : 0);
        this.random = new Random();
    }

    public int getPiecesCount() {
        return piecesCount;
    }

    public boolean hasPiece(int pieceIndex) {
        byte[] bytes = pieces.get(pieceIndex);
        return bytes != null && bytes.length != 0;
    }

    public boolean hasAllPieces() {
        for(int i = 0; i < this.piecesCount; i++){
            if (!hasPiece(i)) {
                return false;
            }
        }
        return true;
    }

    // Aggregates all existing chunks in pieces HashMap and saves to one file
    public boolean aggregateAllPieces() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        if (hasAllPieces()) {
            for (int i = 0; i < this.piecesCount; i++) {
                try {
                    if (hasPiece(i)) {
                        byte[] data = getPieceByteData(i);
                        byteStream.write(data);
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    return false;
                }
            }
            byte[] completeData = byteStream.toByteArray();
            return saveBytesToFile(completeData);
        }
        return false;
    }

    // Chunks file and stores chunks in pieces HashMap. Returns true if file was chunked successfully.
    public boolean chunkFile() {
        Path path = Paths.get(this.fileName);
        try {
            List<FilePiece> filePieces = new ArrayList<FilePiece>();
            byte[] data = Files.readAllBytes(path);
            int i = 0;
            for( ; i + this.pieceSize <= this.fileSize; i += this.pieceSize) {
                filePieces.add(new FilePiece(i/this.pieceSize, Arrays.copyOfRange(data, i, i + this.pieceSize)));
            }
            filePieces.add(new FilePiece(i/this.pieceSize, Arrays.copyOfRange(data, i, this.fileSize - 1)));
            for (FilePiece piece : filePieces) {
                pieces.put(piece.pieceIndex, piece.data);
            }
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Returns the byte data for a piece given the index
    public byte[] getPieceByteData(int pieceIndex) {
        return pieces.get(pieceIndex);
    }

    // Adds a piece to the file's data given pieceData and the index
    public void setPiece(int pieceIndex, byte[] pieceData) {
        pieces.put(pieceIndex, pieceData);
    }

    // Returns a random piece index from the set of missing indices given two BitSets
    public int getRandomMissingPiece(BitSet myBitSet, BitSet otherBitSet) {
        ArrayList<Integer> missingPieces = getMissingPieces(myBitSet, otherBitSet);
        if (missingPieces.isEmpty()) {
            return -1;
        }
        int index = random.nextInt(missingPieces.size());
        return missingPieces.get(index);
    }


    // Returns indices of missing pieces given two BitSets
    public ArrayList<Integer> getMissingPieces(BitSet myBitSet, BitSet otherBitSet) {
        BitSet missingPieces = otherBitSet;
        missingPieces.andNot(myBitSet);
        ArrayList<Integer> missingPieceIndices = new ArrayList();
        for (int i = missingPieces.nextSetBit(0); i != -1; i = missingPieces.nextSetBit(i + 1)) {
            missingPieceIndices.add(i);
        }
        return missingPieceIndices;
    }

    // Saves data to file in peerDirectory + "/" + fileName. Returns true if save was successful.
    private boolean saveBytesToFile(byte[] data) {
        FileOutputStream fileOut = null;
        try {
            File file = new File(peerDirectory + "/" + fileName);
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
            return false;
        } finally {
            try {
                if(fileOut != null){
                    fileOut.close();
                }
                return true;
            } catch(Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }
}
