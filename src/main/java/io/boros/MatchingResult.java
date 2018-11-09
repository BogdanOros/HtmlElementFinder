package io.boros;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.reverse;

public class MatchingResult {

    private final Element element;
    private final Map<String, String> matchedAttributes;

    MatchingResult(Element element, Map<String, String> matchedAttributes) {
        this.element = element;
        this.matchedAttributes = matchedAttributes;
    }

    public Element getElement() {
        return element;
    }

    public int getMatchingScore() {
        return matchedAttributes.size();
    }

    public Map<String, String> getMatchedAttributes() {
        return matchedAttributes;
    }

    @Override
    public String toString() {
        return "Path: " + build(getElement()) +
                ". Matching score: " + getMatchingScore() +
                ". Matches: " + getMatchedAttributes();
    }

    private String build(Element element) {
        ArrayList<Element> parents = element.parents();
        reverse(parents);
        StringBuilder s = new StringBuilder();
        for (Element e : parents) {
            List<Element> sameTypeSiblings = getSameTypeSiblings(e);
            if (sameTypeSiblings.size() > 1) {
                int index = sameTypeSiblings.indexOf(e);
                s.append(e.tagName())
                        .append("[")
                        .append(index)
                        .append("]")
                        .append(" > ");
            } else {
                s.append(e.tagName())
                        .append(" > ");
            }
        }
        return s.append(element.tagName()).toString();

    }

    private List<Element> getSameTypeSiblings(Element e) {
        Elements siblingElements = e.parent().children();
        if (siblingElements.size() <= 1) {
            return Collections.emptyList();
        }
        return siblingElements
                .stream()
                .filter(sibling -> sibling.tag().equals(e.tag()))
                .collect(Collectors.toList());
    }

}
