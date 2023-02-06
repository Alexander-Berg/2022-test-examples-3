package ru.yandex.market.promoboss.api.v2;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.promoboss.dao.search.SearchField;
import ru.yandex.market.promoboss.model.MechanicsType;
import ru.yandex.market.promoboss.model.Status;
import ru.yandex.market.promoboss.model.search.PromoSearchItem;
import ru.yandex.market.promoboss.service.search.PromoSearchRequest;
import ru.yandex.market.promoboss.service.search.PromoSearchService;
import ru.yandex.market.promoboss.service.search.SortClause;
import ru.yandex.market.promoboss.service.search.SortClause.Direction;
import ru.yandex.mj.generated.client.self_client.api.SearchPromoApiClient;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchRequestDtoV2;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchRequestDtoV2Sort;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchRequestDtoV2SrcCiface;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchResult;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchResultItem;
import ru.yandex.mj.generated.client.self_client.model.PromoSearchResultItemSrcCiface;
import ru.yandex.mj.generated.client.self_client.model.PromoStatus;
import ru.yandex.mj.generated.client.self_client.model.SourceType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.promoboss.dao.search.SearchField.END_AT;
import static ru.yandex.market.promoboss.dao.search.SearchField.MECHANICS_TYPE;
import static ru.yandex.market.promoboss.dao.search.SearchField.NAME;
import static ru.yandex.market.promoboss.dao.search.SearchField.PARENT_PROMO_ID;
import static ru.yandex.market.promoboss.dao.search.SearchField.SOURCE;
import static ru.yandex.market.promoboss.dao.search.SearchField.SRC_CIFACE_ASSORTMENT_LOAD_METHOD;
import static ru.yandex.market.promoboss.dao.search.SearchField.SRC_CIFACE_AUTHOR;
import static ru.yandex.market.promoboss.dao.search.SearchField.SRC_CIFACE_CATEGORY_DEPARTMENT;
import static ru.yandex.market.promoboss.dao.search.SearchField.SRC_CIFACE_COMPENSATION_SOURCE;
import static ru.yandex.market.promoboss.dao.search.SearchField.SRC_CIFACE_FINAL_BUDGET;
import static ru.yandex.market.promoboss.dao.search.SearchField.SRC_CIFACE_PROMO_KIND;
import static ru.yandex.market.promoboss.dao.search.SearchField.SRC_CIFACE_SUPPLIER_TYPE;
import static ru.yandex.market.promoboss.dao.search.SearchField.SRC_CIFACE_TRADE_MANAGER;
import static ru.yandex.market.promoboss.dao.search.SearchField.START_AT;
import static ru.yandex.market.promoboss.dao.search.SearchField.STATUS;
import static ru.yandex.market.promoboss.dao.search.SearchField.UPDATED_AT;
import static ru.yandex.market.promoboss.service.search.Operation.EQUALS;
import static ru.yandex.market.promoboss.service.search.Operation.GTE;
import static ru.yandex.market.promoboss.service.search.Operation.LIKE;
import static ru.yandex.market.promoboss.service.search.Operation.LTE;
import static ru.yandex.market.promoboss.service.search.SearchRequestClause.multiValueClause;
import static ru.yandex.market.promoboss.service.search.SearchRequestClause.singleValueClause;
import static ru.yandex.mj.generated.client.self_client.model.PromoSearchRequestDtoV2Sort.DirectionEnum;
import static ru.yandex.mj.generated.client.self_client.model.PromoSearchRequestDtoV2Sort.FieldEnum;

public class PromoSearchApiTest extends AbstractApiTest {

