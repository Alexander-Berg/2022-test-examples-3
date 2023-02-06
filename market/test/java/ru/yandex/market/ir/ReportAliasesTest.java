package ru.yandex.market.ir;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.ir.http.Formalizer;

import static org.junit.Assert.assertEquals;

public class ReportAliasesTest {

    private static final int CATEGORY_ID = 1;

    private DefaultFormalizer formalizer;

    @Before
    public void setup() throws ParserConfigurationException, SAXException, IOException {
        formalizer = new FormalizerBuilder()
                .setCategoryId(CATEGORY_ID)
                .setCategoryPathPostfix("report_aliases_test")
                .setReportAliasesFileName("reportAliases.pb")
                .build();
    }

    @Test
    public void withoutLayerTest() throws MissingKnowledgeTypeException, InterruptedException {
        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(buildFormalizeOfferRequest("без " +
                "кнопочное", null));
        assertEquals(1, formalizedOffer.getPositionCount());

        formalizedOffer = formalizer.formalizeOffer(buildFormalizeOfferRequest("касательное", null));
        assertEquals(0, formalizedOffer.getPositionCount());
    }

    @Test
    public void withLayerTest() throws MissingKnowledgeTypeException, InterruptedException {
        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(buildFormalizeOfferRequest("без " +
                "кнопочное", "A"));
        assertEquals(1, formalizedOffer.getPositionCount());

        formalizedOffer = formalizer.formalizeOffer(buildFormalizeOfferRequest("касательное", "A"));
        assertEquals(1, formalizedOffer.getPositionCount());

        formalizedOffer = formalizer.formalizeOffer(buildFormalizeOfferRequest("касательное", "B"));
        assertEquals(0, formalizedOffer.getPositionCount());
    }

    private FormalizeOfferRequest buildFormalizeOfferRequest(String value, String layer) {
        Formalizer.Offer offer = Formalizer.Offer.newBuilder()
                .setLocale("RU")
                .setCategoryId(CATEGORY_ID)
                .setTitle("Чайник, управление " + value)
                .build();

        Formalizer.FormalizerRequest.Builder protoRequest = Formalizer.FormalizerRequest.newBuilder()
                .addOffer(offer)
                .setReturnValueName(true)
                .setReturnParamName(true)
                .setReturnAllPosition(true);

        if (layer != null) {
            protoRequest.setLayer(layer);
        }

        return new FormalizeOfferRequest(offer, protoRequest.build());
    }
}
