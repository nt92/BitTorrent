package files;

import configs.PeerInfoConfig;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigParser {
    public Map<String, String> parseConfig(String fileName) throws Exception{
        Map<String, String> result = new HashMap<>();
        BufferedReader input = new BufferedReader(new FileReader(fileName));

        String currentLine;
        while((currentLine = input.readLine()) != null){

            String[] line = currentLine.split(" ");

            //Sanity check for the input
            if (line.length != 2){
                throw new Exception("Invalid format, must have two values per line");
            }

            result.put(line[0], line[1]);
        }

        return result;
    }

    public List<PeerInfoConfig> parsePeerInfo(String fileName) throws Exception{
        List<PeerInfoConfig> result = new ArrayList<>();
        BufferedReader input = new BufferedReader(new FileReader(fileName));

        String currentLine;
        while((currentLine = input.readLine()) != null){

            String[] line = currentLine.split(" ");

            //Sanity check for input
            if (line.length != 4){
                throw new Exception("Invalid peer info, must have 4 attributes per peer");
            }

            result.add(new PeerInfoConfig(Integer.parseInt(line[0]), line[1], Integer.parseInt(line[2]), line[3].equals("1")));
        }

        return result;
    }
}
