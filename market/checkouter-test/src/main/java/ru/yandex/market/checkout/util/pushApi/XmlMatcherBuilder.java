package ru.yandex.market.checkout.util.pushApi;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.Matcher;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import static org.hamcrest.MatcherAssert.assertThat;


/**
 * @author Nikolai Iusiumbeli
 * date: 18/12/2017
 */
public class XmlMatcherBuilder {

    private final List<LoggedRequest> requests;
    private final List<Matcher<Node>> matchers = new ArrayList<>();


    public XmlMatcherBuilder(List<LoggedRequest> requests) {
        this.requests = requests;
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(false);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
    }

    public XmlMatcherBuilder matches(Matcher<Node> matcher) {
        matchers.add(matcher);
        return this;
    }

    public void check() throws Exception {
        for (LoggedRequest request : requests) {
            Document xml = parse(request.getBodyAsString());
            for (Matcher<Node> matcher : matchers) {
                assertThat(xml, matcher);
            }
        }
    }
}
