package ru.yandex.market.promoboss.api.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.promoboss.AbstractFunctionalTest;
import ru.yandex.mj.generated.client.self_client.model.CategoryPromoConstraintDto;
import ru.yandex.mj.generated.client.self_client.model.CategoryPromoConstraintDtoCategories;
import ru.yandex.mj.generated.client.self_client.model.GenerateableUrlDto;
import ru.yandex.mj.generated.client.self_client.model.MskuPromoConstraintDto;
import ru.yandex.mj.generated.client.self_client.model.PromoMainRequestParams;
import ru.yandex.mj.generated.client.self_client.model.PromoMainResponseParams;
import ru.yandex.mj.generated.client.self_client.model.PromoRequestV2;
import ru.yandex.mj.generated.client.self_client.model.PromoResponseV2;
import ru.yandex.mj.generated.client.self_client.model.PromoStatus;
import ru.yandex.mj.generated.client.self_client.model.Promotion;
import ru.yandex.mj.generated.client.self_client.model.RegionPromoConstraintDto;
import ru.yandex.mj.generated.client.self_client.model.SrcCifaceDtoV2;
import ru.yandex.mj.generated.client.self_client.model.SupplierPromoConstraintsDto;
import ru.yandex.mj.generated.client.self_client.model.VendorPromoConstraintDto;
import ru.yandex.mj.generated.client.self_client.model.WarehousePromoConstraintDto;

public class AbstractApiTest extends AbstractFunctionalTest {
    protected static PromoRequestV2 PROMO_REQUEST;
    protected static PromoRequestV2 PROMO_REQUEST_WITH_SSKU;
    protected static PromoResponseV2 PROMO_RESPONSE_WITH_SSKU;
    protected static PromoResponseV2 PROMO_RESPONSE;

