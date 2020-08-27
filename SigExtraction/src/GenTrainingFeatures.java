
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GenTrainingFeatures {
    static int i;
    static int nbLinesTot = 0;

    public static void listFilesForFolder(File folder) throws IOException {
        System.out.println("Start");
        FileWriter myWriter = new FileWriter("featest" + ".txt");
        FileWriter myWriter2 = new FileWriter("textest" + ".txt");
        myWriter.write("");
        myWriter2.write("");
        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                listFilesForFolder(fileEntry);
            } else {
                // if (!fileEntry.getName().endsWith("sender")) {
                i = 0;
                List<Integer> labels;
                List<Integer[]> features = new ArrayList<Integer[]>();
                // String path = fileEntry.getAbsolutePath();
                labels = Labels.getLabels(fileEntry);
                features = Features.getFeatures(fileEntry);
                features.forEach(array -> {
                    try {
                        myWriter.append(labels.get(i).toString());
                        for (int j = 0; j < array.length; j++) {
                            int val = array[j];
                            if (val == 1) {
                                myWriter.append(" " + j + ":" + array[j]);
                            }
                        }
                        myWriter.append("\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        i++;
                    }
                });
                nbLinesTot += NumberLines.getNumberOfLines(fileEntry);
                System.out.println(nbLinesTot);
            }
            BufferedReader reader = new BufferedReader(new FileReader(fileEntry));
            String line = reader.readLine();
            while (line != null) {
                myWriter2.append(line + "\n");
                line = reader.readLine();
            }
            reader.close();
            // }
        }
        myWriter2.close();
        myWriter.close();
        System.out.println("Done");
    }

    public static void main(String[] args) throws IOException {
        // File folder = new
        // File("../../Documents/Extraction_sig_mail/Datasets/forge-master/dataset/P");
        // File folder = new
        // File("../../Documents/Extraction_sig_mail/Datasets/sigPlusReply");
        // File folder = new
        // File("../../Documents/Extraction_sig_mail/Datasets/data/body");
        File folder = new File("../../Documents/Extraction_sig_mail/Datasets/All");
        listFilesForFolder(folder);
    }

}
