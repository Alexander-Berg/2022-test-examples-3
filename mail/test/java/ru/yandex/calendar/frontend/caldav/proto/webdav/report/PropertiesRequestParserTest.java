package ru.yandex.calendar.frontend.caldav.proto.webdav.report;

import java.io.StringReader;

import org.junit.Test;
import org.w3c.dom.Element;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.xml.dom.DomUtils;

/**
 * @author Daniel Brylev
 */
public class PropertiesRequestParserTest {
    @Test
    public void byProperty() {
        String text =
                "<D:prop xmlns:D=\"DAV:\">\n" +
                "   <D:getetag/>\n" +
                "   <D:displayname/>\n" +
                "   <D:getcontenttype/>\n" +
                "</D:prop>\n";

        Element xml = DomUtils.read(new StringReader(text)).getDocumentElement();
        Option<PropertiesRequest> request = PropertiesRequestParser.parseProp(xml);

        Assert.some(request);
        Assert.equals(PropFindType.BY_PROPERTY, request.get().getType());
        Assert.hasSize(3, request.get().getDavPropertyNameSetO().get().getContent());
    }

    @Test
    public void allProp() {
        String text = "<D:allprop xmlns:D=\"DAV:\"/>";
        Element xml = DomUtils.read(new StringReader(text)).getDocumentElement();
        Option<PropertiesRequest> request = PropertiesRequestParser.parseProp(xml);

        Assert.some(request);
        Assert.equals(PropFindType.ALL_PROP, request.get().getType());
    }

    @Test
    public void propertyNames() {
        String text = "<D:propname xmlns:D=\"DAV:\"/>";
        Element xml = DomUtils.read(new StringReader(text)).getDocumentElement();
        Option<PropertiesRequest> request = PropertiesRequestParser.parseProp(xml);

        Assert.some(request);
        Assert.equals(PropFindType.PROPERTY_NAMES, request.get().getType());
    }

    @Test
    public void none() {
        String text = "<D:another xmlns:D=\"DAV:\"/>";
        Element xml = DomUtils.read(new StringReader(text)).getDocumentElement();

        Assert.none(PropertiesRequestParser.parseProp(xml));
    }
}
