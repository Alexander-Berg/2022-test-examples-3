package ru.yandex.market.ir;

import java.io.IOException;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.ir.http.Offer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BooleanAntipatternTest {

    private static final int CATEGORY_ID = 1;

    private DefaultFormalizer formalizer;

    @Before
    public void setup() throws ParserConfigurationException, SAXException, IOException {
        formalizer = new FormalizerBuilder()
                .setCategoryId(CATEGORY_ID)
                .setCategoryPathPostfix("boolean_antipattern")
                .build();
    }

    @Test
    public void test() throws MissingKnowledgeTypeException, InterruptedException {
        final Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(buildFormalizeOfferRequest());

        assertEquals(2, formalizedOffer.getPositionCount());
        var params = formalizedOffer.getPositionList()
                .stream()
                .collect(Collectors.toMap(FormalizerParam.FormalizedParamPosition::getParamId, pos -> pos));

        assertEquals("Крышка", params.get(1).getParamName());
        assertTrue(params.get(1).getBooleanValue());

        assertEquals("Ручка", params.get(2).getParamName());
        assertFalse(params.get(2).getBooleanValue());
    }

    @NotNull
    private FormalizeOfferRequest buildFormalizeOfferRequest() {
        Formalizer.Offer offer = Formalizer.Offer.newBuilder()
                .setLocale("RU")
                .setCategoryId(CATEGORY_ID)
                .setTitle("Чайник")
                .addYmlParam(Offer.YmlParam.newBuilder()
                        .setName("Крышка")
                        .setValue("без ручки"))
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
