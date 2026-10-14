package org.example;

import wordfrequency.PorterStemer; // Asigură-te că pachetul este corect

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {


    private static final String RESOURCES_PATH = "src/main/resources/";
    private static final String STOPWORDS_FILE = RESOURCES_PATH + "stopwords.txt";
    private static final String TRAINING_DIR = RESOURCES_PATH + "Training";
    private static final String TESTING_DIR = RESOURCES_PATH + "Testing";
    private static final String OUTPUT_FILE = "rezultat.txt";

    private static Map<String, Integer> globalDictionary = new LinkedHashMap<>();
    private static Set<String> stopWords = new HashSet<>();
    private static PorterStemer stemmer = new PorterStemer();


    static class DocumentData {
        String docName;
        String folderName;
        List<String> topics = new ArrayList<>();
        Map<Integer, Integer> sparseVector = new TreeMap<>();
    }

    public static void main(String[] args) {
        try {
            System.out.println("1. Se încarcă stopwords...");
            loadStopWords();

            List<DocumentData> allDocuments = new ArrayList<>();

            System.out.println("2. Se procesează folderul Training...");
            allDocuments.addAll(processFolder(Paths.get(TRAINING_DIR), "Training"));

            System.out.println("3. Se procesează folderul Testing...");
            allDocuments.addAll(processFolder(Paths.get(TESTING_DIR), "Testing"));

            System.out.println("4. Se generează fișierul de ieșire...");
            generateOutputFile(allDocuments);

            System.out.println("GATA! Fișierul a fost salvat ca: " + OUTPUT_FILE);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Încarcă cuvintele de legătură într-un Set
    private static void loadStopWords() throws IOException {
        Path path = Paths.get(STOPWORDS_FILE);
        if (Files.exists(path)) {
            List<String> lines = Files.readAllLines(path);
            for (String line : lines) {
                stopWords.add(line.trim().toLowerCase());
            }
        } else {
            System.err.println("Atenție: Fișierul stopwords.txt nu a fost găsit la " + path.toAbsolutePath());
        }
    }


    private static List<DocumentData> processFolder(Path folderPath, String folderName) throws IOException {
        List<DocumentData> documents = new ArrayList<>();

        if (!Files.exists(folderPath)) {
            System.err.println("Atenție: Folderul " + folderPath.toAbsolutePath() + " nu există!");
            return documents;
        }

        try (Stream<Path> paths = Files.list(folderPath)) {
            List<Path> files = paths.filter(Files::isRegularFile).collect(Collectors.toList());
            for (Path file : files) {
                documents.add(processSingleFile(file, folderName));
            }
        }
        return documents;
    }


    private static DocumentData processSingleFile(Path filePath, String folderName) throws IOException {
        String content = Files.readString(filePath);
        DocumentData doc = new DocumentData();
        doc.docName = filePath.getFileName().toString().replace(".txt", "").replace(".XML", "");
        doc.folderName = folderName;


        String title = extractTagContent(content, "<title>", "</title>");
        String text = extractTagContent(content, "<text>", "</text>");
        String fullText = title + " " + text;


        doc.topics = extractTopics(content);


        fullText = fullText.replaceAll("<[^>]+>", " "); // elimină orice tag
        fullText = fullText.replaceAll("[^a-zA-Z\\s]", " "); // păstrează doar litere (fără cifre/punctuație)
        fullText = fullText.toLowerCase();


        String[] words = fullText.split("\\s+");
        Map<String, Integer> wordFrequencies = new HashMap<>();

        for (String word : words) {
            word = word.trim();
            // Ignoră cuvintele prea scurte sau stopwords
            if (word.length() > 2 && !stopWords.contains(word)) {
                // Aplică stemming
                String stemmedWord = stemmer.stemWord(word);

                if (stemmedWord.length() > 2) {
                    wordFrequencies.put(stemmedWord, wordFrequencies.getOrDefault(stemmedWord, 0) + 1);
                }
            }
        }


        for (Map.Entry<String, Integer> entry : wordFrequencies.entrySet()) {
            String word = entry.getKey();
            int frequency = entry.getValue();

            if (!globalDictionary.containsKey(word)) {
                globalDictionary.put(word, globalDictionary.size()); // indexul va fi mărimea curentă
            }

            int wordIndex = globalDictionary.get(word);
            doc.sparseVector.put(wordIndex, frequency);
        }

        return doc;
    }

    private static String extractTagContent(String source, String startTag, String endTag) {
        int startIndex = source.indexOf(startTag);
        int endIndex = source.indexOf(endTag);
        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return source.substring(startIndex + startTag.length(), endIndex);
        }
        return "";
    }


    private static List<String> extractTopics(String content) {
        List<String> topics = new ArrayList<>();

        String topicsBlock = extractTagContent(content, "<codes class=\"bip:topics:1.0\">", "</codes>");

        if (!topicsBlock.isEmpty()) {

            Pattern codePattern = Pattern.compile("<code code=\"([^\"]+)\"");
            Matcher matcher = codePattern.matcher(topicsBlock);
            while (matcher.find()) {
                topics.add(matcher.group(1));
            }
        }
        return topics;
    }


    private static void generateOutputFile(List<DocumentData> documents) throws IOException {
        Path outPath = Paths.get(OUTPUT_FILE);

        try (BufferedWriter writer = Files.newBufferedWriter(outPath)) {

            for (String word : globalDictionary.keySet()) {
                writer.write("@attribute " + word);
                writer.newLine();
            }


            writer.write("@data");
            writer.newLine();


            for (DocumentData doc : documents) {
                StringBuilder line = new StringBuilder();
                line.append(doc.docName).append(" # ");

                for (Map.Entry<Integer, Integer> entry : doc.sparseVector.entrySet()) {
                    line.append(entry.getKey()).append(":").append(entry.getValue()).append(" ");
                }
                line.append("# ");


                if (doc.topics.isEmpty()) {
                    line.append("none ");
                } else {
                    for (String topic : doc.topics) {
                        line.append(topic).append(" ");
                    }
                }

                line.append("# ").append(doc.folderName);

                writer.write(line.toString().trim());
                writer.newLine();
            }
        }
    }
}