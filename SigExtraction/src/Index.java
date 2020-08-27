import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import libsvm.svm;
import libsvm.svm_model;

public class Index {
    static String prov;
    static String modelPath = "classifier.model";
    static String serverPath = "http://localhost:8080/api/";

    public static DataCollected extractDataFromMail(String text) throws IOException {
        // Initializing variables
        DataCollected dataCollected = new DataCollected();
        List<String> tels = new ArrayList<String>();
        List<String> mails = new ArrayList<String>();
        List<List<String>> names = new ArrayList<List<String>>();
        List<Integer[]> features = new ArrayList<Integer[]>();

        // Processing text to features and formating it
        features = Features.getFeaturesFromString(text);
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

        // Use libsvm to predict which line is part of the signature
        BufferedReader input = new BufferedReader(new StringReader(prov));
        svm_model model = svm.svm_load_model(modelPath);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedOutputStream bufferOut = new BufferedOutputStream(out);
        DataOutputStream output = new DataOutputStream(bufferOut);
        svm_predict.predict(input, output, model, 0);
        bufferOut.flush();
        String labels = new String(out.toByteArray());

        // If line is part of the signature, we search for useful data
        BufferedReader reader = new BufferedReader(new StringReader(text));
        String line = reader.readLine();
        String lines[] = labels.split("\\r?\\n");
        int l = lines.length;
        int i = 0;
        while (line != null && i < l) {
            if (lines[i].startsWith("1")) {
                // We store data found in signature
                String temp = EntityRecognition.telExtraction(line);
                if (!temp.isBlank()) {
                    tels.add(temp);
                }
                temp = EntityRecognition.mailExtraction(line);
                if (!temp.isBlank()) {
                    mails.add(temp);
                }
                names.add(EntityRecognition.nerPersonExtraction(line));
            }
            i++;
            line = reader.readLine();
        }
        if (tels.size() >= 1) {
            dataCollected.setTel(tels.get(0));
        }
        return dataCollected;
    }

    public static String getExclusionList(String dest, String exp) throws IOException {
        String request = serverPath + "contacts/exclusionList/" + dest + "/" + exp;
        URL url = new URL(request);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("charset", "utf-8");
        connection.connect();

        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder();

        String line;
        while ((line = rd.readLine()) != null) {
            response.append(line);
        }
        rd.close();

        JsonObject body = Json.createReader(new StringReader(response.toString())).readObject();
        String prov = body.get("data").toString();
        prov = prov.substring(1, prov.length() - 1);
        if (!prov.isEmpty()) {
            String res = "";
            JsonObject data = Json.createReader(new StringReader(prov)).readObject();
            res += data.get("phones").toString() + " ";
            res += data.get("jobs").toString() + " ";
            return res;
        } else
            return "";
    }

    public static void setPrediction(String dest, String exp, String phone, String job) throws IOException {
        String request = serverPath + "contacts/predictionList/" + dest + "/" + exp;
        URL url = new URL(request);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("PUT");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("charset", "utf-8");
        connection.connect();
        OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
        out.write("{\"phone\": \"" + phone + "\", \"job\": \"" + job + "\"}");
        out.close();

        InputStream is = connection.getInputStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder();

        String line;
        while ((line = rd.readLine()) != null) {
            response.append(line);
        }
        rd.close();

        JsonObject body = Json.createReader(new StringReader(response.toString())).readObject();

        System.out.println(body);
    }

    public static void collect(String mail) throws IOException {
        JsonObject body = Json.createReader(new StringReader(mail)).readObject();
        String exp = EntityRecognition.mailExtraction(body.get("sender").toString());
        String dest = EntityRecognition.mailExtraction(body.get("recipients").toString());
        String[] dests = dest.split(" ");
        String text = body.get("text").toString();
        text = text.replaceAll("\\\\n", System.getProperty("line.separator"));
        DataCollected dataCollected = extractDataFromMail(text);
        String tel = dataCollected.getTel();
        // String job = dataCollected.getJob();

        for (String d : dests) {
            String excludedPhones = getExclusionList(d, exp);
            if (!excludedPhones.contains(tel)) {
                setPrediction(d, exp, tel, "");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        String fakeMail = "{\"sender\":\"test@email.com\",\"recipients\":\"[Nom Prenom <jd@email.com>, User2 <tobis@james.org>]\"," + "\"text\":\"This occurs when your H drive is not mapped.  To fix the mapping, follow the instructions at:\\n" + "\\n"
                + "http://172.17.172.62/rt/tips/mapHdrive.html\\n" + "\\n" + "Hope this helps!\\n" + "\\n" + "#sig#John Oh\\n" + "#sig#Enron North America        503.464.5066\\n" + "#sig#121 SW Salmon Street       503.701.1160 (cell)\\n" + "#sig#3WTC 0306          503.464.3740 (fax)\\n"
                + "#sig#Portland, OR 97204         John.Oh@Enron.com\"}";
        String mail;
        if (args.length == 0) {
            mail = fakeMail;
        }
        else {
            mail = args[0];
        }
        collect(mail);
    }

}
