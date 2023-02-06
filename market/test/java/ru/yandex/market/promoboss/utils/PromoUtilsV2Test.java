package ru.yandex.market.promoboss.utils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.promoboss.model.CifaceMultipleProperty;
import ru.yandex.market.promoboss.model.CifaceMultipleValue;
import ru.yandex.market.promoboss.model.CifacePromo;
import ru.yandex.market.promoboss.model.CifacePromotion;
import ru.yandex.market.promoboss.model.Constraints;
import ru.yandex.market.promoboss.model.GenerateableUrl;
import ru.yandex.market.promoboss.model.MechanicsType;
import ru.yandex.market.promoboss.model.Promo;
import ru.yandex.market.promoboss.model.PromoMainParams;
import ru.yandex.market.promoboss.model.PromoMechanicsParams;
import ru.yandex.market.promoboss.model.PromoSrcParams;
import ru.yandex.market.promoboss.model.SourceType;
import ru.yandex.market.promoboss.model.SrcCiface;
import ru.yandex.market.promoboss.model.Status;
import ru.yandex.market.promoboss.model.mechanics.CheapestAsGift;
import ru.yandex.market.promoboss.model.mechanics.Promocode;
import ru.yandex.market.promoboss.model.mechanics.PromocodeType;
import ru.yandex.market.promoboss.model.postgres.CategoryConstraintDto;
import ru.yandex.market.promoboss.model.postgres.SupplierConstraintDto;
import ru.yandex.market.promoboss.utils.exception.ConvertException;
import ru.yandex.mj.generated.server.model.CategoryPromoConstraintDto;
import ru.yandex.mj.generated.server.model.GenerateableUrlDto;
import ru.yandex.mj.generated.server.model.MskuPromoConstraintDto;
import ru.yandex.mj.generated.server.model.PromoMainRequestParams;
import ru.yandex.mj.generated.server.model.PromoMainResponseParams;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.PromoResponseV2;
import ru.yandex.mj.generated.server.model.PromoStatus;
import ru.yandex.mj.generated.server.model.RegionPromoConstraintDto;
import ru.yandex.mj.generated.server.model.SrcCifaceDtoV2;
import ru.yandex.mj.generated.server.model.SupplierPromoConstraintsDto;
import ru.yandex.mj.generated.server.model.VendorPromoConstraintDto;
import ru.yandex.mj.generated.server.model.WarehousePromoConstraintDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class PromoUtilsV2Test {

    protected final Long startAt = OffsetDateTime.of(2022, 1, 1, 1, 1, 1, 0, ZoneOffset.UTC).toEpochSecond();
    protected final Long endAt = OffsetDateTime.of(2023, 2, 2, 2, 2, 2, 0, ZoneOffset.UTC).toEpochSecond();

    protected Promo buildPromo() {
        return Promo.builder()
                .promoId("cf_123")
                .mainParams(
                        PromoMainParams.builder()
                                .parentPromoId("parent_promo_id")
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url("https://landing.url")
                                                .auto(false)
                                                .build()
                                )
                                .rulesUrl(GenerateableUrl.builder()
                                        .url("https://rules.url")
                                        .auto(false)
                                        .build()
                                )
                                .source(SourceType.CATEGORYIFACE)
                                .name("new_name")
                                .status(Status.NEW)
                                .active(true)
                                .hidden(false)
                                .startAt(startAt)
                                .endAt(endAt)
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(new CheapestAsGift(3))
                                .promocode(Promocode.builder()
                                        .codeType(PromocodeType.FIXED_DISCOUNT)
                                        .value(1)
                                        .code("code")
                                        .minCartPrice(11L)
                                        .maxCartPrice(22L)
                                        .applyMultipleTimes(true)
                                        .additionalConditions("additionalConditions")
                                        .build())
                                .build()
                )
                .ssku(Set.of("ssku1"))
                .srcParams(
                        PromoSrcParams.builder()
                                .ciface(SrcCiface.builder()
                                        .cifacePromo(CifacePromo.builder()
                                                .promoPurpose("purpose")
                                                .compensationSource("compensationSource")
                                                .tradeManager("tradeManager")
                                                .markom("catManager")
                                                .promoKind("promoType")
                                                .supplierType("supplierType")
                                                .mediaPlanS3Key("mediaPlanS3Key")
                                                .mediaPlanS3FileName("mediaPlanS3FileName")
                                                .build())
                                        .multipleProperties(
                                                List.of(
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
                                                )
                                        )
                                        .cifacePromotions(Collections.emptyList())
                                        .build())
                                .build()
                )
                .constraints(Constraints.builder()
                        .suppliers(List.of(
                                SupplierConstraintDto.builder().supplierId(123L).exclude(false).build(),
                                SupplierConstraintDto.builder().supplierId(124L).exclude(false).build()
                        ))
                        .build())
                .build();
    }

    protected Promo buildPromoSeveralExcludeValues() {
        return Promo.builder()
                .promoId("cf_123")
                .mainParams(
                        PromoMainParams.builder()
                                .promoKey("promo_key")
                                .parentPromoId("parent_promo_id")
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url("https://landing.url")
                                                .auto(false)
                                                .build()
                                )
                                .rulesUrl(GenerateableUrl.builder()
                                        .url("https://rules.url")
                                        .auto(false)
                                        .build()
                                )
                                .source(SourceType.CATEGORYIFACE)
                                .name("new_name")
                                .status(Status.NEW)
                                .active(true)
                                .hidden(false)
                                .startAt(startAt)
                                .endAt(endAt)
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(new CheapestAsGift(3))
                                .build()
                )
                .ssku(Set.of("ssku1"))
                .srcParams(
                        PromoSrcParams.builder()
                                .ciface(SrcCiface.builder()
                                        .cifacePromo(CifacePromo.builder()
                                                .promoPurpose("purpose")
                                                .compensationSource("compensationSource")
                                                .tradeManager("tradeManager")
                                                .markom("catManager")
                                                .promoKind("promoType")
                                                .supplierType("supplierType")
                                                .mediaPlanS3Key("mediaPlanS3Key")
                                                .mediaPlanS3FileName("mediaPlanS3FileName")
                                                .build())
                                        .cifacePromotions(List.of(
                                                CifacePromotion.builder()
                                                        .catteam("catteam")
                                                        .category("category")
                                                        .channel("channel")
                                                        .count(123L)
                                                        .countUnit(null)
                                                        .budgetPlan(124L)
                                                        .budgetFact(125L)
                                                        .isCustomBudgetPlan(false)
                                                        .comment("comment")
                                                        .build()
                                        ))
                                        .build())
                                .build()
                )
                .constraints(Constraints.builder()
                        .suppliers(List.of(
                                SupplierConstraintDto.builder().supplierId(123L).exclude(true).build(),
                                SupplierConstraintDto.builder().supplierId(124L).exclude(false).build()
                        ))
                        .build())
                .build();
    }

    protected Promo buildPromoExcludeCategoryPercent() {
        return Promo.builder()
                .promoId("cf_123")
                .mainParams(
                        PromoMainParams.builder()
                                .promoKey("promo_key")
                                .parentPromoId("parent_promo_id")
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        GenerateableUrl.builder()
                                                .url("https://landing.url")
                                                .auto(false)
                                                .build()
                                )
                                .rulesUrl(GenerateableUrl.builder()
                                        .url("https://rules.url")
                                        .auto(false)
                                        .build()
                                )
                                .source(SourceType.CATEGORYIFACE)
                                .name("new_name")
                                .status(Status.NEW)
                                .active(true)
                                .hidden(false)
                                .startAt(startAt)
                                .endAt(endAt)
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(new CheapestAsGift(3))
                                .build()
                )
                .ssku(Set.of("ssku1"))
                .srcParams(
                        PromoSrcParams.builder()
                                .ciface(SrcCiface.builder()
                                        .cifacePromo(CifacePromo.builder()
                                                .promoPurpose("purpose")
                                                .compensationSource("compensationSource")
                                                .tradeManager("tradeManager")
                                                .markom("catManager")
                                                .promoKind("promoType")
                                                .supplierType("supplierType")
                                                .mediaPlanS3Key("mediaPlanS3Key")
                                                .mediaPlanS3FileName("mediaPlanS3FileName")
                                                .build())
                                        .cifacePromotions(List.of(
                                                CifacePromotion.builder()
                                                        .catteam("catteam")
                                                        .category("category")
                                                        .channel("channel")
                                                        .count(123L)
                                                        .countUnit(null)
                                                        .budgetPlan(124L)
                                                        .budgetFact(125L)
                                                        .isCustomBudgetPlan(false)
                                                        .comment("comment")
                                                        .build()
                                        ))
                                        .build())
                                .build()
                )
                .constraints(Constraints.builder()
                        .categories(List.of(
                                CategoryConstraintDto.builder().categoryId("123").exclude(true).percent(100).build()
                        ))
                        .build())
                .build();
    }

    @Test
    public void shouldThrowExceptionOnSeveralExcludeValues() {
        Promo promo = buildPromoSeveralExcludeValues();
        ConvertException exception = assertThrows(ConvertException.class, () -> PromoUtils.toPromoResponseV2(promo));
        assertEquals("Only one value 'exclude' is allowed", exception.getMessage());
    }

    @Test
    public void shouldThrowExceptionOnExcludeCategoryPercent() {
        Promo promo = buildPromoExcludeCategoryPercent();
        ConvertException exception = assertThrows(ConvertException.class, () -> PromoUtils.toPromoResponseV2(promo));
        assertEquals("Excluded category contains percent", exception.getMessage());
    }

    private PromoRequestV2 buildPromoRequest() {
        return new PromoRequestV2()
                .promoId("cf_123")
                .main(
                        new PromoMainRequestParams()
                                .parentPromoId("parent_promo_id")
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT.getApiValue())
                                .landingUrl(new GenerateableUrlDto()
                                        .url("https://landing.url")
                                        .auto(false)
                                )
                                .rulesUrl(new GenerateableUrlDto()
                                        .url("https://rules.url")
                                        .auto(false)
                                )
                                .sourceType(ru.yandex.mj.generated.server.model.SourceType.CATEGORYIFACE)
                                .name("new_name")
                                .status(PromoStatus.NEW)
                                .active(true)
                                .hidden(false)
                                .startAt(startAt)
                                .endAt(endAt)
                )
                .mechanics(
                        new ru.yandex.mj.generated.server.model.PromoMechanicsParams()
                                .cheapestAsGift(new ru.yandex.mj.generated.server.model.CheapestAsGift()
                                        .count(3)
                                )
                                .promocode(new ru.yandex.mj.generated.server.model.Promocode()
                                        .codeType(ru.yandex.mj.generated.server.model.Promocode.CodeTypeEnum.FIXED_DISCOUNT)
                                        .value(1)
                                        .code("code")
                                        .minCartPrice(11L)
                                        .maxCartPrice(22L)
                                        .applyMultipleTimes(true)
                                        .additionalConditions("additionalConditions")
                                )
                )
                .ssku(List.of("ssku1"))
                .src(
                        new ru.yandex.mj.generated.server.model.PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .streams(List.of("stream1", "stream2"))
                                        .departments(List.of("department1", "department2"))
                                        .compensationReceiveMethods(List.of("compensationReceiveMethod1",
                                                "compensationReceiveMethod2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoType")
                                        .supplierType("supplierType")
                                        .mediaPlanS3Key("mediaPlanS3Key")
                                        .mediaPlanS3FileName("mediaPlanS3FileName")
                                        .promotions(Collections.emptyList())
                                )
                )
                .suppliersConstraints(new SupplierPromoConstraintsDto()
                        .suppliers(List.of(123L, 124L))
                        .exclude(false)
                );
    }

    private PromoResponseV2 buildPromoResponse() {
        return new PromoResponseV2()
                .promoId("cf_123")
                .main(
                        new PromoMainResponseParams()
                                .parentPromoId("parent_promo_id")
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT.getApiValue())
                                .landingUrl(new GenerateableUrlDto()
                                        .url("https://landing.url")
                                        .auto(false)
                                )
                                .rulesUrl(new GenerateableUrlDto()
                                        .url("https://rules.url")
                                        .auto(false)
                                )
                                .name("new_name")
                                .status(PromoStatus.NEW)
                                .active(true)
                                .hidden(false)
                                .startAt(startAt)
                                .endAt(endAt)
                                .sourceType(ru.yandex.mj.generated.server.model.SourceType.CATEGORYIFACE)
                )
                .mechanics(
                        new ru.yandex.mj.generated.server.model.PromoMechanicsParams()
                                .cheapestAsGift(new ru.yandex.mj.generated.server.model.CheapestAsGift()
                                        .count(3)
                                )
                                .promocode(new ru.yandex.mj.generated.server.model.Promocode()
                                        .codeType(ru.yandex.mj.generated.server.model.Promocode.CodeTypeEnum.FIXED_DISCOUNT)
                                        .value(1)
                                        .code("code")
                                        .minCartPrice(11L)
                                        .maxCartPrice(22L)
                                        .applyMultipleTimes(true)
                                        .additionalConditions("additionalConditions")
                                )
                )
                .ssku(List.of("ssku1"))
                .src(
                        new ru.yandex.mj.generated.server.model.PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .streams(List.of("stream1", "stream2"))
                                        .departments(List.of("department1", "department2"))
                                        .compensationReceiveMethods(List.of("compensationReceiveMethod1", "compensationReceiveMethod2"))
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoType")
                                        .supplierType("supplierType")
                                        .mediaPlanS3Key("mediaPlanS3Key")
                                        .mediaPlanS3FileName("mediaPlanS3FileName")
                                        .promotions(Collections.emptyList())
                                )
                )
                .suppliersConstraints(new SupplierPromoConstraintsDto()
                        .suppliers(List.of(123L, 124L))
                        .exclude(false)
                )
                .mskusConstraints(new MskuPromoConstraintDto().mskus(Collections.emptyList()))
                .categoriesConstraints(new CategoryPromoConstraintDto().categories(Collections.emptyList()).excludedCategories(Collections.emptyList()))
                .regionsConstraints(new RegionPromoConstraintDto().regions(Collections.emptyList()).excludedRegions(Collections.emptyList()))
                .vendorsConstraints(new VendorPromoConstraintDto().vendors(Collections.emptyList()))
                .warehousesConstraints(new WarehousePromoConstraintDto().warehouses(Collections.emptyList()));
    }

    @Test
    void fromPromoRequest_ok() {
        var expected = buildPromo();
        var actual = PromoUtils.fromPromoRequestV2(buildPromoRequest());
        assertEquals(expected, actual);
    }

    @Test
    void fromPromoRequestWithoutUrls_ok() {
        var request = buildPromoRequest();
        request.getMain()
                .landingUrl(null)
                .rulesUrl(null);

        var expected = buildPromo();
        expected.getMainParams().setLandingUrl(GenerateableUrl.builder().build());
        expected.getMainParams().setRulesUrl(GenerateableUrl.builder().build());

        var actual = PromoUtils.fromPromoRequestV2(request);
        assertEquals(expected, actual);
    }

    @Test
    void toPromoResponse_ok() {
        var expected = buildPromoResponse();
        var actual = PromoUtils.toPromoResponseV2(buildPromo());
        assertEquals(expected, actual);
    }
}
