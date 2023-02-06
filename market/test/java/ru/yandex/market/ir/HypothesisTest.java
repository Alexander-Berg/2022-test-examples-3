package ru.yandex.market.ir;

import java.io.IOException;

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

public class HypothesisTest {

    private static final int CATEGORY_ID = 90584;

    private DefaultFormalizer formalizer;

    @Before
    public void setup() throws ParserConfigurationException, SAXException, IOException {
        formalizer = new FormalizerBuilder()
                .setCategoryId(CATEGORY_ID)
                .build();
    }

    @Test
    public void returnOneHypothesesTest() throws InterruptedException, MissingKnowledgeTypeException {
        FormalizeOfferRequest request = buildFormalizeOfferRequest(true,
                Offer.YmlParam.newBuilder()
                        .setName("Программы")
                        .setValue("быстрая"),
                Offer.YmlParam.newBuilder()
                        .setName("Цвет")
                        .setValue("Грибной"));

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.STANDARD);

        assertEquals(1, formalizedOffer.getPositionCount());
        assertEquals(1, formalizedOffer.getHypothesesCount());

        final FormalizerParam.FormalizedParamPosition position = formalizedOffer.getPosition(0);
        assertEquals(28616160, position.getParamId());
        assertEquals(28616187, position.getValueId());

        final FormalizerParam.FormalizedHypothesisPosition hypothesis = formalizedOffer.getHypotheses(0);
        assertEquals(14871214, hypothesis.getParamId());
        assertEquals("Грибной", hypothesis.getStringValue());
        assertEquals(0, hypothesis.getParamStart());
        assertEquals(4, hypothesis.getParamEnd());
        assertEquals(5, hypothesis.getValueStart());
        assertEquals(12, hypothesis.getValueEnd());
    }

    @Test
    public void returnTwoHypothesesForOneParamTest() throws InterruptedException, MissingKnowledgeTypeException {
        FormalizeOfferRequest request = buildFormalizeOfferRequest(true,
                Offer.YmlParam.newBuilder()
                        .setName("Программы")
                        .setValue("быстрая"),
                Offer.YmlParam.newBuilder()
                        .setName("Класс мойки")
                        .setValue("Z"));

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.STANDARD);

        assertEquals(1, formalizedOffer.getPositionCount());
        assertEquals(1, formalizedOffer.getHypothesesCount());

        FormalizerParam.FormalizedParamPosition position = formalizedOffer.getPosition(0);
        assertEquals(28616160, position.getParamId());
        assertEquals(28616187, position.getValueId());

        FormalizerParam.FormalizedHypothesisPosition hypothesis = formalizedOffer.getHypotheses(0);
        assertEquals(4922801, hypothesis.getParamId());
        assertEquals("Z", hypothesis.getStringValue());
    }

    @Test
    public void hypothesesForNonMultivalueParam() throws MissingKnowledgeTypeException, InterruptedException {
        FormalizeOfferRequest offer = buildFormalizeOfferRequest(true,
                Offer.YmlParam.newBuilder()
                        .setName("Тип установки")
                        .setValue("встраиваемая"),
                Offer.YmlParam.newBuilder()
                        .setName("Тип установки")
                        .setValue("левитируемая")
        );

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(offer);
        assertEquals(1, formalizedOffer.getPositionCount());
        assertEquals(0, formalizedOffer.getHypothesesCount());
    }

    @Test
    public void hypothesesForMultivalueParam() throws MissingKnowledgeTypeException, InterruptedException {
        FormalizeOfferRequest offer = buildFormalizeOfferRequest(true,
                Offer.YmlParam.newBuilder()
                        .setName("Протокол связи")
                        .setValue("Ethernet"),
                Offer.YmlParam.newBuilder()
                        .setName("Протокол связи")
                        .setValue("MaskSatellite")
        );

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(offer);
        assertEquals(1, formalizedOffer.getPositionCount());
        assertEquals(1, formalizedOffer.getHypothesesCount());
    }

    @Test
    public void notReturnHypothesesTest() throws InterruptedException, MissingKnowledgeTypeException {
        FormalizeOfferRequest request = buildFormalizeOfferRequest(false,
                Offer.YmlParam.newBuilder()
                        .setName("Программы")
                        .setValue("быстрая"),
                Offer.YmlParam.newBuilder()
                        .setName("Цвет")
                        .setValue("Грибной"));


        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.STANDARD);

        assertEquals(1, formalizedOffer.getPositionCount());
        assertEquals(0, formalizedOffer.getHypothesesCount());

        assertEquals(28616160, formalizedOffer.getPosition(0).getParamId());
        assertEquals(28616187, formalizedOffer.getPosition(0).getValueId());
    }

    @Test
    public void emptyValueTest() throws InterruptedException, MissingKnowledgeTypeException {
        FormalizeOfferRequest request = buildFormalizeOfferRequest(true,
                Offer.YmlParam.newBuilder()
                        .setName("Программы")
                        .setValue("быстрая"),
                Offer.YmlParam.newBuilder()
                        .setName("Цвет")
                        .setValue(""));

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.STANDARD);

        assertEquals(1, formalizedOffer.getPositionCount());
        assertEquals(0, formalizedOffer.getHypothesesCount());

        FormalizerParam.FormalizedParamPosition position = formalizedOffer.getPosition(0);
        assertEquals(28616160, position.getParamId());
        assertEquals(28616187, position.getValueId());
    }

    @Test
    public void nameAndValueEndOnSpaceTest() throws InterruptedException, MissingKnowledgeTypeException {
        FormalizeOfferRequest request = buildFormalizeOfferRequest(true,
                Offer.YmlParam.newBuilder()
                        .setName("Программы")
                        .setValue("быстрая"),
                Offer.YmlParam.newBuilder()
                        .setName("Цвет\t")
                        .setValue("Природный "));

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.STANDARD);

        assertEquals(1, formalizedOffer.getPositionCount());
        assertEquals(1, formalizedOffer.getHypothesesCount());

        FormalizerParam.FormalizedParamPosition position = formalizedOffer.getPosition(0);
        assertEquals(28616160, position.getParamId());
        assertEquals(28616187, position.getValueId());

        FormalizerParam.FormalizedHypothesisPosition hypothesis = formalizedOffer.getHypotheses(0);
        assertEquals(14871214, hypothesis.getParamId());
        assertEquals("Природный ", hypothesis.getStringValue());
        assertEquals(0, hypothesis.getParamStart());
        assertEquals(4, hypothesis.getParamEnd());
        assertEquals(6, hypothesis.getValueStart());
        assertEquals(17, hypothesis.getValueEnd());
    }

    @NotNull
    private FormalizeOfferRequest buildFormalizeOfferRequest(boolean returnHypotheses,
                                                             Offer.YmlParam.Builder... params) {
        Formalizer.Offer.Builder offer = Formalizer.Offer.newBuilder()
                .setLocale("RU")
                .setCategoryId(CATEGORY_ID)
                .setTitle("Посудомоечная машина");

        for (var param : params) {
            offer.addYmlParam(param);
        }

        Formalizer.FormalizerRequest protoRequest = Formalizer.FormalizerRequest.newBuilder()
                .addOffer(offer)
                .setReturnValueName(true)
                .setReturnParamName(true)
                .setReturnAllPosition(true)
                .setReturnHypotheses(returnHypotheses)
                .build();

        return new FormalizeOfferRequest(offer.build(), protoRequest);
    }
}
