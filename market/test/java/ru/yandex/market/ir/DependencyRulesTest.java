package ru.yandex.market.ir;

import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.ir.http.ClientType;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.ir.http.Offer;

import static org.junit.Assert.assertEquals;

public class DependencyRulesTest {

    private static final int CATEGORY_ID = 90584;

    private DefaultFormalizer formalizer;

    @Before
    public void setUp() throws Exception {
        formalizer = new FormalizerBuilder()
                .setCategoryId(CATEGORY_ID)
//                .setCategoryPath("/home/marbok/arc/arcadia/market/ir/formalizer/src/test/resources")
                .build();
    }

    @Test
    public void ruleTest() throws MissingKnowledgeTypeException, InterruptedException {
        FormalizeOfferRequest request = buildFormalizeOfferRequest();

        Formalizer.FormalizedOffer formalizedOffer = formalizer.formalizeOffer(request, ClientType.STANDARD);

        assertEquals(2, formalizedOffer.getPositionCount());

        Map<Integer, FormalizerParam.FormalizedParamPosition> formalizedParam = formalizedOffer.getPositionList()
                .stream()
                .collect(Collectors.toMap(FormalizerParam.FormalizedParamPosition::getParamId, pos -> pos));

        assertEquals(0, formalizedParam.get(4922812).getRuleId());
        assertEquals(28627905, formalizedParam.get(28616334).getRuleId());
    }

    @NotNull
    private FormalizeOfferRequest buildFormalizeOfferRequest() {
        Formalizer.Offer offer = Formalizer.Offer.newBuilder()
                .setLocale("RU")
                .setCategoryId(CATEGORY_ID)
                .setTitle("Посудомоечная машина")
                .addYmlParam(Offer.YmlParam.newBuilder()
                        .setName("Тип защиты от протечек")
                        .setValue("полная"))
                .build();

        Formalizer.FormalizerRequest protoRequest = Formalizer.FormalizerRequest.newBuilder()
                .addOffer(offer)
                .setReturnValueName(true)
                .setReturnParamName(true)
                .setReturnAllPosition(true)
                .setApplyRules(true)
                .build();

        return new FormalizeOfferRequest(offer, protoRequest);
    }
}
