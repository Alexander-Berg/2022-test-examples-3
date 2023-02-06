package ru.yandex.market.ir;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.ir.http.ClientType;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.Offer;

import static org.junit.Assert.assertEquals;

public class BlackListTest {
    private static final int CATEGORY_ID = 90584;

    private DefaultFormalizer formalizer;

    @Before
    public void setup() throws ParserConfigurationException, SAXException, IOException {
        formalizer = new FormalizerBuilder()
                .setCategoryId(CATEGORY_ID)
                .setRootCategoryAcceptable(true)
                .build();
    }

    @Test
    public void removeBlackListParamTest() throws InterruptedException, MissingKnowledgeTypeException {
        FormalizeOfferRequest request = buildFormalizeOfferRequest();

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.STANDARD);

        assertEquals(0, formalizedOffer.getPositionCount());
    }

    @NotNull
    private FormalizeOfferRequest buildFormalizeOfferRequest() {
        Formalizer.Offer offer = Formalizer.Offer.newBuilder()
                .setLocale("RU")
                .setCategoryId(CATEGORY_ID)
                .setTitle("Посудомоечная машина")
                .addYmlParam(Offer.YmlParam.newBuilder()
                        .setName("Программы")
                        .setValue("быстрая"))
                .build();

        Formalizer.FormalizerRequest protoRequest = Formalizer.FormalizerRequest.newBuilder()
                .addOffer(offer)
                .setReturnValueName(true)
                .setReturnParamName(true)
                .setReturnAllPosition(true)
                .setClient(Formalizer.Client.REPORT)
                .build();

        return new FormalizeOfferRequest(offer, protoRequest);
    }
}
