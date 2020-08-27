
import java.util.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Labels {
    
    public static List<Integer> getLabels(String path) {
        List<Integer> labels = new ArrayList<Integer>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("#sig#")) {
                    labels.add(1);
                } else {
                    labels.add(0);
                }
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return labels;
    }
    
    public static List<Integer> getLabels(File file) {
        List<Integer> labels = new ArrayList<Integer>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                if (line.startsWith("#sig#")) {
                    labels.add(1);
                } else {
                    labels.add(0);
                }
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return labels;
    }

}
