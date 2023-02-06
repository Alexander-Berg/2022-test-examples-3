package ru.yandex.common.util.xml.parser.stackable;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import ru.yandex.common.util.xml.parser.ElementListener;
import ru.yandex.common.util.xml.parser.ElementOrientedSAXHandler;
import ru.yandex.common.util.xml.parser.ElementValueListener;
import ru.yandex.common.util.xml.parser.StackableElementOrientedSAXHandler;

public class OfferParser extends StackableElementOrientedSAXHandler<Offer> {
    
    Offer offer;

    public OfferParser() {
        addElementListener("/offer", new ElementListener() {
            @Override
            public void onOpen(ElementOrientedSAXHandler handler, Attributes attributes) throws SAXParseException {
                offer = new Offer();
                offer.id = Integer.parseInt(attributes.getValue("id"));
            }

            @Override
            public void onClose(ElementOrientedSAXHandler handler) throws SAXParseException { }
        });

        addElementValueListener("/offer/shop", new ElementValueListener() {
            @Override
            public void onValue(ElementOrientedSAXHandler handler, String value) throws SAXParseException {
                offer.shop = value;
            }
        });
    }

    @Override
    public Offer getParsed() {
        return offer;
    }
}
