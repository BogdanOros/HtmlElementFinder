package io.boros;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

public class OkApp {

    private final static String DEFAULT_ID = "make-everything-ok-button";
    private final static String ENCODING = "utf8";

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("No arguments provided");
        }
        String elementId = args.length == 3 ? args[2] : DEFAULT_ID;

        OkApp okApp = new OkApp();

        final String inputFile = args[0];
        Map<String, String> criteria = okApp.collectCriteria(new File(inputFile), elementId);

        if (criteria.isEmpty()) {
            throw new IllegalArgumentException("Input HTML provided without '" + elementId + "' button");
        }

        final String changedFile = args[1];
        Collection<MatchingResult> matchingResults = okApp.searchSimilarElements(new File(changedFile), criteria, elementId);
        okApp.printMatches(System.out, matchingResults);
    }


    private Map<String, String> collectCriteria(File htmlInput, String id) {
        try {
            Document doc = Jsoup.parse(htmlInput, ENCODING, htmlInput.getAbsolutePath());

            Element element = doc.getElementById(id);

            if (element == null) {
                return emptyMap();
            }

            Map<String, String> attributes = new HashMap<>();
            for (Attribute attribute : element.attributes()) {
                attributes.put(attribute.getKey(), attribute.getValue());
            }

            attributes.put("text", element.text());

            return attributes;
        } catch (IOException e) {
            throw new IllegalArgumentException("Error during opening input file. Exiting");
        }
    }

    private Collection<MatchingResult> searchSimilarElements(File file, Map<String, String> criteria, String id) {
        Document doc;
        try {
            doc = Jsoup.parse(file, ENCODING);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error during opening output file. Exiting");
        }

        Element elementById = doc.getElementById(id);

        // Checking direct id equality to prevent unnecessary HTML crawling.
        if (elementById != null) {
            return Collections.singletonList(new MatchingResult(elementById, singletonMap("id", id)));
        }

        Elements elementsByTag = doc.getElementsByTag("a");

        // To prevent toMap collection and further sorts.
        PriorityQueue<MatchingResult> matchedElements = new PriorityQueue<>(
                Collections.reverseOrder(Comparator.comparingInt(MatchingResult::getMatchingScore))
        );

        for (Element element : elementsByTag) {
            Attributes attributes = element.attributes();

            Map<String, String> matchedAttributes = new HashMap<>();

            for (Attribute attribute : attributes) {
                String criteriaValue = criteria.get(attribute.getKey());

                if (attribute.getValue().equals(criteriaValue)) {
                    matchedAttributes.put(attribute.getKey(), attribute.getValue());
                }
            }

            String text = element.text();

            boolean sameTextPresent = !text.isEmpty() && text.equals(criteria.get("text"));

            if (sameTextPresent) {
                matchedAttributes.put("text", text);
            }

            if (!matchedAttributes.isEmpty()) {
                matchedElements.add(new MatchingResult(element, matchedAttributes));
            }
        }
        return matchedElements;
    }


    private void printMatches(OutputStream outputStream, Collection<MatchingResult> matchingResults) {
        PrintWriter printWriter = new PrintWriter(outputStream);

        Iterator<MatchingResult> iter = matchingResults.iterator();

        if (matchingResults.isEmpty()) {
            printWriter.write("No matches were found.");
        } else if (matchingResults.size() == 1) {
            printWriter.write(iter.next().toString());
        } else {
            printWriter.write("Best match: \n");
            printWriter.write(iter.next() + "\n");
            printWriter.write("Maybe similar elements: \n");
            while (iter.hasNext()) {
                printWriter.write(iter.next().toString() + "\n");

            }
        }
        printWriter.flush();

    }

}
