import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;

/**
 * Created by gonzalonunez on 11/27/17.
 */
public class FileHandlerTests {

    FileHandler fileHandler;

    @Before
    public void setUpFileHandler() throws Exception {
        CommonConfig commonConfig = CommonConfig.createConfigFromFile(Constants.COMMON_CONFIG_FILENAME);
        fileHandler = new FileHandler(1001, commonConfig);
    }

    @Test
    public void testChunksAndAggregates() throws Exception {
        Path path = Paths.get("src/files/tiny.txt");
        byte[] expected = Files.readAllBytes(path);
        Assert.assertTrue(fileHandler.chunkFile());
        Assert.assertTrue(fileHandler.aggregateAllPieces());
        byte[] actual = Files.readAllBytes(path);
        Assert.assertArrayEquals(expected, actual);
    }

    @Test
    public void testHasAllPieces() throws Exception {
        Assert.assertTrue(fileHandler.chunkFile());
        Assert.assertTrue(fileHandler.hasAllPieces());
        for (int i = 0; i < fileHandler.getPiecesCount(); i++) {
            Assert.assertTrue(fileHandler.hasPiece(i));
        }
    }

    @Test
    public void testSavingPiece() throws Exception {
        fileHandler.setPiece(0, new byte[]{ 1, 2, 3, 4 });
        Assert.assertTrue(fileHandler.hasPiece(0));
        Assert.assertFalse(fileHandler.hasPiece(1));
        Assert.assertFalse(fileHandler.hasAllPieces());
    }

    @Test
    public void testOneMissingPiece() {
        BitSet ours = new BitSet(); // 11010
        ours.set(0, true);
        ours.set(1, true);
        ours.set(2, false);
        ours.set(3, true);
        ours.set(4, false);
        BitSet theirs = new BitSet(); // 01001
        theirs.set(0, false);
        theirs.set(1, true);
        theirs.set(2, false);
        theirs.set(3, false);
        theirs.set(4, true);
        ArrayList<Integer> expected = new ArrayList(); // [4], because theirs - ours = 00001
        expected.add(4);
        Assert.assertArrayEquals(expected.toArray(), fileHandler.getMissingPieces(ours, theirs).toArray());
    }

    @Test
    public void testTwoMissingPieces() {
        BitSet ours = new BitSet(); // 01010
        ours.set(0, false);
        ours.set(1, true);
        ours.set(2, false);
        ours.set(3, true);
        ours.set(4, false);
        BitSet theirs = new BitSet(); // 11100
        theirs.set(0, true);
        theirs.set(1, true);
        theirs.set(2, true);
        theirs.set(3, false);
        theirs.set(4, false);
        ArrayList<Integer> expected = new ArrayList(); // [0, 2], because theirs - ours = 10100
        expected.add(0);
        expected.add(2);
        Assert.assertArrayEquals(expected.toArray(), fileHandler.getMissingPieces(ours, theirs).toArray());
    }
}