    private static final Long PROMO_START_AT = OffsetDateTime.of(2022, 6, 25, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();
    private static final Long PROMO_END_AT = OffsetDateTime.of(2023, 6, 25, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();
    private static final Long PROMO_UPDATE_AT = OffsetDateTime.of(2022, 11, 25, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond();

    @MockBean
    PromoSearchService promoSearchService;

    @Autowired
    SearchPromoApiClient promoSearchApiTest;

    @Test
    public void shouldReturnEmptyResult() {
        when(promoSearchService.find(any())).thenReturn(List.of());
        when(promoSearchService.getTotalCount(any())).thenReturn(0);

        PromoSearchRequestDtoV2 PromoSearchRequestDtoV2 = new PromoSearchRequestDtoV2().promoId(List.of("123"));
        PromoSearchResult actualResult = promoSearchApiTest.searchPromoV2(PromoSearchRequestDtoV2).schedule().join();

        assertNotNull(actualResult.getPromos());
        assertEquals(0, actualResult.getPromos().size());
        assertEquals(0, actualResult.getTotalCount());
    }

    @Test
    public void searchWithoutFilters() {
        PromoSearchRequest promoSearchRequest = new PromoSearchRequest();
        when(promoSearchService.find(eq(promoSearchRequest))).thenReturn(List.of(buildPromoSearchItem()));
        when(promoSearchService.getTotalCount(any())).thenReturn(10);

        PromoSearchRequestDtoV2 PromoSearchRequestDtoV2 = new PromoSearchRequestDtoV2();
        PromoSearchResult actualResult = promoSearchApiTest.searchPromoV2(PromoSearchRequestDtoV2).schedule().join();

        assertEquals(buildPromoSearchResultDto(), actualResult);
    }

    @Test
    public void searchWithOneFilter() {
        PromoSearchRequest expectedRequest =
                new PromoSearchRequest().addClause(singleValueClause(
                        EQUALS, SearchField.PROMO_ID, "cf_00000000022"));

        when(promoSearchService.find(eq(expectedRequest))).thenReturn(List.of(buildPromoSearchItem()));
        when(promoSearchService.getTotalCount(any())).thenReturn(10);

        PromoSearchRequestDtoV2 PromoSearchRequestDtoV2 = new PromoSearchRequestDtoV2().promoId(List.of("cf_00000000022"));
        PromoSearchResult actualResult = promoSearchApiTest.searchPromoV2(PromoSearchRequestDtoV2).schedule().join();

        verify(promoSearchService).find(any());

        assertEquals(buildPromoSearchResultDto(), actualResult);
    }

    @Test
    public void searchWithMultiValueFilter() {
        PromoSearchRequest expectedRequest =
                new PromoSearchRequest().addClause(multiValueClause(
                        EQUALS, SearchField.PROMO_ID,
                        List.of("cf_00000000022", "cf_00000000023", "cf_00000000024")));

        when(promoSearchService.find(eq(expectedRequest))).thenReturn(List.of(buildPromoSearchItem()));
        when(promoSearchService.getTotalCount(any())).thenReturn(10);

        PromoSearchRequestDtoV2 PromoSearchRequestDtoV2 =
                new PromoSearchRequestDtoV2().promoId(List.of("cf_00000000022", "cf_00000000023", "cf_00000000024"));
        PromoSearchResult actualResult = promoSearchApiTest.searchPromoV2(PromoSearchRequestDtoV2).schedule().join();

        verify(promoSearchService).find(any());

        assertEquals(buildPromoSearchResultDto(), actualResult);
    }

    @Test
    public void searchWithPagination() {
        PromoSearchRequest expectedRequest =
                new PromoSearchRequest().addClause(singleValueClause(
                                EQUALS, SearchField.PROMO_ID, "cf_00000000022"))
                        .setPagination(5, 6);

        when(promoSearchService.find(eq(expectedRequest))).thenReturn(List.of(buildPromoSearchItem()));
        when(promoSearchService.getTotalCount(any())).thenReturn(10);

        PromoSearchRequestDtoV2 PromoSearchRequestDtoV2 =
                new PromoSearchRequestDtoV2().promoId(List.of("cf_00000000022")).pageNumber(5).pageSize(6);
        PromoSearchResult actualResult = promoSearchApiTest.searchPromoV2(PromoSearchRequestDtoV2).schedule().join();

        verify(promoSearchService).find(any());

        assertEquals(buildPromoSearchResultDto(), actualResult);
    }

    @Test
    public void searchWithSorting() {
        PromoSearchRequest expectedRequest =
                new PromoSearchRequest()
                        .addClause(singleValueClause(EQUALS, SearchField.PROMO_ID, "cf_00000000022"))
                        .addSortClauses(
                                List.of(SortClause.builder().field(NAME).direction(Direction.ASC).build()));

        when(promoSearchService.find(eq(expectedRequest))).thenReturn(List.of(buildPromoSearchItem()));
        when(promoSearchService.getTotalCount(any())).thenReturn(10);

        PromoSearchRequestDtoV2 PromoSearchRequestDtoV2 =
                new PromoSearchRequestDtoV2()
                        .promoId(List.of("cf_00000000022"))
                        .sort(List.of(
                                new PromoSearchRequestDtoV2Sort().field(FieldEnum.NAME).direction(DirectionEnum.ASC)
                        ));

        PromoSearchResult actualResult = promoSearchApiTest.searchPromoV2(PromoSearchRequestDtoV2).schedule().join();

        verify(promoSearchService).find(any());

        assertEquals(buildPromoSearchResultDto(), actualResult);
    }

    @Test
    public void searchWithSeveralSortingFields() {
        PromoSearchRequest expectedRequest =
                new PromoSearchRequest()
                        .addClause(singleValueClause(EQUALS, SearchField.PROMO_ID, "cf_00000000022"))
                        .addSortClauses(
                                List.of(
                                        SortClause.builder().field(NAME).direction(Direction.ASC).build(),
                                        SortClause.builder().field(SearchField.PROMO_ID).direction(Direction.DESC)
                                                .build(),
                                        SortClause.builder().field(START_AT).direction(Direction.ASC).build(),
                                        SortClause.builder().field(END_AT).direction(Direction.DESC).build(),
                                        SortClause.builder().field(UPDATED_AT).direction(Direction.DESC).build()
                                ));

        when(promoSearchService.find(eq(expectedRequest))).thenReturn(List.of(buildPromoSearchItem()));
        when(promoSearchService.getTotalCount(any())).thenReturn(10);

        PromoSearchRequestDtoV2 PromoSearchRequestDtoV2 =
                new PromoSearchRequestDtoV2()
                        .promoId(List.of("cf_00000000022"))
                        .sort(List.of(
                                new PromoSearchRequestDtoV2Sort().field(FieldEnum.NAME).direction(DirectionEnum.ASC),
                                new PromoSearchRequestDtoV2Sort().field(FieldEnum.PROMOID).direction(DirectionEnum.DESC),
                                new PromoSearchRequestDtoV2Sort().field(FieldEnum.STARTAT).direction(DirectionEnum.ASC),
                                new PromoSearchRequestDtoV2Sort().field(FieldEnum.ENDAT).direction(DirectionEnum.DESC),
                                new PromoSearchRequestDtoV2Sort().field(FieldEnum.UPDATEDAT).direction(DirectionEnum.DESC)
                        ));

        PromoSearchResult actualResult = promoSearchApiTest.searchPromoV2(PromoSearchRequestDtoV2).schedule().join();

        verify(promoSearchService).find(any());

        assertEquals(buildPromoSearchResultDto(), actualResult);
    }

    @Test
    public void searchWithWrongSorting() {
        var request = new PromoSearchRequestDtoV2().sort(
                List.of(new PromoSearchRequestDtoV2Sort().field(FieldEnum.NAME)));

        searchWithWrongRequestTest(request);
    }

    @Test
    public void searchWithWrongSorting_duplicatedFields() {
        var request = new PromoSearchRequestDtoV2().sort(
                List.of(new PromoSearchRequestDtoV2Sort().field(FieldEnum.NAME).direction(DirectionEnum.ASC),
                        new PromoSearchRequestDtoV2Sort().field(FieldEnum.NAME).direction(DirectionEnum.DESC)));

        searchWithWrongRequestTest(request);
    }

    @Test
    public void searchWithWrongPagination() {
        searchWithWrongRequestTest(new PromoSearchRequestDtoV2().pageNumber(5));
    }

    @Test
    public void searchWithAllFilter() {
        PromoSearchRequest expectedRequest =
                new PromoSearchRequest()
                        .addClause(singleValueClause(GTE, START_AT, PROMO_START_AT - 1))
                        .addClause(singleValueClause(LTE, START_AT, PROMO_START_AT + 1))
                        .addClause(singleValueClause(GTE, END_AT, PROMO_END_AT - 1))
                        .addClause(singleValueClause(LTE, END_AT, PROMO_END_AT + 1))
                        .addClause(singleValueClause(GTE, UPDATED_AT, PROMO_UPDATE_AT - 1))
                        .addClause(singleValueClause(LTE, UPDATED_AT, PROMO_UPDATE_AT + 1))
                        .addClause(singleValueClause(LIKE, NAME, "Promo"))
                        .addClause(singleValueClause(EQUALS, SRC_CIFACE_FINAL_BUDGET, false))
                        .addClause(multiValueClause(EQUALS, SearchField.PROMO_ID, List.of("cf_22", "cf_23", "cf_24")))
                        .addClause(
                                multiValueClause(EQUALS, MECHANICS_TYPE, List.of("cheapest_as_gift", "generic_bundle")))
                        .addClause(singleValueClause(EQUALS, PARENT_PROMO_ID, "promo id 123"))
                        .addClause(multiValueClause(EQUALS, SRC_CIFACE_PROMO_KIND, List.of("kind1", "kind2", "kind3")))
                        .addClause(multiValueClause(EQUALS, SRC_CIFACE_CATEGORY_DEPARTMENT,
                                List.of("dept1", "dept2", "dept3")))
                        .addClause(multiValueClause(EQUALS, SRC_CIFACE_TRADE_MANAGER,
                                List.of("trade1", "trade2", "trade3")))
                        .addClause(multiValueClause(EQUALS, SRC_CIFACE_SUPPLIER_TYPE, List.of("1p", "3p")))
                        .addClause(multiValueClause(EQUALS, SRC_CIFACE_AUTHOR, List.of("ivan", "petr")))
                        .addClause(singleValueClause(EQUALS, SRC_CIFACE_COMPENSATION_SOURCE, "compensation"))
                        .addClause(multiValueClause(EQUALS, STATUS, List.of("READY", "NEW", "CANCELED")))
                        .addClause(multiValueClause(EQUALS, SOURCE, List.of("AFFILIATE", "PARTNER_SOURCE", "CATEGORYIFACE")))
                        .addClause(multiValueClause(EQUALS, SRC_CIFACE_ASSORTMENT_LOAD_METHOD, List.of("PI", "OTHER_METHOD")));

        when(promoSearchService.find(eq(expectedRequest))).thenReturn(List.of(buildPromoSearchItem()));
        when(promoSearchService.getTotalCount(any())).thenReturn(10);

        PromoSearchRequestDtoV2 PromoSearchRequestDtoV2 = new PromoSearchRequestDtoV2()
                .promoId(List.of("cf_22", "cf_23", "cf_24"))
                .name("Promo")
                .startAtFrom(PROMO_START_AT - 1)
                .startAtTo(PROMO_START_AT + 1)
                .endAtFrom(PROMO_END_AT - 1)
                .endAtTo(PROMO_END_AT + 1)
                .updatedAtFrom(PROMO_UPDATE_AT - 1)
                .updatedAtTo(PROMO_UPDATE_AT + 1)
                .mechanicsType(List.of(ru.yandex.mj.generated.client.self_client.model.MechanicsType.CHEAPEST_AS_GIFT, ru.yandex.mj.generated.client.self_client.model.MechanicsType.GENERIC_BUNDLE))
                .parentPromoId(List.of("promo id 123"))
                .status(List.of(PromoStatus.NEW, PromoStatus.READY, PromoStatus.CANCELED))
                .sourceType(List.of(SourceType.AFFILIATE, SourceType.PARTNER_SOURCE, SourceType.CATEGORYIFACE))
                .srcCiface(new PromoSearchRequestDtoV2SrcCiface()
                        .promoKind(List.of("kind1", "kind2", "kind3"))
                        .department(List.of("dept1", "dept2", "dept3"))
                        .tradeManager(List.of("trade1", "trade2", "trade3"))
                        .supplierType(List.of("1p", "3p"))
                        .compensationSource(List.of("compensation"))
                        .author(List.of("ivan", "petr"))
                        .finalBudget(false)
                        .assortmentLoadMethod(List.of("PI", "OTHER_METHOD"))
                );

        PromoSearchResult actualResult = promoSearchApiTest.searchPromoV2(PromoSearchRequestDtoV2).schedule().join();

        verify(promoSearchService).find(eq(expectedRequest));

        assertEquals(buildPromoSearchResultDto(), actualResult);
    }

    private static PromoSearchItem buildPromoSearchItem() {
        return PromoSearchItem.builder()
                .id(1L)
                .promoId("cf_00000000022")
                .name("Promo test")
                .startAt(PROMO_START_AT)
                .endAt(PROMO_END_AT)
                .updatedAt(PROMO_UPDATE_AT)
                .mechanicsType(MechanicsType.CHEAPEST_AS_GIFT)
                .parentPromoId("promo id 123")
                .status(Status.NEW)
                .srcCifacePromoKind("srcCifacePromoKind")
                .srcCifaceCategoryDepartments(List.of("srcCifaceCategoryDepartment"))
                .srcCifaceTradeManager("srcCifaceTradeManager")
                .srcCifaceSupplierType("srcCifaceSupplierType")
                .srcCifaceCompensationSource("srcCifaceCompensationSource")
                .srcCifaceAuthor("srcCifaceAuthor")
                .srcCifacePromotionBudgetFact(500L)
                .srcCifaceFinalBudget(false)
                .srcCifaceAssortmentLoadMethod("srcCifaceAssortmentLoadMethod")
                .productsCount1p(22)
                .productsCount3p(0)
                .active(true)
                .build();
    }

    private static PromoSearchResult buildPromoSearchResultDto() {
        return new PromoSearchResult().promos(List.of(new PromoSearchResultItem()
                .promoId("cf_00000000022")
                .name("Promo test")
                .startAt(PROMO_START_AT)
                .endAt(PROMO_END_AT)
                .updatedAt(PROMO_UPDATE_AT)
                .mechanicsType(ru.yandex.mj.generated.client.self_client.model.MechanicsType.CHEAPEST_AS_GIFT)
                .parentPromoId("promo id 123")
                .status(Status.NEW.getCode())
                .productsCount1p(22)
                .productsCount3p(0)
                .active(true)
                .srcCiface(new PromoSearchResultItemSrcCiface()
                        .promoKind("srcCifacePromoKind")
                        .departments(List.of("srcCifaceCategoryDepartment"))
                        .tradeManager("srcCifaceTradeManager")
                        .supplierType("srcCifaceSupplierType")
                        .compensationSource("srcCifaceCompensationSource")
                        .author("srcCifaceAuthor")
                        .promotionBudgetFact(500L)
                        .finalBudget(false)
                        .assortmentLoadMethod("srcCifaceAssortmentLoadMethod")
                )
        )).totalCount(10);
    }

    private void searchWithWrongRequestTest(PromoSearchRequestDtoV2 PromoSearchRequestDtoV2) {
        String exceptionMessage = null;
        try {
            promoSearchApiTest.searchPromoV2(PromoSearchRequestDtoV2).schedule().join();
        } catch (Exception e) {
            exceptionMessage = e.getMessage();
        }
        Assertions.assertNotNull(exceptionMessage);
    }
}
