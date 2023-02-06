package ru.yandex.market.ir;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.ir.http.ClientType;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.ir.http.Offer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfidentTest {
    private static final int CATEGORY_ID = 90584;

    private DefaultFormalizer formalizer;

    @Before
    public void setup() throws ParserConfigurationException, SAXException, IOException {
        formalizer = new FormalizerBuilder()
                .setCategoryId(CATEGORY_ID)
                .setCategoryPathPostfix("confident_test")
                .build();
    }

    @Test
    public void confidentTest() throws InterruptedException, MissingKnowledgeTypeException {
        FormalizeOfferRequest request = buildFormalizeOfferRequest();

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.STANDARD);

        assertEquals(4, formalizedOffer.getPositionCount());
        var params = formalizedOffer.getPositionList()
                .stream()
                .collect(Collectors.toMap(FormalizerParam.FormalizedParamPosition::getParamId, Function.identity()));
        assertTrue(params.get(28616160).getConfident());
        assertFalse(params.get(4922804).getConfident());
        assertTrue(params.get(16395390).getConfident());
        assertFalse(params.get(16395391).getConfident());
    }

    @NotNull
    private FormalizeOfferRequest buildFormalizeOfferRequest() {
        Formalizer.Offer offer = Formalizer.Offer.newBuilder()
                .setLocale("RU")
                .setCategoryId(CATEGORY_ID)
                .addYmlParam(Offer.YmlParam.newBuilder()
                        .setName("Программы")
                        .setValue("интенсивная"))
                .addYmlParam(Offer.YmlParam.newBuilder()
                        .setName("Управление")
                        .setValue("сенсорное"))
                .build();

        Formalizer.FormalizerRequest protoRequest = Formalizer.FormalizerRequest.newBuilder()
                .addOffer(offer)
                .setReturnValueName(true)
                .setReturnParamName(true)
                .setReturnAllPosition(true)
                .build();

        return new FormalizeOfferRequest(offer, protoRequest);
    }
}
