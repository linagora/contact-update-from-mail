
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

public class Features {

    // Read from the file found following path
    public static List<Integer[]> getFeatures(String path) {
        List<Integer[]> features = new ArrayList<Integer[]>();

        int dimLine = 37;
        int dim = dimLine * 3;
        features.add(new Integer[dim]);

        BufferedReader reader;
        try {
            int nbLines = NumberLines.getNumberOfLines(path);
            int i = 0;
            reader = new BufferedReader(new FileReader(path));
            String line = reader.readLine();
            while (line != null) {
                int j = 0;
                if (i < nbLines - 1) {
                    features.add(new Integer[dim]);
                }

                // Tel in line
                String tels = EntityRecognition.telExtraction(line);
                if (tels != "") {
                    features.get(i)[j++] = 1;
                } else {
                    features.get(i)[j++] = 0;
                }

                // Mail in line
                String mails = EntityRecognition.mailExtraction(line);
                if (mails != "") {
                    features.get(i)[j++] = 1;
                } else {
                    features.get(i)[j++] = 0;
                }

                // Blank line
                int blank = EntityRecognition.isBlankLine(line);
                features.get(i)[j++] = blank;

                // Last line
                if (i + 1 == nbLines) {
                    features.get(i)[j++] = 1;
                } else {
                    features.get(i)[j++] = 0;
                }

                // Previous to last line
                if (i + 2 == nbLines) {
                    features.get(i)[j++] = 1;
                } else {
                    features.get(i)[j++] = 0;
                }

                // Sig pattern
                int sigPattern = EntityRecognition.startWithSigPattern(line);
                features.get(i)[j++] = sigPattern;

                // Many special characters
                int manySpeChar = EntityRecognition.manySpecialChar(line);
                features.get(i)[j++] = manySpeChar;

                // Typical Signature Word
                int typSigWord = EntityRecognition.typicalSigWord(line);
                features.get(i)[j++] = typSigWord;

                // End with quotation
                int namePattern = EntityRecognition.endWithQuotation(line);
                features.get(i)[j++] = namePattern;

                // Number of tabulations
                int numberOfTab = EntityRecognition.numberOfTab(line);
                if (numberOfTab == 1) {
                    features.get(i)[j++] = 1;
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 0;
                } else if (numberOfTab == 2) {
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 1;
                    features.get(i)[j++] = 0;
                } else if (numberOfTab >= 3) {
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 1;
                } else {
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 0;
                }

                // Punctuation
                double percentPunct = EntityRecognition.percentagePunctation(line);
                if (percentPunct > 20) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;
                if (percentPunct > 50) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;
                if (percentPunct > 90) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;

                // Start with reply sign
                int startWithReply = EntityRecognition.startWithReply(line);
                features.get(i)[j++] = startWithReply;

                // Start with punctuation
                int startWithPunct = EntityRecognition.startWithPunct(line);
                features.get(i)[j++] = startWithPunct;

                // Start with punctuation then reply
                int startWithPunctRep = EntityRecognition.startWithPunctuationReply(line);
                features.get(i)[j++] = startWithPunctRep;

                // Percentage numbers and letters
                double percentNumbLett = EntityRecognition.percentageNumberLetter(line);
                if (percentNumbLett < 90) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;
                if (percentNumbLett < 50) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;
                if (percentNumbLett < 10) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;

                // NER
                int person = 0;
                int org = 0;
                int number = 0;
                int city = 0;
                int country = 0;
                int nationality = 0;
                int state_or_province = 0;
                int date = 0;
                int email = 0;
                int title = 0;
                int money = 0;
                int misc = 0;
                int location = 0;
                int time = 0;
                int url = 0;
                int duration = 0;
                Pattern p = Pattern.compile("[a-z]|[A-Z]");
                Matcher m = p.matcher(line);
                if (m.find()) {
                    int[] ner = EntityRecognition.nerDetection(line);
                    person = ner[0];
                    org = ner[1];
                    number = ner[2];
                    city = ner[3];
                    country = ner[4];
                    nationality = ner[5];
                    state_or_province = ner[6];
                    date = ner[7];
                    email = ner[8];
                    title = ner[9];
                    money = ner[10];
                    misc = ner[11];
                    location = ner[12];
                    time = ner[13];
                    url = ner[14];
                    duration = ner[15];
                }
                // Person
                features.get(i)[j++] = person;
                // Organization
                features.get(i)[j++] = org;
                // Number
                features.get(i)[j++] = number;
                // City
                features.get(i)[j++] = city;
                // Country
                features.get(i)[j++] = country;
                // Nationality
                features.get(i)[j++] = nationality;
                // State_or_province
                features.get(i)[j++] = state_or_province;
                // Date
                features.get(i)[j++] = date;
                // Email
                features.get(i)[j++] = email;
                // Title
                features.get(i)[j++] = title;
                // Money
                features.get(i)[j++] = money;
                // Misc
                features.get(i)[j++] = misc;
                // Location
                features.get(i)[j++] = location;
                // Time
                features.get(i)[j++] = time;
                // Url
                features.get(i)[j++] = url;
                // Duration
                features.get(i)[j++] = duration;

                // Features for previous and next lines
                // First line has no previous
                if (i == 0) {
                    for (int k = 0; k < dimLine; k++) {
                        features.get(i)[dimLine + k] = 0;
                    }
                } else {
                    for (int k = 0; k < dimLine; k++) {
                        features.get(i - 1)[dimLine * 2 + k] = features.get(i)[k];
                    }
                }
                // Last line has no next
                if (i + 1 == nbLines) {
                    for (int k = 0; k < dimLine; k++) {
                        features.get(i)[dimLine * 2 + k] = 0;
                    }
                } else {
                    for (int k = 0; k < dimLine; k++) {
                        features.get(i + 1)[dimLine + k] = features.get(i)[k];
                    }
                }

                // read next line
                line = reader.readLine();
                i++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return features;
    }

    // Read from file
    public static List<Integer[]> getFeatures(File file) throws IOException {
        System.out.println(file.getName());

        List<Integer[]> features = new ArrayList<Integer[]>();

        int dimLine = 37;
        int dim = dimLine * 3;
        features.add(new Integer[dim]);

        BufferedReader reader;

        int nbLines = NumberLines.getNumberOfLines(file);
        int i = 0;
        reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        while (line != null) {
            int j = 0;
            if (i < nbLines - 1) {
                features.add(new Integer[dim]);
            }

            // Tel in line
            String tels = EntityRecognition.telExtraction(line);
            if (tels != "") {
                features.get(i)[j++] = 1;
            } else {
                features.get(i)[j++] = 0;
            }

            // Mail in line
            String mails = EntityRecognition.mailExtraction(line);
            if (mails != "") {
                features.get(i)[j++] = 1;
            } else {
                features.get(i)[j++] = 0;
            }

            // Blank line
            int blank = EntityRecognition.isBlankLine(line);
            features.get(i)[j++] = blank;

            // Last line
            if (i + 1 == nbLines) {
                features.get(i)[j++] = 1;
            } else {
                features.get(i)[j++] = 0;
            }

            // Previous to last line
            if (i + 2 == nbLines) {
                features.get(i)[j++] = 1;
            } else {
                features.get(i)[j++] = 0;
            }

            // Sig pattern
            int sigPattern = EntityRecognition.startWithSigPattern(line);
            features.get(i)[j++] = sigPattern;

            // Many special characters
            int manySpeChar = EntityRecognition.manySpecialChar(line);
            features.get(i)[j++] = manySpeChar;

            // Typical Signature Word
            int typSigWord = EntityRecognition.typicalSigWord(line);
            features.get(i)[j++] = typSigWord;

            // End with quotation
            int namePattern = EntityRecognition.endWithQuotation(line);
            features.get(i)[j++] = namePattern;

            // Number of tabulations
            int numberOfTab = EntityRecognition.numberOfTab(line);
            if (numberOfTab == 1) {
                features.get(i)[j++] = 1;
                features.get(i)[j++] = 0;
                features.get(i)[j++] = 0;
            } else if (numberOfTab == 2) {
                features.get(i)[j++] = 0;
                features.get(i)[j++] = 1;
                features.get(i)[j++] = 0;
            } else if (numberOfTab >= 3) {
                features.get(i)[j++] = 0;
                features.get(i)[j++] = 0;
                features.get(i)[j++] = 1;
            } else {
                features.get(i)[j++] = 0;
                features.get(i)[j++] = 0;
                features.get(i)[j++] = 0;
            }

            // Punctuation
            double percentPunct = EntityRecognition.percentagePunctation(line);
            if (percentPunct > 20) {
                features.get(i)[j++] = 1;
            } else
                features.get(i)[j++] = 0;
            if (percentPunct > 50) {
                features.get(i)[j++] = 1;
            } else
                features.get(i)[j++] = 0;
            if (percentPunct > 90) {
                features.get(i)[j++] = 1;
            } else
                features.get(i)[j++] = 0;

            // Start with reply sign
            int startWithReply = EntityRecognition.startWithReply(line);
            features.get(i)[j++] = startWithReply;

            // Start with punctuation
            int startWithPunct = EntityRecognition.startWithPunct(line);
            features.get(i)[j++] = startWithPunct;

            // Start with punctuation then reply
            int startWithPunctRep = EntityRecognition.startWithPunctuationReply(line);
            features.get(i)[j++] = startWithPunctRep;

            // Percentage numbers and letters
            double percentNumbLett = EntityRecognition.percentageNumberLetter(line);
            if (percentNumbLett < 90) {
                features.get(i)[j++] = 1;
            } else
                features.get(i)[j++] = 0;
            if (percentNumbLett < 50) {
                features.get(i)[j++] = 1;
            } else
                features.get(i)[j++] = 0;
            if (percentNumbLett < 10) {
                features.get(i)[j++] = 1;
            } else
                features.get(i)[j++] = 0;
            
            // NER
            int person = 0;
            int org = 0;
            int number = 0;
            int city = 0;
            int country = 0;
            int nationality = 0;
            int state_or_province = 0;
            int date = 0;
            int email = 0;
            int title = 0;
            int money = 0;
            int misc = 0;
            int location = 0;
            int time = 0;
            int url = 0;
            int duration = 0;
            Pattern p = Pattern.compile("[a-z]|[A-Z]");
            Matcher m = p.matcher(line);
            if (m.find()) {
                int[] ner = EntityRecognition.nerDetection(line);
                person = ner[0];
                org = ner[1];
                number = ner[2];
                city = ner[3];
                country = ner[4];
                nationality = ner[5];
                state_or_province = ner[6];
                date = ner[7];
                email = ner[8];
                title = ner[9];
                money = ner[10];
                misc = ner[11];
                location = ner[12];
                time = ner[13];
                url = ner[14];
                duration = ner[15];
            }
            // Person
            features.get(i)[j++] = person;
            // Organization
            features.get(i)[j++] = org;
            // Number
            features.get(i)[j++] = number;
            // City
            features.get(i)[j++] = city;
            // Country
            features.get(i)[j++] = country;
            // Nationality
            features.get(i)[j++] = nationality;
            // State_or_province
            features.get(i)[j++] = state_or_province;
            // Date
            features.get(i)[j++] = date;
            // Email
            features.get(i)[j++] = email;
            // Title
            features.get(i)[j++] = title;
            // Money
            features.get(i)[j++] = money;
            // Misc
            features.get(i)[j++] = misc;
            // Location
            features.get(i)[j++] = location;
            // Time
            features.get(i)[j++] = time;
            // Url
            features.get(i)[j++] = url;
            // Duration
            features.get(i)[j++] = duration;

            // Features for previous and next lines
            // First line has no previous
            if (i == 0) {
                for (int k = 0; k < dimLine; k++) {
                    features.get(i)[dimLine + k] = 0;
                }
            } else {
                for (int k = 0; k < dimLine; k++) {
                    features.get(i - 1)[dimLine * 2 + k] = features.get(i)[k];
                }
            }
            // Last line has no next
            if (i + 1 == nbLines) {
                for (int k = 0; k < dimLine; k++) {
                    features.get(i)[dimLine * 2 + k] = 0;
                }
            } else {
                for (int k = 0; k < dimLine; k++) {
                    features.get(i + 1)[dimLine + k] = features.get(i)[k];
                }
            }

            // read next line
            line = reader.readLine();
            i++;
        }
        reader.close();

        return features;
    }

    // Read directly from a string
    public static List<Integer[]> getFeaturesFromString(String mail) {
        List<Integer[]> features = new ArrayList<Integer[]>();

        int dimLine = 37;
        int dim = dimLine * 3;
        features.add(new Integer[dim]);

        BufferedReader reader;
        try {
            int nbLines = NumberLines.getNumberOfLinesFromString(mail);
            int i = 0;
            reader = new BufferedReader(new StringReader(mail));
            String line = reader.readLine();
            while (line != null) {
                int j = 0;
                if (i < nbLines - 1) {
                    features.add(new Integer[dim]);
                }

                // Tel in line
                String tels = EntityRecognition.telExtraction(line);
                if (tels != "") {
                    features.get(i)[j++] = 1;
                } else {
                    features.get(i)[j++] = 0;
                }

                // Mail in line
                String mails = EntityRecognition.mailExtraction(line);
                if (mails != "") {
                    features.get(i)[j++] = 1;
                } else {
                    features.get(i)[j++] = 0;
                }

                // Blank line
                int blank = EntityRecognition.isBlankLine(line);
                features.get(i)[j++] = blank;

                // Last line
                if (i + 1 == nbLines) {
                    features.get(i)[j++] = 1;
                } else {
                    features.get(i)[j++] = 0;
                }

                // Previous to last line
                if (i + 2 == nbLines) {
                    features.get(i)[j++] = 1;
                } else {
                    features.get(i)[j++] = 0;
                }

                // Sig pattern
                int sigPattern = EntityRecognition.startWithSigPattern(line);
                features.get(i)[j++] = sigPattern;

                // Many special characters
                int manySpeChar = EntityRecognition.manySpecialChar(line);
                features.get(i)[j++] = manySpeChar;

                // Typical Signature Word
                int typSigWord = EntityRecognition.typicalSigWord(line);
                features.get(i)[j++] = typSigWord;

                // End with quotation
                int namePattern = EntityRecognition.endWithQuotation(line);
                features.get(i)[j++] = namePattern;

                // Number of tabulations
                int numberOfTab = EntityRecognition.numberOfTab(line);
                if (numberOfTab == 1) {
                    features.get(i)[j++] = 1;
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 0;
                } else if (numberOfTab == 2) {
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 1;
                    features.get(i)[j++] = 0;
                } else if (numberOfTab >= 3) {
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 1;
                } else {
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 0;
                    features.get(i)[j++] = 0;
                }

                // Punctuation
                double percentPunct = EntityRecognition.percentagePunctation(line);
                if (percentPunct > 20) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;
                if (percentPunct > 50) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;
                if (percentPunct > 90) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;

                // Start with reply sign
                int startWithReply = EntityRecognition.startWithReply(line);
                features.get(i)[j++] = startWithReply;

                // Start with punctuation
                int startWithPunct = EntityRecognition.startWithPunct(line);
                features.get(i)[j++] = startWithPunct;

                // Start with punctuation then reply
                int startWithPunctRep = EntityRecognition.startWithPunctuationReply(line);
                features.get(i)[j++] = startWithPunctRep;

                // Percentage numbers and letters
                double percentNumbLett = EntityRecognition.percentageNumberLetter(line);
                if (percentNumbLett < 90) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;
                if (percentNumbLett < 50) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;
                if (percentNumbLett < 10) {
                    features.get(i)[j++] = 1;
                } else
                    features.get(i)[j++] = 0;
                
                // NER
                int person = 0;
                int org = 0;
                int number = 0;
                int city = 0;
                int country = 0;
                int nationality = 0;
                int state_or_province = 0;
                int date = 0;
                int email = 0;
                int title = 0;
                int money = 0;
                int misc = 0;
                int location = 0;
                int time = 0;
                int url = 0;
                int duration = 0;
                Pattern p = Pattern.compile("[a-z]|[A-Z]");
                Matcher m = p.matcher(line);
                if (m.find()) {
                    int[] ner = EntityRecognition.nerDetection(line);
                    person = ner[0];
                    org = ner[1];
                    number = ner[2];
                    city = ner[3];
                    country = ner[4];
                    nationality = ner[5];
                    state_or_province = ner[6];
                    date = ner[7];
                    email = ner[8];
                    title = ner[9];
                    money = ner[10];
                    misc = ner[11];
                    location = ner[12];
                    time = ner[13];
                    url = ner[14];
                    duration = ner[15];
                }
                // Person
                features.get(i)[j++] = person;
                // Organization
                features.get(i)[j++] = org;
                // Number
                features.get(i)[j++] = number;
                // City
                features.get(i)[j++] = city;
                // Country
                features.get(i)[j++] = country;
                // Nationality
                features.get(i)[j++] = nationality;
                // State_or_province
                features.get(i)[j++] = state_or_province;
                // Date
                features.get(i)[j++] = date;
                // Email
                features.get(i)[j++] = email;
                // Title
                features.get(i)[j++] = title;
                // Money
                features.get(i)[j++] = money;
                // Misc
                features.get(i)[j++] = misc;
                // Location
                features.get(i)[j++] = location;
                // Time
                features.get(i)[j++] = time;
                // Url
                features.get(i)[j++] = url;
                // Duration
                features.get(i)[j++] = duration;

                // Features for previous and next lines
                // First line has no previous
                if (i == 0) {
                    for (int k = 0; k < dimLine; k++) {
                        features.get(i)[dimLine + k] = 0;
                    }
                } else {
                    for (int k = 0; k < dimLine; k++) {
                        features.get(i - 1)[dimLine * 2 + k] = features.get(i)[k];
                    }
                }
                // Last line has no next
                if (i + 1 == nbLines) {
                    for (int k = 0; k < dimLine; k++) {
                        features.get(i)[dimLine * 2 + k] = 0;
                    }
                } else {
                    for (int k = 0; k < dimLine; k++) {
                        features.get(i + 1)[dimLine + k] = features.get(i)[k];
                    }
                }

                // read next line
                line = reader.readLine();
                i++;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return features;
    }

}
