package ru.yandex.market.ir;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerParam;

public class UnitAliasesFormalizerTest {

    private static final int CATEGORY_ID = 12463727;

    private DefaultFormalizer formalizer;

    @Before
    public void setup() throws ParserConfigurationException, SAXException, IOException {
        formalizer = new FormalizerBuilder()
                .setCategoryId(CATEGORY_ID)
                .build();
    }

    @Test
    public void capacityCubicCentimeters() throws InterruptedException, MissingKnowledgeTypeException {
        String value = "Размер: 20 см³";

        FormalizerParam.FormalizedParamPosition position = getFormalizedParamPosition(value);

        Assert.assertEquals("capacity", position.getParamXslName());
        Assert.assertFalse("Unit start not found!", -1 == position.getUnitStart());
        Assert.assertFalse("Unit end not found!", -1 == position.getUnitEnd());
    }

    @Test
    public void capacityCubicMeters() throws InterruptedException, MissingKnowledgeTypeException {
        String value = "Размер: 0,0002 м³";

        FormalizerParam.FormalizedParamPosition position = getFormalizedParamPosition(value);

        Assert.assertEquals("capacity", position.getParamXslName());
        Assert.assertFalse("Unit start not found!", -1 == position.getUnitStart());
        Assert.assertFalse("Unit end not found!", -1 == position.getUnitEnd());
    }

    @Test
    public void widthCentimeters() throws InterruptedException, MissingKnowledgeTypeException {
        String value = "Размер: 20 см";

        FormalizerParam.FormalizedParamPosition position = getFormalizedParamPosition(value);

        Assert.assertEquals("Width", position.getParamXslName());
        Assert.assertFalse("Unit start not found!", -1 == position.getUnitStart());
        Assert.assertFalse("Unit end not found!", -1 == position.getUnitEnd());
    }

    private FormalizerParam.FormalizedParamPosition getFormalizedParamPosition(String value)
            throws InterruptedException, MissingKnowledgeTypeException {
        Formalizer.Offer offer = Formalizer.Offer.newBuilder().setLocale("RU").setCategoryId(CATEGORY_ID).setTitle(
            value).build();

        Formalizer.FormalizerRequest protoRequest = Formalizer.FormalizerRequest.newBuilder().addOffer(offer)
            .setReturnValueName(true).setReturnParamXslName(true).build();

        FormalizeOfferRequest request = new FormalizeOfferRequest(offer, protoRequest);

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request);
        Assert.assertEquals(1, formalizedOffer.getPositionCount());

        return formalizedOffer.getPosition(0);
    }
}
