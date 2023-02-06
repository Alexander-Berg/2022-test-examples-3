package ru.yandex.market.promoboss.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.promoboss.model.CifaceMultipleProperty;
import ru.yandex.market.promoboss.model.CifaceMultipleValue;
import ru.yandex.market.promoboss.model.CifacePromo;
import ru.yandex.market.promoboss.model.CifacePromotion;
import ru.yandex.market.promoboss.model.Constraints;
import ru.yandex.market.promoboss.model.MechanicsType;
import ru.yandex.market.promoboss.model.Promo;
import ru.yandex.market.promoboss.model.PromoEvent;
import ru.yandex.market.promoboss.model.PromoField;
import ru.yandex.market.promoboss.model.PromoMainParams;
import ru.yandex.market.promoboss.model.PromoMechanicsParams;
import ru.yandex.market.promoboss.model.PromoSrcParams;
import ru.yandex.market.promoboss.model.SourceType;
import ru.yandex.market.promoboss.model.SrcCiface;
import ru.yandex.market.promoboss.model.Status;
import ru.yandex.market.promoboss.model.mechanics.CheapestAsGift;
import ru.yandex.market.promoboss.model.search.PromoSearchItem;
import ru.yandex.market.promoboss.model.postgres.SupplierConstraintDto;
import ru.yandex.market.promoboss.model.postgres.WarehouseConstraintDto;
import ru.yandex.market.promoboss.utils.PromoFieldUtilsTest;

import static ru.yandex.market.promoboss.utils.PromoSearchUtils.fillSearchItem;

