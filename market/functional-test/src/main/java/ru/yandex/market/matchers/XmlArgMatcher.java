package ru.yandex.market.matchers;

import java.io.IOException;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.mockito.ArgumentMatcher;
import org.xml.sax.SAXException;

public class XmlArgMatcher implements ArgumentMatcher<String> {

    private final String expected;

    public XmlArgMatcher(String expected) {
        this.expected = expected;
    }

    @Override
    public boolean matches(String actual) {
        try {
            XMLUnit.setIgnoreWhitespace(true);
            Diff diff = XMLUnit.compareXML(expected, actual);
            return diff.similar();
        } catch (SAXException | IOException e) {
            return false;
        }
    }
}
