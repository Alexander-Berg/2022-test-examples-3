package ru.yandex.market.promoboss;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.javaframework.main.config.SpringApplicationConfig;
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
import ru.yandex.market.promoboss.model.postgres.CategoryConstraintDto;
import ru.yandex.market.promoboss.model.postgres.MskuConstraintDto;
import ru.yandex.market.promoboss.model.postgres.RegionConstraintDto;
import ru.yandex.market.promoboss.model.postgres.SupplierConstraintDto;
import ru.yandex.market.promoboss.model.postgres.VendorConstraintDto;
import ru.yandex.market.promoboss.model.postgres.WarehouseConstraintDto;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = {
                SpringApplicationConfig.class
        }
)
@TestPropertySource(
        {"classpath:test_properties/yt_test.properties", "classpath:test_properties/application.properties"})
public abstract class AbstractFunctionalTest {

    protected static final String PROMO_ID = "cf_123";
    protected static final Set<String> SSKU = Set.of("ssku1", "ssku2");
    protected static final Long startAt = Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond();
    protected static final Long endAt = Instant.now().plus(2, ChronoUnit.HOURS).getEpochSecond();
    protected static final Long piPublishedAt = Instant.now().plus(1, ChronoUnit.HOURS).getEpochSecond();

    protected static Promo PROMO;
    protected static Promo PROMO_WITH_SSKU;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void prepare() {
        PROMO = Promo.builder()
                .promoId(PROMO_ID)
                .mainParams(
                        PromoMainParams.builder()
                                .promoKey("promo_key")
                                .parentPromoId("parent_promo_id")
                                .source(SourceType.CATEGORYIFACE)
                                .name("name")
                                .status(Status.NEW)
                                .active(true)
                                .hidden(false)
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
                                .startAt(startAt)
                                .endAt(endAt)
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(new CheapestAsGift(3))
                                .build()
                )
                .srcParams(
                        PromoSrcParams.builder()
                                .ciface(
                                        SrcCiface.builder()
                                                .cifacePromo(CifacePromo.builder()
                                                        .promoPurpose("purpose")
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
                                                        .piPublishedAt(piPublishedAt)
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
                                                                        .stringValue("categoryStream1")
                                                                        .build(),
                                                                CifaceMultipleValue.builder()
                                                                        .property(CifaceMultipleProperty.CATEGORY_STREAM)
                                                                        .stringValue("categoryStream2")
                                                                        .build(),
                                                                CifaceMultipleValue.builder()
                                                                        .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                                                                        .stringValue("categoryDepartment1")
                                                                        .build(),
                                                                CifaceMultipleValue.builder()
                                                                        .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                                                                        .stringValue("categoryDepartment2")
                                                                        .build()

                                                        )
                                                )
                                                .cifacePromotions(List.of(
                                                        CifacePromotion.builder()
                                                                .id(111L)
                                                                .catteam("catteam01")
                                                                .category("category01")
                                                                .channel("channel01")
                                                                .count(12301L)
                                                                .countUnit("countUnit01")
                                                                .budgetPlan(12401L)
                                                                .budgetFact(12501L)
                                                                .isCustomBudgetPlan(true)
                                                                .comment("comment01")
                                                                .build(),
                                                        CifacePromotion.builder()
                                                                .id(222L)
                                                                .catteam("catteam02")
                                                                .category("category02")
                                                                .channel("channel02")
                                                                .count(12302L)
                                                                .countUnit("countUnit02")
                                                                .budgetPlan(12402L)
                                                                .budgetFact(12502L)
                                                                .isCustomBudgetPlan(true)
                                                                .comment("comment02")
                                                                .build()
                                                ))
                                                .build()
                                )
                                .build()
                )
                .ssku(Collections.emptySet())
                .constraints(Constraints.builder()
                        .suppliers(List.of(
                                SupplierConstraintDto.builder().supplierId(123L).exclude(true).build(),
                                SupplierConstraintDto.builder().supplierId(124L).exclude(true).build(),
                                SupplierConstraintDto.builder().supplierId(125L).exclude(true).build()
                        ))
                        .warehouses(List.of(
                                WarehouseConstraintDto.builder().warehouseId(523L).exclude(false).build(),
                                WarehouseConstraintDto.builder().warehouseId(524L).exclude(false).build(),
                                WarehouseConstraintDto.builder().warehouseId(525L).exclude(false).build()
                        ))
                        .vendors(List.of(
                                VendorConstraintDto.builder().vendorId("vendor03").exclude(false).build(),
                                VendorConstraintDto.builder().vendorId("vendor04").exclude(false).build(),
                                VendorConstraintDto.builder().vendorId("vendor05").exclude(false).build()
                        ))
                        .mskus(List.of(
                                MskuConstraintDto.builder().mskuId(100003L).exclude(false).build(),
                                MskuConstraintDto.builder().mskuId(100004L).exclude(false).build(),
                                MskuConstraintDto.builder().mskuId(100005L).exclude(false).build()
                        ))
                        .categories(List.of(
                                CategoryConstraintDto.builder().categoryId("cat03").exclude(false).percent(null)
                                        .build(),
                                CategoryConstraintDto.builder().categoryId("cat04").exclude(false).percent(4).build(),
                                CategoryConstraintDto.builder().categoryId("cat05").exclude(false).percent(5).build(),
                                CategoryConstraintDto.builder().categoryId("exclCat03").exclude(true).build(),
                                CategoryConstraintDto.builder().categoryId("exclCat04").exclude(true).build(),
                                CategoryConstraintDto.builder().categoryId("exclCat05").exclude(true).build()
                        ))
                        .regions(List.of(
                                RegionConstraintDto.builder().regionId("reg03").exclude(false).build(),
                                RegionConstraintDto.builder().regionId("reg04").exclude(false).build(),
                                RegionConstraintDto.builder().regionId("reg05").exclude(false).build(),
                                RegionConstraintDto.builder().regionId("exclReg03").exclude(true).build(),
                                RegionConstraintDto.builder().regionId("exclReg04").exclude(true).build(),
                                RegionConstraintDto.builder().regionId("exclReg05").exclude(true).build()
                        ))
                        .build())
                .build();

