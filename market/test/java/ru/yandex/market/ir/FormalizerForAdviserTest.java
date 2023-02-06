package ru.yandex.market.ir;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.ir.http.ClientType;
import ru.yandex.market.ir.http.Formalizer;

public class FormalizerForAdviserTest {

    private static final int CATEGORY_ID = 91491;

    private DefaultFormalizer formalizer;

    @Before
    public void setup() throws ParserConfigurationException, SAXException, IOException {
        formalizer = new FormalizerBuilder()
                .setCategoryId(CATEGORY_ID)
                .build();
    }

    @Test
    public void test() throws InterruptedException, MissingKnowledgeTypeException {
        Formalizer.Offer offer = Formalizer.Offer.newBuilder().setLocale("RU").setCategoryId(CATEGORY_ID).setTitle(
                "Смартфон Apple iPhone XR 128GB красный").build();

        Formalizer.FormalizerRequest protoRequest = Formalizer.FormalizerRequest.newBuilder().addOffer(offer)
                .setReturnValueName(true).setReturnParamName(true).build();

        FormalizeOfferRequest request = new FormalizeOfferRequest(offer, protoRequest);

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.ADVISER);
        Assert.assertEquals(1, formalizedOffer.getPositionCount());

        Assert.assertEquals(formalizedOffer.getPosition(0).getParamName(), "Цвет товара");
    }
}
