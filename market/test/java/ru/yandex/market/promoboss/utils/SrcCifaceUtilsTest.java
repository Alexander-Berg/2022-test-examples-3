package ru.yandex.market.promoboss.utils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.promoboss.model.CifaceMultipleProperty;
import ru.yandex.market.promoboss.model.CifaceMultipleValue;
import ru.yandex.market.promoboss.model.CifacePromo;
import ru.yandex.market.promoboss.model.CifacePromotion;
import ru.yandex.market.promoboss.model.SrcCiface;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.PromoSrcParams;
import ru.yandex.mj.generated.server.model.Promotion;
import ru.yandex.mj.generated.server.model.SrcCifaceDtoV2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.promoboss.utils.SrcCifaceUtils.srcCifaceFromPromoRequest;

public class SrcCifaceUtilsTest {
    private static final CifacePromo CIFACE_PROMO = CifacePromo.builder()
            .promoPurpose("promoPurpose")
            .compensationSource("compensationSource")
            .tradeManager("tradeManager")
            .markom("markom")
            .promoKind("promoKind")
            .supplierType("supplierType")
            .author("author")
            .budgetOwner("budgetOwner")
            .mediaPlanS3Key("mediaPlanS3Key")
            .mediaPlanS3FileName("mediaPlanS3FileName")
            .assortmentLoadMethod("assortmentLoadMethod")
            .piPublishedAt(LocalDateTime.of(2022, 1, 1, 1, 1, 1, 1).toEpochSecond(ZoneOffset.UTC))
            .build();

    private static final List<CifaceMultipleValue> MULTIPLE_VALUES = List.of(
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.COMPENSATION_RECEIVE_METHOD)
                    .stringValue("compensationReceiveMethod1")
                    .build(),
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.COMPENSATION_RECEIVE_METHOD)
                    .stringValue("compensationReceiveMethod2")
                    .build(),
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.CATEGORY_STREAM)
                    .stringValue("stream1")
                    .build(),
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.CATEGORY_STREAM)
                    .stringValue("stream2")
                    .build(),
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                    .stringValue("department1")
                    .build(),
            CifaceMultipleValue.builder()
                    .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                    .stringValue("department2")
                    .build()
    );

    private static final List<CifacePromotion> CIFACE_PROMOTIONS = List.of(
            CifacePromotion.builder()
                    .id(1L)
                    .catteam("catteam")
                    .category("category")
                    .channel("channel")
                    .count(123L)
                    .countUnit("countUnit")
                    .budgetPlan(123L)
                    .budgetFact(123L)
                    .isCustomBudgetPlan(false)
                    .comment("comment")
                    .build()
    );

    private static final PromoRequestV2 PromoRequest = new PromoRequestV2()
            .src(
                    new PromoSrcParams()
                            .ciface(new SrcCifaceDtoV2()
                                    .streams(List.of("stream1", "stream2"))
                                    .departments(List.of("department1", "department2"))
                                    .compensationReceiveMethods(List.of("compensationReceiveMethod1",
                                            "compensationReceiveMethod2"))
                                    .purpose("promoPurpose")
                                    .compensationSource("compensationSource")
                                    .tradeManager("tradeManager")
                                    .markom("markom")
                                    .promoKind("promoKind")
                                    .supplierType("supplierType")
                                    .author("author")
                                    .budgetOwner("budgetOwner")
                                    .mediaPlanS3Key("mediaPlanS3Key")
                                    .mediaPlanS3FileName("mediaPlanS3FileName")
                                    .assortmentLoadMethod("assortmentLoadMethod")
                                    .piPublishedAt(OffsetDateTime.of(2022, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC).toEpochSecond())
                                    .promotions(List.of(
                                            new Promotion()
                                                    .id("1")
                                                    .catteam("catteam")
                                                    .category("category")
                                                    .channel("channel")
                                                    .count(123L)
                                                    .countUnit("countUnit")
                                                    .budgetPlan(123L)
                                                    .budgetFact(123L)
                                                    .isCustomBudgetPlan(false)
                                                    .comment("comment")
                                    ))
                            ));

    @Test
    void srcCifaceFromPromoRequestTest() {
        var expected = SrcCiface.builder()
                .cifacePromo(CIFACE_PROMO)
                .multipleProperties(MULTIPLE_VALUES)
                .cifacePromotions(CIFACE_PROMOTIONS)
                .build();

        assertEquals(expected, srcCifaceFromPromoRequest(PromoRequest.getSrc().getCiface()));
    }
}
