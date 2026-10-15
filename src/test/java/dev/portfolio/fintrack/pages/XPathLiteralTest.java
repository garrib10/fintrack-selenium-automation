package dev.portfolio.fintrack.pages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Evaluates generated literals with the JDK's XPath 1.0 engine, the same
 * dialect browsers use, so the escaping is proven rather than string-compared.
 */
@Tag("unit")
class XPathLiteralTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "plain text",
            "O'Brien",
            "\"QA\" team",
            "selenium-run O'Brien \"QA\" & Co.",
            "'starts and ends'",
            "\"",
            "'"
    })
    void literalMatchesTheExactText(String text) throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .newDocument();

        Element root = document.createElement("rows");
        Element match = document.createElement("row");
        match.setTextContent(text);
        Element other = document.createElement("row");
        other.setTextContent(text + " (other)");

        root.appendChild(other);
        root.appendChild(match);
        document.appendChild(root);

        Double matches = (Double) XPathFactory.newInstance()
                .newXPath()
                .evaluate(
                        "count(//row[text()=" + BasePage.xpathLiteral(text) + "])",
                        document,
                        XPathConstants.NUMBER);

        assertEquals(1.0, matches);
    }
}
