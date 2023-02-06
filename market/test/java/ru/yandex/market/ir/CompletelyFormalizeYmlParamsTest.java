package ru.yandex.market.ir;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import ru.yandex.market.ir.http.ClientType;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.Offer;
import ru.yandex.market.ir.processor.PatternEntriesExtractor;
import ru.yandex.utils.string.DeNullingProcessor;
import ru.yandex.utils.string.indexed.IndexedStringFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CompletelyFormalizeYmlParamsTest {

    private static final int CATEGORY_ID = 90584;

    private DefaultFormalizer formalizer;

    @Before
    public void setup() throws ParserConfigurationException, SAXException, IOException {
        formalizer = new FormalizerBuilder()
                .setCategoryId(CATEGORY_ID)
                .build();
    }

    @Test
    public void completelyFormalizeYmlParamsTest() throws InterruptedException, MissingKnowledgeTypeException {
        formalizer.setFormalizerContractor(buildFormalizerContractor(true));
        FormalizeOfferRequest request = buildFormalizeOfferRequest();

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.STANDARD);

        assertEquals(3, formalizedOffer.getPositionCount());
        final Set<String> params = formalizedOffer.getPositionList()
                .stream()
                .map(p -> p.getParamName() + " " + p.getValueName())
                .collect(Collectors.toSet());
        assertTrue(params.contains("Программы быстрая"));
        assertTrue(params.contains("Программы интенсивная"));
        assertTrue(params.contains("Программы стандартная"));
    }

    @Test
    public void formalizeYmlParamsByNameTest() throws InterruptedException, MissingKnowledgeTypeException {
        formalizer.setFormalizerContractor(buildFormalizerContractor(false));
        FormalizeOfferRequest request = buildFormalizeOfferRequest();

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.STANDARD);

        assertEquals(0, formalizedOffer.getPositionCount());
    }

    @NotNull
    private FormalizeOfferRequest buildFormalizeOfferRequest() {
        Formalizer.Offer offer = Formalizer.Offer.newBuilder()
                .setLocale("RU")
                .setCategoryId(CATEGORY_ID).setTitle(
                        "Посудомоечная машина")
                .addYmlParam(Offer.YmlParam.newBuilder().setName("Стандартные мойки")
                        .setValue("обычная для повседневного мытья, интенсивная для сильнозагрязненной посуды, (" +
                                "быстрый цикл)")
                        .build())
                .build();

        Formalizer.FormalizerRequest protoRequest = Formalizer.FormalizerRequest.newBuilder()
                .addOffer(offer)
                .setReturnValueName(true)
                .setReturnParamName(true)
                .setReturnAllPosition(true)
                .build();

        return new FormalizeOfferRequest(offer, protoRequest);
    }

    private FormalizerContractor buildFormalizerContractor(boolean parseYmlParametersCompletely) {
        PatternEntriesExtractor patternEntriesExtractor = new PatternEntriesExtractor(parseYmlParametersCompletely);
        IndexedStringFactory indexedStringFactory = new IndexedStringFactory();
        indexedStringFactory.setSteam(true);
        indexedStringFactory.setStringProcessor(new DeNullingProcessor());
        patternEntriesExtractor.setIndexedStringFactory(indexedStringFactory);
        FormalizerContractor formalizerContractor = new FormalizerContractor();
        formalizerContractor.setPatternEntriesExtractor(patternEntriesExtractor);
        return formalizerContractor;
    }
}
