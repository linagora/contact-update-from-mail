

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

public class NumberLines {

    public static int getNumberOfLines(String path) {
        int n = 0;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            while (line != null) {
                n++;
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return n;
    }
    
    public static int getNumberOfLines(File file) {
        int n = 0;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                n++;
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return n;
    }
    
    public static int getNumberOfLinesFromString(String mail) {
        int n = 0;
        BufferedReader reader;
        try {
            reader = new BufferedReader(new StringReader(mail));
            String line = reader.readLine();
            while (line != null) {
                n++;
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return n;
    }


}
