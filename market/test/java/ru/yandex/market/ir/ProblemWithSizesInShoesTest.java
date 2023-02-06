package ru.yandex.market.ir;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.ir.http.ClientType;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.Offer;

public class ProblemWithSizesInShoesTest {

    private static final int CATEGORY_ID = 7814994;

    private DefaultFormalizer formalizer;

    @Before
    public void setup() throws ParserConfigurationException, SAXException, IOException {
        formalizer = new FormalizerBuilder()
                .setCategoryId(CATEGORY_ID)
                .build();
    }

    @Test
    public void test() throws InterruptedException, MissingKnowledgeTypeException {
        Formalizer.Offer offer1 = Formalizer.Offer.newBuilder()
                .setLocale("RU")
                .setCategoryId(CATEGORY_ID).setTitle(
                        "Женские кроссовки adidas Originals Ozweego")
                .addYmlParam(Offer.YmlParam.newBuilder().setName("Размер").setValue("40.5").setUnit("EU").build())
                .build();

        Formalizer.Offer offer2 = Formalizer.Offer.newBuilder()
                .setLocale("RU")
                .setCategoryId(CATEGORY_ID).setTitle(
                        "Женские кроссовки adidas Originals Ozweego")
                .addYmlParam(Offer.YmlParam.newBuilder().setName("Размер").setValue("40.5").setUnit("EUR").build())
                .build();

        Formalizer.FormalizerRequest protoRequest1 = Formalizer.FormalizerRequest.newBuilder().addOffer(offer1)
                .setReturnValueName(true).setReturnParamName(true).setReturnAllPosition(true).build();

        FormalizeOfferRequest request1 = new FormalizeOfferRequest(offer1, protoRequest1);

        Formalizer.FormalizerRequest protoRequest2 = Formalizer.FormalizerRequest.newBuilder().addOffer(offer2)
                .setReturnValueName(true).setReturnParamName(true).setReturnAllPosition(true).build();

        FormalizeOfferRequest request2 = new FormalizeOfferRequest(offer2, protoRequest2);

        Formalizer.FormalizedOffer formalizedOffer1 = formalizer.formalizeOffer(request1, ClientType.STANDARD);

        Formalizer.FormalizedOffer formalizedOffer2 = formalizer.formalizeOffer(request2, ClientType.STANDARD);

        //ответы formalizedOffer1 и formalizedOffer2 должны быть практически одинаковыми
        //как минимум это тест должен проходить
//        Assert.assertEquals(formalizedOffer1.getPositionList().size(), formalizedOffer2.getPositionList().size());


    }
}
