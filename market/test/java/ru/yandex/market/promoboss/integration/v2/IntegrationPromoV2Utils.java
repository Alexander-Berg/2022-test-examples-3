package ru.yandex.market.promoboss.integration.v2;

import java.util.List;

import ru.yandex.market.promoboss.integration.IntegrationPromoUtils;
import ru.yandex.market.promoboss.model.MechanicsType;
import ru.yandex.market.promoboss.model.SourceType;
import ru.yandex.mj.generated.server.model.CategoryPromoConstraintDto;
import ru.yandex.mj.generated.server.model.CategoryPromoConstraintDtoCategories;
import ru.yandex.mj.generated.server.model.CheapestAsGift;
import ru.yandex.mj.generated.server.model.GenerateableUrlDto;
import ru.yandex.mj.generated.server.model.MskuPromoConstraintDto;
import ru.yandex.mj.generated.server.model.PromoMainRequestParams;
import ru.yandex.mj.generated.server.model.PromoMainResponseParams;
import ru.yandex.mj.generated.server.model.PromoMechanicsParams;
import ru.yandex.mj.generated.server.model.PromoRequestV2;
import ru.yandex.mj.generated.server.model.PromoResponseV2;
import ru.yandex.mj.generated.server.model.PromoSrcParams;
import ru.yandex.mj.generated.server.model.Promotion;
import ru.yandex.mj.generated.server.model.RegionPromoConstraintDto;
import ru.yandex.mj.generated.server.model.SrcCifaceDtoV2;
import ru.yandex.mj.generated.server.model.PromoStatus;
import ru.yandex.mj.generated.server.model.SupplierPromoConstraintsDto;
import ru.yandex.mj.generated.server.model.VendorPromoConstraintDto;
import ru.yandex.mj.generated.server.model.WarehousePromoConstraintDto;