public class PromoSearchUtilsTest {
    private static final Long ID = 1000L;
    private static final String PROMO_ID = "cf_123";
    private static final Long START_DATE = OffsetDateTime.of(2022, 5, 10, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();
    private static final Long END_DATE = OffsetDateTime.of(2022, 10, 10, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();
    private static final Long EVENT_DATE = LocalDateTime.of(2022, 8, 10, 0, 0, 0, 0).toEpochSecond(ZoneOffset.UTC);


    @ParameterizedTest
    @MethodSource("getFinalBudget")
    public void createSearchItem(Boolean finalBudget) {
        PromoSearchItem searchItem = PromoSearchItem.builder().id(ID).build();
        fillSearchItem(searchItem, buildPromo(finalBudget), PromoFieldUtilsTest.createAll(), PromoEvent.create(ID,
                EVENT_DATE));

        PromoSearchItem expectedSearchItem = PromoSearchItem.builder()
                .id(ID)
                .promoId(PROMO_ID)
                .parentPromoId("parent_promo_id")
                .startAt(START_DATE)
                .endAt(END_DATE)
                .updatedAt(EVENT_DATE)
                .name("promo")
                .status(Status.NEW)
                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                .srcCifaceCategoryDepartments(List.of("categoryDepartment"))
                .srcCifaceCompensationSource("compensationSource")
                .srcCifaceTradeManager("tradeManager")
                .srcCifacePromoKind("promoKind")
                .srcCifaceSupplierType("supplierType")
                .srcCifaceAuthor("sergey")
                .srcCifacePromotionBudgetFact(300L)
                .srcCifaceFinalBudget(finalBudget)
                .productsCount1p(2)
                .productsCount3p(0)
                .active(false)
                .source(SourceType.CATEGORYIFACE)
                .srcCifaceAssortmentLoadMethod("PI")
                .build();

        Assertions.assertEquals(expectedSearchItem, searchItem);
    }

    @ParameterizedTest
    @MethodSource("getFinalBudget")
    public void createPromoSearchItemWithEmptyBudgetFact(Boolean finalBudget) {
        var promo = buildPromo(finalBudget);
        promo.getSrcParams().getCiface().getCifacePromotions().add(CifacePromotion.builder()
                .catteam("catteam")
                .category("category")
                .channel("channel")
                .count(123L)
                .countUnit(null)
                .budgetPlan(124L)
                .budgetFact(null)
                .isCustomBudgetPlan(false)
                .comment("comment")
                .build());

        PromoSearchItem searchItem = PromoSearchItem.builder().id(ID).build();
        fillSearchItem(searchItem, promo, PromoFieldUtilsTest.createAll(), PromoEvent.create(ID, EVENT_DATE));

        PromoSearchItem expectedSearchItem = PromoSearchItem.builder()
                .id(ID)
                .promoId(PROMO_ID)
                .parentPromoId("parent_promo_id")
                .startAt(START_DATE)
                .endAt(END_DATE)
                .updatedAt(EVENT_DATE)
                .name("promo")
                .status(Status.NEW)
                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                .srcCifaceCategoryDepartments(List.of("categoryDepartment"))
                .srcCifaceCompensationSource("compensationSource")
                .srcCifaceTradeManager("tradeManager")
                .srcCifacePromoKind("promoKind")
                .srcCifaceSupplierType("supplierType")
                .srcCifaceAuthor("sergey")
                .srcCifacePromotionBudgetFact(300L)
                .srcCifaceFinalBudget(finalBudget)
                .productsCount1p(2)
                .productsCount3p(0)
                .active(false)
                .source(SourceType.CATEGORYIFACE)
                .srcCifaceAssortmentLoadMethod("PI")
                .build();

        Assertions.assertEquals(expectedSearchItem, searchItem);
    }

    @Test
    public void createPromoSearchItemWithoutCiface() {
        PromoSearchItem searchItem = PromoSearchItem.builder().id(ID).build();
        Set<PromoField> promoFields = PromoFieldUtilsTest.createAll();
        promoFields.remove(PromoField.SRC);
        fillSearchItem(searchItem, buildPromo(true), promoFields, PromoEvent.create(ID, EVENT_DATE));

        PromoSearchItem expectedSearchItem = PromoSearchItem.builder()
                .id(ID)
                .promoId(PROMO_ID)
                .parentPromoId("parent_promo_id")
                .startAt(START_DATE)
                .endAt(END_DATE)
                .updatedAt(EVENT_DATE)
                .name("promo")
                .status(Status.NEW)
                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                .productsCount1p(2)
                .productsCount3p(0)
                .active(false)
                .source(SourceType.CATEGORYIFACE)
                .build();

        Assertions.assertEquals(expectedSearchItem, searchItem);
    }

    @Test
    public void updatePromoSearchItemWithoutCiface() {
        PromoSearchItem searchItem = PromoSearchItem.builder()
                //region searchItem init
                .id(ID)
                .promoId(PROMO_ID)
                .parentPromoId("old_parent_promo_id")
                .startAt(END_DATE)
                .endAt(START_DATE)
                .updatedAt(EVENT_DATE)
                .name("old_promo")
                .status(Status.CANCELED)
                .mechanicsType(MechanicsType.BLUE_FLASH)
                .srcCifaceCategoryDepartments(List.of("old_categoryDepartment"))
                .srcCifaceCompensationSource("old_compensationSource")
                .srcCifaceTradeManager("old_tradeManager")
                .srcCifacePromoKind("old_promoKind")
                .srcCifaceSupplierType("old_supplierType")
                .srcCifaceAuthor("old_sergey")
                .srcCifacePromotionBudgetFact(3000L)
                .srcCifaceFinalBudget(null)
                .productsCount1p(22)
                .productsCount3p(20)
                .active(true)
                .source(SourceType.ANAPLAN)
                .srcCifaceAssortmentLoadMethod("PI")
                .build();
        //endregion

        Set<PromoField> promoFields = PromoFieldUtilsTest.createAll();
        promoFields.remove(PromoField.SRC);
        fillSearchItem(searchItem, buildPromo(true), promoFields, PromoEvent.create(ID, EVENT_DATE));

        PromoSearchItem expectedSearchItem = PromoSearchItem.builder()
                //region expectedSearchItem init
                .id(ID)
                .promoId(PROMO_ID)
                .parentPromoId("parent_promo_id")
                .startAt(START_DATE)
                .endAt(END_DATE)
                .updatedAt(EVENT_DATE)
                .name("promo")
                .status(Status.NEW)
                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                .srcCifaceCategoryDepartments(List.of("old_categoryDepartment"))
                .srcCifaceCompensationSource("old_compensationSource")
                .srcCifaceTradeManager("old_tradeManager")
                .srcCifacePromoKind("old_promoKind")
                .srcCifaceSupplierType("old_supplierType")
                .srcCifaceAuthor("old_sergey")
                .srcCifacePromotionBudgetFact(3000L)
                .srcCifaceFinalBudget(null)
                .productsCount1p(2)
                .productsCount3p(0)
                .active(false)
                .source(SourceType.CATEGORYIFACE)
                .srcCifaceAssortmentLoadMethod("PI")
                .build();
        //endregion

        Assertions.assertEquals(expectedSearchItem, searchItem);
    }

    private Promo buildPromo(Boolean finalBudget) {
        CifacePromo cifacePromo = CifacePromo.builder()
                .author("sergey")
                .promoPurpose("promoPurpose")
                .compensationSource("compensationSource")
                .tradeManager("tradeManager")
                .markom("catManager")
                .promoKind("promoKind")
                .supplierType("supplierType")
                .finalBudget(finalBudget)
                .autoCompensation(false)
                .assortmentLoadMethod("PI")
                .build();
        List<CifaceMultipleValue> multipleProperties = List.of(
                CifaceMultipleValue.builder()
                        .property(CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                        .stringValue("categoryDepartment")
                        .build()
        );
        List<CifacePromotion> cifacePromotions = new ArrayList<>() {
            {
                add(CifacePromotion.builder()
                        .catteam("catteam")
                        .category("category")
                        .channel("channel")
                        .count(123L)
                        .countUnit(null)
                        .budgetPlan(124L)
                        .budgetFact(130L)
                        .isCustomBudgetPlan(false)
                        .comment("comment")
                        .build());
                add(CifacePromotion.builder()
                        .catteam("catteam")
                        .category("category")
                        .channel("channel")
                        .count(123L)
                        .countUnit(null)
                        .budgetPlan(124L)
                        .budgetFact(170L)
                        .isCustomBudgetPlan(false)
                        .comment("comment")
                        .build());
            }
        };
        Constraints constraints = Constraints.builder()
                .suppliers(List.of(
                        SupplierConstraintDto.builder().supplierId(123L).exclude(false).build(),
                        SupplierConstraintDto.builder().supplierId(124L).exclude(false).build(),
                        SupplierConstraintDto.builder().supplierId(125L).exclude(false).build()
                ))
                .warehouses(List.of(
                        WarehouseConstraintDto.builder().warehouseId(523L).exclude(true).build(),
                        WarehouseConstraintDto.builder().warehouseId(524L).exclude(true).build(),
                        WarehouseConstraintDto.builder().warehouseId(525L).exclude(true).build()
                ))
                .build();
        return Promo.builder()
                .promoId(PROMO_ID)
                .mainParams(
                        PromoMainParams.builder()
                                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                                .status(Status.NEW)
                                .startAt(START_DATE)
                                .endAt(END_DATE)
                                .name("promo")
                                .parentPromoId("parent_promo_id")
                                .active(false)
                                .source(SourceType.CATEGORYIFACE)
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
                                        .cifacePromo(cifacePromo)
                                        .multipleProperties(multipleProperties)
                                        .cifacePromotions(cifacePromotions)
                                        .build())
                                .build()
                )
                .ssku(Set.of("ssku1", "ssku2"))
                .constraints(constraints)
                .build();
    }

    private static Stream<Boolean> getFinalBudget() {
        return Stream.of(null, true, false);
    }
}
