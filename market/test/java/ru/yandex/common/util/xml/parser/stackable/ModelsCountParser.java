package ru.yandex.common.util.xml.parser.stackable;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import ru.yandex.common.util.xml.parser.ElementListener;
import ru.yandex.common.util.xml.parser.ElementOrientedSAXHandler;
import ru.yandex.common.util.xml.parser.StackableElementOrientedSAXHandler;

public class ModelsCountParser extends StackableElementOrientedSAXHandler<Integer> {
    
    Integer count;

    public ModelsCountParser() {
        addElementListener("/models", new ElementListener() {
            @Override
            public void onOpen(ElementOrientedSAXHandler handler, Attributes attributes) throws SAXParseException {
                count = 0;
            }

            @Override
            public void onClose(ElementOrientedSAXHandler handler) throws SAXParseException { }
        });
        addElementListener("/models/model", new ElementListener() {
            @Override
            public void onOpen(ElementOrientedSAXHandler handler, Attributes attributes) throws SAXParseException {
                count++;
            }

            @Override
            public void onClose(ElementOrientedSAXHandler handler) throws SAXParseException { }
        });
    }

    @Override
    public Integer getParsed() {
        return count;
    }
}