public class IntegrationPromoV2Utils {
    public static PromoRequestV2 buildCreatePromoRequest() {
        return new PromoRequestV2()
                .promoId(IntegrationPromoUtils.PROMO_ID)
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(ru.yandex.market.promoboss.model.SourceType.CATEGORYIFACE.getApiValue())
                                .active(true)
                                .endAt(1658437140L)
                                .hidden(false)
                                .landingUrl(new GenerateableUrlDto()
                                        .auto(true)
                                        .url("https://market.yandex.ru/special/spread-discount-count-landing?shopPromoId=cf_104547")
                                )
                                .rulesUrl(new GenerateableUrlDto()
                                        .auto(true)
                                        .url("https://market.yandex.ru/special/spread-discount-count?shopPromoId=cf_104547")
                                )
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT.getApiValue())
                                .name("2022-06-06-15-16")
                                .parentPromoId("SP#200")
                                .startAt(1657486800L)
                                .status(PromoStatus.NEW)
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(
                                        new CheapestAsGift()
                                                .count(2)
                                )
                )
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .assortmentLoadMethod("TRACKER")
                                        .author("author")
                                        .budgetOwner("TRADE_MARKETING")
                                        .compensationSource("PARTNER")
                                        .finalBudget(false)
                                        .markom("markom")
                                        .piPublishedAt(1654517852L)
                                        .promoKind("NATIONAL")
                                        .purpose("GMV_GENERATION")
                                        .supplierType("1P")
                                        .tradeManager("tradeManager")
                                        .streams(List.of("categoryStream1", "categoryStream2"))
                                        .departments(List.of("FMCG", "CEHAC"))
                                        .compensationReceiveMethods(List.of("compensationReceiveMethod1", "compensationReceiveMethod2"))
                                        .promotions(List.of(
                                                new Promotion()
                                                        .budgetPlan(150000L)
                                                        .budgetFact(140000L)
                                                        .category("Медийное размещение Главная")
                                                        .catteam("DiY")
                                                        .channel("Главная страница. Растяжка 500 тыс. показов")
                                                        .count(1L)
                                                        .countUnit("нед")
                                                        .isCustomBudgetPlan(false)
                                        ))
                                )
                )
                .ssku(List.of("101", "102", "103"))
                .regionsConstraints(new RegionPromoConstraintDto()
                        .regions(List.of("31", "32", "33"))
                        .excludedRegions(List.of("34", "35", "36"))
                )
                .categoriesConstraints(new CategoryPromoConstraintDto()
                        .categories(List.of(
                                new CategoryPromoConstraintDtoCategories()
                                        .id("11")
                                        .percent(91),
                                new CategoryPromoConstraintDtoCategories()
                                        .id("12")
                                        .percent(92),
                                new CategoryPromoConstraintDtoCategories()
                                        .id("13")
                                        .percent(93)
                        ))
                        .excludedCategories(List.of(
                                "14",
                                "15",
                                "16"
                        ))
                )
                .mskusConstraints(
                        new MskuPromoConstraintDto()
                                .mskus(List.of(21L, 22L, 23L))
                                .exclude(false)
                )
                .suppliersConstraints(
                        new SupplierPromoConstraintsDto()
                                .suppliers(List.of(41L, 42L, 43L))
                                .exclude(false)
                )
                .vendorsConstraints(
                        new VendorPromoConstraintDto()
                                .vendors(List.of("51", "52", "53"))
                                .exclude(false)
                )
                .warehousesConstraints(
                        new WarehousePromoConstraintDto()
                                .warehouses(List.of(61L, 62L, 63L))
                                .exclude(false)
                );
    }

    public static PromoRequestV2 buildUpdatePromoRequest() {
        return new PromoRequestV2()
                .promoId(IntegrationPromoUtils.PROMO_ID)
                .modifiedBy("modifiedBy")
                .main(
                        new PromoMainRequestParams()
                                .sourceType(SourceType.CATEGORYIFACE.getApiValue())
                                .active(false)
                                .endAt(1658438140L)
                                .hidden(true)
                                .landingUrl(new GenerateableUrlDto()
                                        .auto(false)
                                        .url("https://market.yandex.ru/special/cheapest-as-gift-1-2-landing?shopPromoId=cf_104547-1")
                                )
                                .rulesUrl(new GenerateableUrlDto()
                                        .auto(false)
                                        .url("https://market.yandex.ru/special/cheapest-as-gift?shopPromoId=cf_104547-1")
                                )
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT.getApiValue())
                                .name("2022-06-06-15-16-1")
                                .parentPromoId("SP#201")
                                .startAt(1657487800L)
                                .status(PromoStatus.READY)
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(
                                        new CheapestAsGift()
                                                .count(3)
                                )
                )
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .assortmentLoadMethod("TRACKER-1")
                                        .author("author-1")
                                        .budgetOwner("TRADE_MARKETING-1")
                                        .compensationReceiveMethods(List.of("ON_INVOICE_FOR_1P-1", "ON_INVOICE_FOR_3P-1"))
                                        .compensationSource("PARTNER-1")
                                        .finalBudget(true)
                                        .markom("markom-1")
                                        .piPublishedAt(1654517952L)
                                        .promoKind("NATIONAL-1")
                                        .purpose("GMV_GENERATION-1")
                                        .supplierType("1P-1")
                                        .tradeManager("tradeManager-1")
                                        .streams(List.of("categoryStream1-1", "categoryStream2-1"))
                                        .departments(List.of("FMCG-1", "CEHAC-1"))
                                        .compensationReceiveMethods(List.of("compensationReceiveMethod1-1", "compensationReceiveMethod2-1"))
                                        .promotions(List.of(
                                                new Promotion()
                                                        .budgetPlan(150001L)
                                                        .budgetFact(140001L)
                                                        .category("Медийное размещение Главная-1")
                                                        .catteam("DiY-1")
                                                        .channel("Главная страница. Растяжка 500 тыс. показов-1")
                                                        .count(2L)
                                                        .countUnit("нед-1")
                                                        .isCustomBudgetPlan(true)
                                                        .id(null)
                                        ))
                                )
                )
                .ssku(List.of("101", "104"))
                .regionsConstraints(new RegionPromoConstraintDto()
                        .regions(List.of("31", "34", "37"))
                        .excludedRegions(List.of("33", "35", "38"))
                )
                .categoriesConstraints(new CategoryPromoConstraintDto()
                        .categories(List.of(
                                new CategoryPromoConstraintDtoCategories()
                                        .id("11")
                                        .percent(81),
                                new CategoryPromoConstraintDtoCategories()
                                        .id("15")
                                        .percent(85),
                                new CategoryPromoConstraintDtoCategories()
                                        .id("18")
                                        .percent(88)
                        ))
                        .excludedCategories(List.of(
                                "12",
                                "14",
                                "17"
                        ))
                )
                .mskusConstraints(
                        new MskuPromoConstraintDto()
                                .mskus(List.of(21L, 24L))
                                .exclude(false)
                )
                .suppliersConstraints(
                        new SupplierPromoConstraintsDto()
                                .suppliers(List.of(41L, 44L))
                                .exclude(false)
                )
                .vendorsConstraints(
                        new VendorPromoConstraintDto()
                                .vendors(List.of("51", "54"))
                                .exclude(false)
                )
                .warehousesConstraints(
                        new WarehousePromoConstraintDto()
                                .warehouses(List.of(61L, 64L))
                                .exclude(false)
                );
    }

    public static PromoResponseV2 buildPromoResponse() {
        return new PromoResponseV2()
                .promoId(IntegrationPromoUtils.PROMO_ID)
                .main(
                        new PromoMainResponseParams()
                                .active(false)
                                .createdAt(1654517851L)
                                .updatedAt(1654617851L)
                                .endAt(1658438140L)
                                .hidden(true)
                                .landingUrl(new GenerateableUrlDto()
                                        .auto(false)
                                        .url("https://market.yandex.ru/special/cheapest-as-gift-1-2-landing?shopPromoId=cf_104547-1")
                                )
                                .rulesUrl(new GenerateableUrlDto()
                                        .auto(false)
                                        .url("https://market.yandex.ru/special/cheapest-as-gift?shopPromoId=cf_104547-1")
                                )
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT.getApiValue())
                                .name("2022-06-06-15-16-1")
                                .parentPromoId("SP#201")
                                .promoKey("GUszXD49w7DuZxuhWsMopw")
                                .startAt(1657487800L)
                                .status(PromoStatus.READY)
                                .sourceType(ru.yandex.mj.generated.server.model.SourceType.CATEGORYIFACE)
                )
                .mechanics(
                        new PromoMechanicsParams()
                                .cheapestAsGift(
                                        new CheapestAsGift()
                                                .count(3)
                                )
                )
                .src(
                        new PromoSrcParams()
                                .ciface(new SrcCifaceDtoV2()
                                        .assortmentLoadMethod("TRACKER-1")
                                        .author("author-1")
                                        .budgetOwner("TRADE_MARKETING-1")
                                        .compensationReceiveMethods(List.of("compensationReceiveMethod1-1", "compensationReceiveMethod2-1"))
                                        .compensationSource("PARTNER-1")
                                        .finalBudget(true)
                                        .markom("markom-1")
                                        .piPublishedAt(1654517952L)
                                        .promoKind("NATIONAL-1")
                                        .purpose("GMV_GENERATION-1")
                                        .supplierType("1P-1")
                                        .tradeManager("tradeManager-1")
                                        .streams(List.of("categoryStream1-1", "categoryStream2-1"))
                                        .departments(List.of("FMCG-1", "CEHAC-1"))
                                        .promotions(List.of(
                                                new Promotion()
                                                        .budgetPlan(150001L)
                                                        .budgetFact(140001L)
                                                        .category("Медийное размещение Главная-1")
                                                        .catteam("DiY-1")
                                                        .channel("Главная страница. Растяжка 500 тыс. показов-1")
                                                        .count(2L)
                                                        .countUnit("нед-1")
                                                        .id("1")
                                                        .isCustomBudgetPlan(true)
                                        ))
                                )
                )
                .ssku(List.of("101", "104"))
                .regionsConstraints(new RegionPromoConstraintDto()
                        .regions(List.of("31", "34", "37"))
                        .excludedRegions(List.of("33", "35", "38"))
                )
                .categoriesConstraints(new CategoryPromoConstraintDto()
                        .categories(List.of(
                                new CategoryPromoConstraintDtoCategories()
                                        .id("11")
                                        .percent(81),
                                new CategoryPromoConstraintDtoCategories()
                                        .id("15")
                                        .percent(85),
                                new CategoryPromoConstraintDtoCategories()
                                        .id("18")
                                        .percent(88)
                        ))
                        .excludedCategories(List.of(
                                "12",
                                "14",
                                "17"
                        ))
                )
                .mskusConstraints(
                        new MskuPromoConstraintDto()
                                .mskus(List.of(21L, 24L))
                                .exclude(false)
                )
                .suppliersConstraints(
                        new SupplierPromoConstraintsDto()
                                .suppliers(List.of(41L, 44L))
                                .exclude(false)
                )
                .vendorsConstraints(
                        new VendorPromoConstraintDto()
                                .vendors(List.of("51", "54"))
                                .exclude(false)
                )
                .warehousesConstraints(
                        new WarehousePromoConstraintDto()
                                .warehouses(List.of(61L, 64L))
                                .exclude(false)
                );
    }
}
