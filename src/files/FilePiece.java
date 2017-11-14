package files;

public class FilePiece{
    public int pieceIndex;
    public byte[] data;

    public FilePiece(int pieceIndex, byte[] data){
        this.pieceIndex = pieceIndex;
        this.data = data;
    }
}
