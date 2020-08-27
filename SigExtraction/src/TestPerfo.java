import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import libsvm.svm;
import libsvm.svm_model;

public class TestPerfo {
    static String prov;
    static String modelPath = "../libsvm-3.24/C7.model";
    
    public static void writeSig(File file, String labels) throws IOException {
        FileWriter myWriter = new FileWriter(new File("testPerfC7", "testSig" + file.getName()));
        myWriter.write("");
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        String lines[] = labels.split("\\r?\\n");
        int l = lines.length;
        int i = 0;
        while (line != null && i < l) {
            if (lines[i].startsWith("1")) {
                myWriter.append(line + "\n");
            }
            i++;
            line = reader.readLine();
        }
        reader.close();
        myWriter.close();
    }

    public static void testOnFolder(File folder) throws IOException {

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                testOnFolder(fileEntry);
            } else {
                List<Integer[]> features = new ArrayList<Integer[]>();
                features = Features.getFeatures(fileEntry);
                prov = "";

                features.forEach(array -> {
                    prov += "2";
                    for (int j = 0; j < array.length; j++) {
                        int val = array[j];
                        if (val == 1) {
                            prov += " " + j + ":" + array[j];
                        }
                    }
                    prov += "\n";
                });
                
                BufferedReader input = new BufferedReader(new StringReader(prov));
                svm_model model = svm.svm_load_model(modelPath);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                BufferedOutputStream bufferOut = new BufferedOutputStream(out);
                DataOutputStream output = new DataOutputStream(bufferOut);
                svm_predict.predict(input, output, model, 0);
                bufferOut.flush();
                String labels = new String(out.toByteArray());
                writeSig(fileEntry, labels);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Start");
        long startTime = System.nanoTime();
        File folder = new File("../../Documents/Extraction_sig_mail/Datasets/All");

        testOnFolder(folder);

        System.out.println("Done");
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;
        System.out.println("Execution time in nanoseconds  : " + timeElapsed);

        System.out.println("Execution time in milliseconds : " + timeElapsed / 1000000);
    }

}
