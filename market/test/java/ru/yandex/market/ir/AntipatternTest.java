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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AntipatternTest {
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
        FormalizeOfferRequest request = buildFormalizeOfferRequest();

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.STANDARD);

        assertEquals(2, formalizedOffer.getPositionCount());
        final Set<String> params = formalizedOffer.getPositionList()
                .stream()
                .map(p -> p.getParamName() + " " + p.getValueName())
                .collect(Collectors.toSet());
        assertTrue(params.contains("Программы быстрая"));
        assertTrue(params.contains("Индикация на полу нет"));
    }

    @NotNull
    private FormalizeOfferRequest buildFormalizeOfferRequest() {
        Formalizer.Offer offer = Formalizer.Offer.newBuilder()
                .setLocale("RU")
                .setCategoryId(CATEGORY_ID)
                .setTitle("Посудомоечная машина, режим половинной загрузки нет")
                .setDescription("бережная: нет")
                .addYmlParam(Offer.YmlParam.newBuilder().setName("Программы")
                        .setValue("быстрая"))
                .addYmlParam(Offer.YmlParam.newBuilder().setName("интенсивная")
                        .setValue("нет"))
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
