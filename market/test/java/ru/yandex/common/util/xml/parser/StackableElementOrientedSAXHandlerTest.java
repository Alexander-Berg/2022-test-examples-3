package ru.yandex.common.util.xml.parser;

import org.apache.xerces.parsers.SAXParser;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import ru.yandex.common.util.xml.InvalidXMLCharactersFilter;
import ru.yandex.common.util.xml.parser.stackable.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class StackableElementOrientedSAXHandlerTest {

    private CategoryParser categoryParser;

    <X extends DefaultHandler & Parser<T>, T> T parse(final X parser, final byte[] bytes) throws Exception {
        XMLReader xmlReader = new SAXParser();
        xmlReader.setContentHandler(parser);
        xmlReader.parse(new InputSource(
            new InvalidXMLCharactersFilter(
                new ByteArrayInputStream(bytes)
            )
        ));
        return parser.getParsed();
    }
    
    byte[] read(InputStream stream) throws Exception {
        final byte[] bytes = new byte[stream.available()];
        stream.read(bytes);
        return bytes;
    }

    @Before
    public void setUp() throws Exception {
        categoryParser = new CategoryParser();
        final ModelParser modelParser = new ModelParser();
        modelParser.setDetailsParser(new DetailsParser());
        modelParser.setOfferParser(new OfferParser());
        categoryParser.setModelParser(modelParser);
        categoryParser.setModelIdParser(new ModelIdParser());
        categoryParser.setModelsCountParser(new ModelsCountParser());

    }

    @Test
    public void testParse() throws Exception {
        final byte[] bytes = read(getClass().getResourceAsStream("/xml/blah.xml"));
        final Category actual = parse(categoryParser, bytes);

        final Category expected = new Category() {{;
            id = 1;
            name = "Телефоны";
            type = "guru";
            models = Arrays.asList(
                new Model() {{
                    id = 1234;
                    name = "iphone 5";
                    price = 18000;
                    details = new Details() {{
                        params = new HashMap<String, String>() {{
                            put("os", "ios");
                            put("flash", "16gb");
                        }};
                    }};
                    offers = Arrays.asList(
                        new Offer() {{
                            id = 2345;
                            shop = "ozon";
                        }},
                        new Offer() {{
                            id = 3456;
                            shop = "euroset";
                        }}
                    );
                }},
                new Model() {{
                    id = 4567;
                    name = "galaxy s4";
                    price = 19000;
                    details = new Details() {{
                        params = new HashMap<String, String>() {{
                            put("os", "android");
                            put("flash", "32gb");
                        }};
                    }};
                    offers = Arrays.asList(
                        new Offer() {{
                            id = 5678;
                            shop = "mvideo";
                        }},
                        new Offer() {{
                            id = 6789;
                            shop = "super-shop";
                        }}
                    );
                }}
            );
            modelIds = Arrays.asList(1234, 4567);
            modelsCount = 2;
        }};
        
        assertEquals(expected, actual);
    }

}
