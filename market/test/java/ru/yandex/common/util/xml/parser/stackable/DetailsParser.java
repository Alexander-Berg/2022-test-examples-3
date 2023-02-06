package ru.yandex.common.util.xml.parser.stackable;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import ru.yandex.common.util.xml.parser.ElementListener;
import ru.yandex.common.util.xml.parser.ElementOrientedSAXHandler;
import ru.yandex.common.util.xml.parser.StackableElementOrientedSAXHandler;

import java.util.HashMap;
import java.util.Map;

public class DetailsParser extends StackableElementOrientedSAXHandler<Details> {

    Map<String, String> params;

    public DetailsParser() {
        addElementListener("/details", new ElementListener() {
            @Override
            public void onOpen(ElementOrientedSAXHandler handler, Attributes attributes) throws SAXParseException {
                params = new HashMap<String, String>();
            }

            @Override
            public void onClose(ElementOrientedSAXHandler handler) throws SAXParseException { }
        });
        addElementListener("/details/param", new ElementListener() {
            @Override
            public void onOpen(ElementOrientedSAXHandler handler, Attributes attributes) throws SAXParseException {
                params.put(attributes.getValue("name"), attributes.getValue("value"));
            }

            @Override
            public void onClose(ElementOrientedSAXHandler handler) throws SAXParseException {
            }
        });
    }

    @Override
    public Details getParsed() {
        return new Details() {{
            params = DetailsParser.this.params;
        }};
    }
}
