import java.util.Map;

public class CommonConfig {
    private int numberOfPreferredNeighbors;
    private int unchokingInterval;
    private int optimisticUnchokingInterval;
    private String fileName;
    private int fileSize;
    private int pieceSize;

    public CommonConfig(int numberOfPreferredNeighbors, int unchokingInterval, int optimisticUnchokingInterval,
                        String fileName, int fileSize, int pieceSize) {
        this.numberOfPreferredNeighbors = numberOfPreferredNeighbors;
        this.unchokingInterval = unchokingInterval;
        this.optimisticUnchokingInterval = optimisticUnchokingInterval;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;
    }

    public static CommonConfig createConfigFromFile(String fileDir) throws Exception {
        CommonConfig commonConfig = null;
        try {
            ConfigParser parser = new ConfigParser();
            Map<String, String> attributes = parser.parseConfig(fileDir);

            //Set to default values
            int numberOfPreferredNeighbors = -1;
            int unchokingInterval = -1;
            int optimisticUnchokingInterval = -1;
            String fileName = "";
            int fileSize = -1;
            int pieceSize = -1;


            for (String key : attributes.keySet()) {
                switch (key) {
                    case "NumberOfPreferredNeighbors":
                        numberOfPreferredNeighbors = Integer.parseInt(attributes.get(key));
                        break;
                    case "UnchokingInterval":
                        unchokingInterval = Integer.parseInt(attributes.get(key));
                        break;
                    case "OptimisticUnchokingInterval":
                        optimisticUnchokingInterval = Integer.parseInt(attributes.get(key));
                        break;
                    case "FileName":
                        fileName = attributes.get(key);
                        break;
                    case "FileSize":
                        fileSize = Integer.parseInt(attributes.get(key));
                        break;
                    case "PieceSize":
                        pieceSize = Integer.parseInt(attributes.get(key));
                        break;
                }
            }

            commonConfig = new CommonConfig(numberOfPreferredNeighbors, unchokingInterval, optimisticUnchokingInterval,
                    fileName, fileSize, pieceSize);
        } catch (Exception e){
            e.printStackTrace();
        }

        return commonConfig;
    }

    public int getNumberOfPreferredNeighbors() {
        return numberOfPreferredNeighbors;
    }

    public int getUnchokingInterval() {
        return unchokingInterval;
    }

    public int getOptimisticUnchokingInterval() {
        return optimisticUnchokingInterval;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getPieceSize() {
        return pieceSize;
    }
}
