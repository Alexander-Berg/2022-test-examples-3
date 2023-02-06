package ru.yandex.market.logistics.nesu.utils.stax;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import com.ctc.wstx.exc.WstxParsingException;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.AbstractTest;
import ru.yandex.market.logistics.nesu.service.feed.parser.ParsedOffer;

import static java.lang.ClassLoader.getSystemResourceAsStream;

class StaxStreamProcessorTest extends AbstractTest {

    private static final XMLInputFactory FACTORY = XMLInputFactory.newInstance();

    @Test
    void xxeTestDTD() throws Exception {
        FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);
        InputStream inputStream = getSystemResourceAsStream("utils/stax/xxe.xml");
        XMLStreamReader xmlStreamReader = FACTORY.createXMLStreamReader(inputStream);

        JAXBContext jc = JAXBContext.newInstance(ParsedOffer.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();

        try {
            unmarshaller.unmarshal(xmlStreamReader);
            softly.fail("failure");
        } catch (UnmarshalException e) {
            WstxParsingException wstxParsingException = (WstxParsingException) e.getLinkedException();
            softly.assertThat(wstxParsingException)
                .hasMessageContaining("Undeclared general entity \"xxe\"");
        }
    }

    @Test
    void xxeTestExternalEntities() throws Exception {
        FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, true);
        FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        InputStream inputStream = getSystemResourceAsStream("utils/stax/xxe.xml");
        XMLStreamReader xmlStreamReader = FACTORY.createXMLStreamReader(inputStream);

        JAXBContext jc = JAXBContext.newInstance(ParsedOffer.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();

        try {
            unmarshaller.unmarshal(xmlStreamReader);
            softly.fail("failure");
        } catch (UnmarshalException e) {
            WstxParsingException wstxParsingException = (WstxParsingException) e.getLinkedException();
            softly.assertThat(wstxParsingException)
                .hasMessageContaining("has feature \"javax.xml.stream.isSupportingExternalEntities\" disabled");
        }
    }

    @Test
    void xxeTestWithoutProperties() throws Exception {
        FACTORY.setProperty(XMLInputFactory.SUPPORT_DTD, true);
        FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, true);
        InputStream inputStream = getSystemResourceAsStream("utils/stax/xxe.xml");
        XMLStreamReader xmlStreamReader = FACTORY.createXMLStreamReader(inputStream);

        JAXBContext jc = JAXBContext.newInstance(ParsedOffer.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();

        ParsedOffer parsedOffer = (ParsedOffer) unmarshaller.unmarshal(xmlStreamReader);

        softly.assertThat(parsedOffer.getName()).isNotNull();
    }

    @Test
    void xxeTest() throws Exception {
        InputStream inputStream = getSystemResourceAsStream("utils/stax/xxe.xml");
        StaxStreamProcessor staxStreamProcessor = new StaxStreamProcessor(inputStream);

        JAXBContext jc = JAXBContext.newInstance(ParsedOffer.class);

        Unmarshaller unmarshaller = jc.createUnmarshaller();

        try {
            unmarshaller.unmarshal(staxStreamProcessor.getReader());
            softly.fail("failure");
        } catch (UnmarshalException e) {
            WstxParsingException wstxParsingException = (WstxParsingException) e.getLinkedException();
            softly.assertThat(wstxParsingException)
                .hasMessageContaining("Undeclared general entity \"xxe\"");
        }
    }
}