        PROMO_WITH_SSKU = Promo.builder()
                .promoId(PROMO_ID)
                .mainParams(
                        PromoMainParams.builder()
                                .promoKey("promo_key")
                                .parentPromoId("parent_promo_id")
                                .source(SourceType.CATEGORYIFACE)
                                .name("name")
                                .status(Status.NEW)
                                .active(true)
                                .hidden(false)
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
                                .startAt(startAt)
                                .endAt(endAt)
                                .build()
                )
                .mechanicsParams(
                        PromoMechanicsParams.builder()
                                .cheapestAsGift(new CheapestAsGift(3))
                                .build()
                )
                .srcParams(
                        PromoSrcParams.builder()
                                .ciface(SrcCiface.builder()
                                        .cifacePromo(CifacePromo.builder()
                                                .promoPurpose("purpose")
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
                                                .piPublishedAt(piPublishedAt)
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
                                                                .stringValue("categoryStream1")
                                                                .build(),
                                                        CifaceMultipleValue.builder()
                                                                .property(CifaceMultipleProperty.CATEGORY_STREAM)
                                                                .stringValue("categoryStream2")
                                                                .build(),
                                                        CifaceMultipleValue.builder()
                                                                .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                                                                .stringValue("categoryDepartment1")
                                                                .build(),
                                                        CifaceMultipleValue.builder()
                                                                .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                                                                .stringValue("categoryDepartment2")
                                                                .build()

                                                )
                                        )
                                        .cifacePromotions(Collections.emptyList())
                                        .build())
                                .build()
                )
                .ssku(SSKU)
                .build();
    }
}

