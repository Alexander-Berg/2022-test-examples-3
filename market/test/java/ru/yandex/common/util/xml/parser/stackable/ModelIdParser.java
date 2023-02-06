package ru.yandex.common.util.xml.parser.stackable;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import ru.yandex.common.util.xml.parser.ElementListener;
import ru.yandex.common.util.xml.parser.ElementOrientedSAXHandler;
import ru.yandex.common.util.xml.parser.ElementValueListener;
import ru.yandex.common.util.xml.parser.StackableElementOrientedSAXHandler;

public class ModelIdParser extends StackableElementOrientedSAXHandler<Integer> {
    
    Integer modelId;

    public ModelIdParser() {
        addElementListener("/model", new ElementListener() {
            @Override
            public void onOpen(ElementOrientedSAXHandler handler, Attributes attributes) throws SAXParseException {
                modelId = Integer.valueOf(attributes.getValue("id"));
            }

            @Override
            public void onClose(ElementOrientedSAXHandler handler) throws SAXParseException { }
        });
    }

    @Override
    public Integer getParsed() {
        return modelId;
    }
}
