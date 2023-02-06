package ru.yandex.common.util.xml.parser.stackable;

import org.xml.sax.Attributes;
import org.xml.sax.SAXParseException;
import ru.yandex.common.util.xml.parser.*;

import java.util.ArrayList;

public class ModelParser extends StackableElementOrientedSAXHandler<Model> {

    private OfferParser offerParser;
    private DetailsParser detailsParser;
    Model model;

    public void setOfferParser(OfferParser offerParser) {
        this.offerParser = offerParser;
    }

    public void setDetailsParser(DetailsParser detailsParser) {
        this.detailsParser = detailsParser;
    }
    
    public ModelParser() {
        addElementListener("/model", new ElementListener() {
            @Override
            public void onOpen(ElementOrientedSAXHandler handler, Attributes attributes) throws SAXParseException {
                model = new Model();
                model.offers = new ArrayList<Offer>();
                model.id = Integer.parseInt(attributes.getValue("id"));
            }

            @Override
            public void onClose(ElementOrientedSAXHandler handler) throws SAXParseException { }
        });
        addElementValueListener("/model/name", new ElementValueListener() {
            @Override
            public void onValue(ElementOrientedSAXHandler handler, String value) throws SAXParseException {
                model.name = value;
            }
        });
        addElementValueListener("/model/price", new ElementValueListener() {
            @Override
            public void onValue(ElementOrientedSAXHandler handler, String value) throws SAXParseException {
                model.price = Integer.parseInt(value);
            }
        });
        addInnerParser("/model/details", new InnerParser<Details>() {
            @Override
            public StackableElementOrientedSAXHandler<Details> getParser() {
                return detailsParser;
            }

            @Override
            public void parsed(Details parsed) {
                model.details = parsed;
            }
        });
        addInnerParser("/model/offers/offer", new InnerParser<Offer>() {
            @Override
            public StackableElementOrientedSAXHandler<Offer> getParser() {
                return offerParser;
            }

            @Override
            public void parsed(Offer parsed) {
                model.offers.add(parsed);
            }
        });
    }

    @Override
    public Model getParsed() {
        return model;
    }
}
