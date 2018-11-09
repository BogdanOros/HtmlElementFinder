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


    /**
     * collectCriteria returns a map of searching criteria to search in modified files.
     *
     * @param htmlInput - the input (original, not-changed) file.
     * @param id        - searched element id.
     * @return a map of attributes to serve as criteria
     */
    private Map<String, String> collectCriteria(File htmlInput, String id) {
        Objects.requireNonNull(htmlInput);
        Objects.requireNonNull(id);

        Document doc = getDocumentSafe(htmlInput);

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
    }

    /**
     * Searches similar elements matching to criteria from provided file.
     * Returns a collection of matching results, which contains
     * all matched elements and explanations about matching decisions (via criteria and criteria intersections).
     */
    private Collection<MatchingResult> searchSimilarElements(File file, Map<String, String> criteria, String id) {
        Objects.requireNonNull(file);
        Objects.requireNonNull(criteria);
        Objects.requireNonNull(id);

        Document doc = getDocumentSafe(file);

        Element elementById = doc.getElementById(id);

        // Checking direct id equality to prevent unnecessary HTML crawling.
        if (elementById != null) {
            return Collections.singletonList(new MatchingResult(elementById, singletonMap("id", id)));
        }

        // TODO: instead of getting elements by tag, parse all tree recursively with ForkJoin
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

    private Document getDocumentSafe(File file) {
        try {
            return Jsoup.parse(file, ENCODING);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error during opening " + file.getName() + "  file. Exiting");
        }
    }


    /**
     * Prints all matches to an output stream.
     *
     * @param outputStream    - stream to write matching results (File, Connection stream, Logger etc)
     * @param matchingResults - a collection of matches
     */
    private void printMatches(OutputStream outputStream, Collection<MatchingResult> matchingResults) {
        Objects.requireNonNull(outputStream);
        Objects.requireNonNull(matchingResults);

        PrintWriter printWriter = new PrintWriter(outputStream);

        // Working with iterator to allow working with Collection<> interface instead of high-level implementations
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