    @BeforeEach
    void prepareDto() {
        PROMO_REQUEST = new PromoRequestV2()
                .promoId(PROMO_ID)
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .parentPromoId("parent_promo_id")
                                .sourceType(ru.yandex.mj.generated.client.self_client.model.SourceType.CATEGORYIFACE)
                                .name("name")
                                .status(PromoStatus.NEW)
                                .active(true)
                                .hidden(false)
                                .mechanicsType(ru.yandex.mj.generated.client.self_client.model.MechanicsType.CHEAPEST_AS_GIFT)
                                .startAt(startAt)
                                .endAt(endAt)
                                .landingUrl(
                                        new GenerateableUrlDto()
                                                .url("https://landing.url")
                                                .auto(false)
                                )
                                .rulesUrl(
                                        new GenerateableUrlDto()
                                                .url("https://rules.url")
                                                .auto(false)
                                )
                )
                .mechanics(
                        new ru.yandex.mj.generated.client.self_client.model.PromoMechanicsParams()
                                .cheapestAsGift(new ru.yandex.mj.generated.client.self_client.model.CheapestAsGift().count(3))
                )
                .src(
                        new ru.yandex.mj.generated.client.self_client.model.PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                        .author("author_user")
                                        .budgetOwner("TRADE_MARKETING")
                                        .finalBudget(true)
                                        .autoCompensation(false)
                                        .mediaPlanS3Key("mediaPlanS3Key")
                                        .mediaPlanS3FileName("mediaPlanS3FileName")
                                        .compensationTicket("compensationTicket")
                                        .assortmentLoadMethod("assortmentLoadMethod")
                                        .compensationReceiveMethods(List.of(
                                                "compensationReceiveMethod1",
                                                "compensationReceiveMethod2"
                                        ))
                                        .streams(List.of(
                                                "categoryStream1",
                                                "categoryStream2"
                                        ))
                                        .departments(List.of(
                                                "categoryDepartment1",
                                                "categoryDepartment2"
                                        ))
                                        .promotions(List.of(
                                                new Promotion()
                                                        .catteam("catteam01")
                                                        .category("category01")
                                                        .channel("channel01")
                                                        .count(12301L)
                                                        .countUnit("countUnit01")
                                                        .budgetPlan(12401L)
                                                        .budgetFact(12501L)
                                                        .isCustomBudgetPlan(true)
                                                        .comment("comment01"),
                                                new Promotion()
                                                        .catteam("catteam02")
                                                        .category("category02")
                                                        .channel("channel02")
                                                        .count(12302L)
                                                        .countUnit("countUnit02")
                                                        .budgetPlan(12402L)
                                                        .budgetFact(12502L)
                                                        .isCustomBudgetPlan(true)
                                                        .comment("comment02")
                                        ))
                                )
                )
                .suppliersConstraints(new SupplierPromoConstraintsDto()
                        .exclude(true)
                        .suppliers(List.of(123L, 124L, 125L)))
                .warehousesConstraints(new WarehousePromoConstraintDto()
                        .exclude(false)
                        .warehouses(List.of(523L, 524L, 525L)))
                .vendorsConstraints(new VendorPromoConstraintDto()
                        .exclude(false)
                        .vendors(List.of("vendor03", "vendor04", "vendor05")))
                .mskusConstraints(new MskuPromoConstraintDto()
                        .exclude(false)
                        .mskus(List.of(100003L, 100004L, 100005L)))
                .categoriesConstraints(new CategoryPromoConstraintDto()
                        .categories(List.of(
                                new CategoryPromoConstraintDtoCategories().id("cat03").percent(null),
                                new CategoryPromoConstraintDtoCategories().id("cat04").percent(4),
                                new CategoryPromoConstraintDtoCategories().id("cat05").percent(5)
                        ))
                        .excludedCategories(List.of("exclCat03", "exclCat04", "exclCat05"))
                )
                .regionsConstraints(new RegionPromoConstraintDto()
                        .regions(List.of("reg03", "reg04", "reg05"))
                        .excludedRegions(List.of("exclReg03", "exclReg04", "exclReg05"))
                );

        PROMO_REQUEST_WITH_SSKU = new PromoRequestV2()
                .promoId(PROMO_ID)
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .parentPromoId("parent_promo_id")
                                .sourceType(ru.yandex.mj.generated.client.self_client.model.SourceType.CATEGORYIFACE)
                                .name("name")
                                .status(PromoStatus.NEW)
                                .active(true)
                                .hidden(false)
                                .mechanicsType(ru.yandex.mj.generated.client.self_client.model.MechanicsType.CHEAPEST_AS_GIFT)
                                .startAt(startAt)
                                .endAt(endAt)
                )
                .mechanics(
                        new ru.yandex.mj.generated.client.self_client.model.PromoMechanicsParams()
                                .cheapestAsGift(new ru.yandex.mj.generated.client.self_client.model.CheapestAsGift().count(3))
                )
                .ssku(List.copyOf(SSKU))
                .src(
                        new ru.yandex.mj.generated.client.self_client.model.PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                        .author("author_user")
                                        .budgetOwner("TRADE_MARKETING")
                                        .finalBudget(true)
                                        .autoCompensation(false)
                                        .mediaPlanS3Key("mediaPlanS3Key")
                                        .mediaPlanS3FileName("mediaPlanS3FileName")
                                        .compensationTicket("compensationTicket")
                                        .assortmentLoadMethod("assortmentLoadMethod")
                                        .compensationReceiveMethods(List.of(
                                                "compensationReceiveMethod1",
                                                "compensationReceiveMethod2"
                                        ))
                                        .streams(List.of(
                                                "categoryStream1",
                                                "categoryStream2"
                                        ))
                                        .departments(List.of(
                                                "categoryDepartment1",
                                                "categoryDepartment2"
                                        ))
                                        .piPublishedAt(piPublishedAt)
                                )
                );

        PROMO_RESPONSE = new PromoResponseV2()
                .promoId(PROMO_ID)
                .main(
                        new PromoMainResponseParams()
                                .promoKey("promo_key")
                                .parentPromoId("parent_promo_id")
                                .name("name")
                                .status(PromoStatus.NEW)
                                .active(true)
                                .hidden(false)
                                .mechanicsType(ru.yandex.mj.generated.client.self_client.model.MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        new GenerateableUrlDto()
                                                .url("https://landing.url")
                                                .auto(false)
                                )
                                .rulesUrl(
                                        new GenerateableUrlDto()
                                                .url("https://rules.url")
                                                .auto(false)
                                )
                                .startAt(startAt)
                                .endAt(endAt)
                                .sourceType(ru.yandex.mj.generated.client.self_client.model.SourceType.CATEGORYIFACE)
                )
                .mechanics(
                        new ru.yandex.mj.generated.client.self_client.model.PromoMechanicsParams()
                                .cheapestAsGift(new ru.yandex.mj.generated.client.self_client.model.CheapestAsGift().count(3))
                )
                .src(
                        new ru.yandex.mj.generated.client.self_client.model.PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                        .author("author_user")
                                        .budgetOwner("TRADE_MARKETING")
                                        .finalBudget(true)
                                        .autoCompensation(false)
                                        .mediaPlanS3Key("mediaPlanS3Key")
                                        .mediaPlanS3FileName("mediaPlanS3FileName")
                                        .compensationTicket("compensationTicket")
                                        .assortmentLoadMethod("assortmentLoadMethod")
                                        .compensationReceiveMethods(List.of(
                                                "compensationReceiveMethod1",
                                                "compensationReceiveMethod2"
                                        ))
                                        .streams(List.of(
                                                "categoryStream1",
                                                "categoryStream2"
                                        ))
                                        .departments(List.of(
                                                "categoryDepartment1",
                                                "categoryDepartment2"
                                        ))
                                        .piPublishedAt(piPublishedAt)
                                        .promotions(new ArrayList<>(List.of(
                                                new Promotion()
                                                        .id(String.valueOf(111L))
                                                        .catteam("catteam01")
                                                        .category("category01")
                                                        .channel("channel01")
                                                        .count(12301L)
                                                        .countUnit("countUnit01")
                                                        .budgetPlan(12401L)
                                                        .budgetFact(12501L)
                                                        .isCustomBudgetPlan(true)
                                                        .comment("comment01"),
                                                new Promotion()
                                                        .id(String.valueOf(222L))
                                                        .catteam("catteam02")
                                                        .category("category02")
                                                        .channel("channel02")
                                                        .count(12302L)
                                                        .countUnit("countUnit02")
                                                        .budgetPlan(12402L)
                                                        .budgetFact(12502L)
                                                        .isCustomBudgetPlan(true)
                                                        .comment("comment02")
                                        )))
                                )
                )
                .ssku(Collections.emptyList())
                .suppliersConstraints(new SupplierPromoConstraintsDto()
                        .exclude(true)
                        .suppliers(List.of(123L, 124L, 125L))
                )
                .warehousesConstraints(new WarehousePromoConstraintDto()
                        .exclude(false)
                        .warehouses(List.of(523L, 524L, 525L)))
                .vendorsConstraints(new VendorPromoConstraintDto()
                        .exclude(false)
                        .vendors(List.of("vendor03", "vendor04", "vendor05")))
                .mskusConstraints(new MskuPromoConstraintDto()
                        .exclude(false)
                        .mskus(List.of(100003L, 100004L, 100005L)))
                .categoriesConstraints(new CategoryPromoConstraintDto()
                        .categories(List.of(
                                new CategoryPromoConstraintDtoCategories().id("cat03").percent(null),
                                new CategoryPromoConstraintDtoCategories().id("cat04").percent(4),
                                new CategoryPromoConstraintDtoCategories().id("cat05").percent(5)
                        ))
                        .excludedCategories(List.of("exclCat03", "exclCat04", "exclCat05"))
                )
                .regionsConstraints(new RegionPromoConstraintDto()
                        .regions(List.of("reg03", "reg04", "reg05"))
                        .excludedRegions(List.of("exclReg03", "exclReg04", "exclReg05"))
                );

        PROMO_RESPONSE_WITH_SSKU = new PromoResponseV2()
                .promoId(PROMO_ID)
                .main(
                        new PromoMainResponseParams()
                                .promoKey("promo_key")
                                .parentPromoId("parent_promo_id")
                                .name("name")
                                .status(PromoStatus.NEW)
                                .active(true)
                                .hidden(false)
                                .mechanicsType(ru.yandex.mj.generated.client.self_client.model.MechanicsType.CHEAPEST_AS_GIFT)
                                .landingUrl(
                                        new GenerateableUrlDto()
                                                .url("https://landing.url")
                                                .auto(false)
                                )
                                .rulesUrl(
                                        new GenerateableUrlDto()
                                                .url("https://rules.url")
                                                .auto(false)
                                )
                                .startAt(startAt)
                                .endAt(endAt)
                                .sourceType(ru.yandex.mj.generated.client.self_client.model.SourceType.CATEGORYIFACE)
                )
                .mechanics(
                        new ru.yandex.mj.generated.client.self_client.model.PromoMechanicsParams()
                                .cheapestAsGift(new ru.yandex.mj.generated.client.self_client.model.CheapestAsGift().count(3))
                )
                .src(
                        new ru.yandex.mj.generated.client.self_client.model.PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .purpose("purpose")
                                        .compensationSource("compensationSource")
                                        .tradeManager("tradeManager")
                                        .markom("catManager")
                                        .promoKind("promoKind")
                                        .supplierType("supplierType")
                                        .author("author_user")
                                        .budgetOwner("TRADE_MARKETING")
                                        .finalBudget(true)
                                        .autoCompensation(false)
                                        .mediaPlanS3Key("mediaPlanS3Key")
                                        .mediaPlanS3FileName("mediaPlanS3FileName")
                                        .compensationTicket("compensationTicket")
                                        .assortmentLoadMethod("assortmentLoadMethod")
                                        .compensationReceiveMethods(List.of(
                                                "compensationReceiveMethod1",
                                                "compensationReceiveMethod2"
                                        ))
                                        .streams(List.of(
                                                "categoryStream1",
                                                "categoryStream2"
                                        ))
                                        .departments(List.of(
                                                "categoryDepartment1",
                                                "categoryDepartment2"
                                        ))
                                        .piPublishedAt(piPublishedAt)
                                        .promotions(Collections.emptyList())
                                )
                )
                .ssku(new ArrayList<>(SSKU))
                .suppliersConstraints(new SupplierPromoConstraintsDto().suppliers(Collections.emptyList()))
                .warehousesConstraints(new WarehousePromoConstraintDto().warehouses(Collections.emptyList()))
                .vendorsConstraints(new VendorPromoConstraintDto().vendors(Collections.emptyList()))
                .mskusConstraints(new MskuPromoConstraintDto().mskus(Collections.emptyList()))
                .categoriesConstraints(new CategoryPromoConstraintDto()
                        .categories(Collections.emptyList())
                        .excludedCategories(Collections.emptyList())
                )
                .regionsConstraints(new RegionPromoConstraintDto()
                        .regions(Collections.emptyList())
                        .excludedRegions(Collections.emptyList())
                );
    }
}
