package ru.yandex.market.promoboss.service.search;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.promoboss.AbstractDBFunctionalTest;
import ru.yandex.market.promoboss.dao.search.SearchField;
import ru.yandex.market.promoboss.model.CifaceMultipleProperty;
import ru.yandex.market.promoboss.model.CifaceMultipleValue;
import ru.yandex.market.promoboss.model.CifacePromo;
import ru.yandex.market.promoboss.model.CifacePromotion;
import ru.yandex.market.promoboss.model.Constraints;
import ru.yandex.market.promoboss.model.GenerateableUrl;
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
import ru.yandex.market.promoboss.utils.PromoFieldUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.promoboss.service.search.SearchRequestClause.multiValueClause;
import static ru.yandex.market.promoboss.service.search.SearchRequestClause.singleValueClause;

public class PromoSearchServiceTest extends AbstractDBFunctionalTest {
    protected static final String PROMO_ID = "cf_123";
    protected static final Long startAt = 456L;
    protected static final Long endAt = 789L;
    protected static final Long updatedAt = 321L;
    protected static final Long piPublishedAt = 456L;
    public static final long START_AT = 456L;
    public static final long END_AT = 789L;
    protected static Promo PROMO = Promo.builder()
            //region init promo
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
                                                    .compensationSource("PARTNER")
                                                    .tradeManager("tradeManager")
                                                    .markom("catManager")
                                                    .promoKind("promoKind")
                                                    .supplierType("1P")
                                                    .author("author")
                                                    .budgetOwner("TRADE_MARKETING")
                                                    .finalBudget(true)
                                                    .autoCompensation(false)
                                                    .mediaPlanS3Key("mediaPlanS3Key")
                                                    .mediaPlanS3FileName("mediaPlanS3FileName")
                                                    .compensationTicket("compensationTicket")
                                                    .assortmentLoadMethod("TRACKER")
                                                    .piPublishedAt(piPublishedAt)
                                                    .build())
                                            .multipleProperties(
                                                    List.of(
                                                            CifaceMultipleValue.builder()
                                                                    .property(
                                                                            CifaceMultipleProperty.COMPENSATION_RECEIVE_METHOD)
                                                                    .stringValue("compensationReceiveMethod1")
                                                                    .build(),
                                                            CifaceMultipleValue.builder()
                                                                    .property(
                                                                            CifaceMultipleProperty.COMPENSATION_RECEIVE_METHOD)
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
                                                                    .property(
                                                                            CifaceMultipleProperty.CATEGORY_DEPARTMENT)
                                                                    .stringValue("categoryDepartment1")
                                                                    .build(),
                                                            CifaceMultipleValue.builder()
                                                                    .property(
                                                                            CifaceMultipleProperty.CATEGORY_DEPARTMENT)
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
                    .suppliers(Collections.emptyList())
                    .warehouses(Collections.emptyList())
                    .vendors(Collections.emptyList())
                    .mskus(Collections.emptyList())
                    .categories(Collections.emptyList())
                    .regions(Collections.emptyList())
                    .build())
            .build();
    //endregion

    @Autowired
    private PromoSearchService promoSearchService;

    @Test
    @DbUnitDataSet(
            before = "PromoSearchServiceTest.addPromoToSearch.before.csv",
            after = "PromoSearchServiceTest.addPromoToSearch.after.csv")
    public void addPromoToSearch() {
        Long promoId = 10L;
        Set<PromoField> modifiedFields = PromoFieldUtils.getAll();
        PromoEvent promoEvent = new PromoEvent(promoId, updatedAt);

        promoSearchService.addPromoToSearch(promoId, modifiedFields, PROMO, promoEvent);
    }

    @Test
    @DbUnitDataSet(
            before = "PromoSearchServiceTest.updatePromoInSearch.before.csv",
            after = "PromoSearchServiceTest.updatePromoInSearch.after.csv")
    public void updatePromoInSearch() {
        Long promoId = 10L;
        Set<PromoField> modifiedFields = PromoFieldUtils.getAll();
        PromoEvent promoEvent = new PromoEvent(promoId, updatedAt);

        promoSearchService.updatePromoInSearch(promoId, modifiedFields, PROMO, promoEvent);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchByOneField() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.PROMO_ID, "cf_123"));

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchByMultipleFields() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.PROMO_ID, "cf_123"))
                .addClause(singleValueClause(Operation.EQUALS, SearchField.PARENT_PROMO_ID, "parent_promo_id"))
                .addClause(singleValueClause(Operation.EQUALS, SearchField.SRC_CIFACE_AUTHOR, "author"));

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchInSeveralValues() {
        var request = new PromoSearchRequest()
                .addClause(multiValueClause(Operation.EQUALS, SearchField.PROMO_ID,
                        Set.of("cf_123", "cf_124", "cf_125")));

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchByDates() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.LT, SearchField.START_AT, 457L));

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchByFullName() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.LIKE, SearchField.NAME, "name"));

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchByPartOfName() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.LIKE, SearchField.NAME, "ame"));

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchWithDifferentOperators() {
        var request = new PromoSearchRequest()
                .addClause(
                        multiValueClause(Operation.EQUALS, SearchField.PROMO_ID, Set.of("cf_123", "cf_124", "cf_125")))
                .addClause(singleValueClause(Operation.LIKE, SearchField.NAME, "ame"))
                .addClause(singleValueClause(Operation.EQUALS, SearchField.SRC_CIFACE_AUTHOR, "author"))
                .addClause(singleValueClause(Operation.GTE, SearchField.START_AT, START_AT - 5))
                .addClause(singleValueClause(Operation.LTE, SearchField.END_AT, END_AT + 5));

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchByMultipleProperty() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.SRC_CIFACE_CATEGORY_DEPARTMENT,
                        "categoryDepartment1"));

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchByMultiplePropertyInSeveralValues() {
        var request = new PromoSearchRequest()
                .addClause(multiValueClause(Operation.EQUALS, SearchField.SRC_CIFACE_CATEGORY_DEPARTMENT,
                        List.of("categoryDepartment1", "categoryDepartment2")));

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchByMultiplePropertyZeroResult() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.SRC_CIFACE_CATEGORY_DEPARTMENT,
                        "categoryDepartment"));

        List<PromoSearchItem> actualResult = promoSearchService.find(request);
        assertEquals(0, actualResult.size());
    }

    @Test
    public void searchWithZeroResult() {
        var request = new PromoSearchRequest()
                .addClause(
                        multiValueClause(Operation.EQUALS, SearchField.PROMO_ID, Set.of("cf_123", "cf_124", "cf_125")))
                .addClause(singleValueClause(Operation.LIKE, SearchField.NAME, "ame"))
                .addClause(singleValueClause(Operation.EQUALS, SearchField.SRC_CIFACE_AUTHOR, "author"))
                .addClause(singleValueClause(Operation.GTE, SearchField.START_AT, START_AT - 5))
                .addClause(singleValueClause(Operation.LTE, SearchField.END_AT, END_AT + 5));


        List<PromoSearchItem> actualResult = promoSearchService.find(request);
        assertEquals(0, actualResult.size());
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchWithPagination() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.PROMO_ID, "cf_123"))
                .setPagination(1, 10);

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void searchWithPaginationAndGetEmptyResult() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.PROMO_ID, "cf_123"))
                .setPagination(2, 10);

        List<PromoSearchItem> actualResult = promoSearchService.find(request);
        assertEquals(0, actualResult.size());
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchBySeveralPromos.csv")
    public void searchWithPaginationAndGetSecondPage() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.STATUS, Status.NEW.name()))
                .setPagination(3, 2);

        List<PromoSearchItem> actualResult = promoSearchService.find(request);
        assertEquals(2, actualResult.size());
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    void searchWithSortingByOneField() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.PROMO_ID, "cf_123"))
                .addSortClauses(List.of(SortClause.builder()
                        .field(SearchField.PROMO_ID)
                        .direction(SortClause.Direction.ASC)
                        .build()));

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    void searchWithSortingBySeveralFields() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.PROMO_ID, "cf_123"))
                .addSortClauses(List.of(
                        SortClause.builder().field(SearchField.PROMO_ID).direction(SortClause.Direction.ASC).build(),
                        SortClause.builder().field(SearchField.NAME).direction(SortClause.Direction.DESC).build()
                ));

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    void searchWithComplexRequest() {
        var request = new PromoSearchRequest()
                .addClause(
                        multiValueClause(Operation.EQUALS, SearchField.PROMO_ID, Set.of("cf_123", "cf_124", "cf_125")))
                .addClause(singleValueClause(Operation.LIKE, SearchField.NAME, "ame"))
                .addClause(singleValueClause(Operation.EQUALS, SearchField.SRC_CIFACE_AUTHOR, "author"))
                .addClause(singleValueClause(Operation.GTE, SearchField.START_AT, START_AT - 5))
                .addClause(singleValueClause(Operation.LTE, SearchField.END_AT, END_AT + 5))
                .addClause(singleValueClause(Operation.EQUALS, SearchField.SRC_CIFACE_CATEGORY_DEPARTMENT,
                        "categoryDepartment1"))
                .addSortClauses(List.of(
                        SortClause.builder().field(SearchField.PROMO_ID).direction(SortClause.Direction.ASC).build(),
                        SortClause.builder().field(SearchField.NAME).direction(SortClause.Direction.DESC).build()
                ))
                .setPagination(1, 10);

        baseSearchTest(request);
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    void getTotalCountTest() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.PROMO_ID, "cf_123"));

        assertEquals(1, promoSearchService.getTotalCount(request));
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchBySeveralPromos.csv")
    public void getTotalCountAndIgnorePaginationTest() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.STATUS, Status.NEW.name()))
                .setPagination(2, 10);

        assertEquals(6, promoSearchService.getTotalCount(request));
    }

    @Test
    @DbUnitDataSet(before = "PromoSearchServiceTest.searchByOnePromo.csv")
    public void getTotalCountAndIgnoreSortingTest() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.STATUS, Status.NEW.name()))
                .addSortClauses(List.of(
                        SortClause.builder().field(SearchField.PROMO_ID).direction(SortClause.Direction.ASC).build(),
                        SortClause.builder().field(SearchField.NAME).direction(SortClause.Direction.DESC).build()
                ));

        assertEquals(1, promoSearchService.getTotalCount(request));
    }

    @Test
    public void getTotalCountTestWithZeroResult() {
        var request = new PromoSearchRequest()
                .addClause(singleValueClause(Operation.EQUALS, SearchField.STATUS, Status.NEW.name()));

        assertEquals(0, promoSearchService.getTotalCount(request));
    }

    private void baseSearchTest(PromoSearchRequest request) {
        List<PromoSearchItem> actualResult = promoSearchService.find(request);
        List<PromoSearchItem> expectedResult = buildPromoSearchResult();
        assertEquals(expectedResult, actualResult);
    }

    private List<PromoSearchItem> buildPromoSearchResult() {
        return List.of(PromoSearchItem.builder()
                .promoId("cf_123")
                .name("name")
                .startAt(START_AT)
                .endAt(END_AT)
                .updatedAt(321L)
                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                .parentPromoId("parent_promo_id")
                .status(Status.NEW)
                .srcCifacePromoKind("promoKind")
                .srcCifacePromotionBudgetFact(25003L)
                .srcCifaceAuthor("author")
                .srcCifaceCompensationSource("PARTNER")
                .srcCifaceSupplierType("1P")
                .srcCifaceTradeManager("tradeManager")
                .srcCifaceCategoryDepartments(List.of("categoryDepartment1", "categoryDepartment2"))
                .srcCifaceAssortmentLoadMethod("TRACKER")
                .srcCifaceFinalBudget(true)
                .active(true)
                .source(SourceType.CATEGORYIFACE)
                .productsCount1p(0)
                .productsCount3p(0)
                .build());
    }
}
