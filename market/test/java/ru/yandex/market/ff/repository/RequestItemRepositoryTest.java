package ru.yandex.market.ff.repository;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.bo.ItemCount;
import ru.yandex.market.ff.model.bo.ItemsWrongCountInfo;
import ru.yandex.market.ff.model.bo.LastSuppliedItemsFilter;
import ru.yandex.market.ff.model.dto.RequestItemsDetailsDTO;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.entity.Identifier;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.RequestItemError;
import ru.yandex.market.ff.model.enums.IdentifierType;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Интеграционные тесты для {@link RequestItemRepository}.
 *
 * @author avetokhin 05/02/18.
 */
class RequestItemRepositoryTest extends IntegrationTest {

    @Autowired
    private RequestItemRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void findSkuCountForRequestTest() {
        int skuCount = repository.getRequestItemsDetails(1L).getRequestSkuCounts();
        assertThat(skuCount, equalTo(3));
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/same_article_case.xml")
    void findSkuCountForRequestWithDuplicatedArticles() {
        int skuCount = repository.getRequestItemsDetails(1L).getRequestSkuCounts();
        assertThat(skuCount, equalTo(2));
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void findSkuCounts() {
        Map<Long, RequestItemsDetailsDTO> skuCounts = repository.getRequestItemsDetails(List.of(1L, 2L, 20L));
        assertions.assertThat(skuCounts).hasSize(3);
        assertions.assertThat(skuCounts.get(1L).getRequestSkuCounts()).isEqualTo(3);
        assertions.assertThat(skuCounts.get(2L).getRequestSkuCounts()).isEqualTo(2);
        assertions.assertThat(skuCounts.get(20L).getRequestSkuCounts()).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    @ExpectedDatabase(value = "classpath:repository/request-item/before.xml", assertionMode = NON_STRICT)
    void findLastSuppliedItemsBySupplierIdAndArticle() {

        Map<Long, List<String>> supplierSkuKeys = Map.of(
                1L, Arrays.asList("art1", "art3"),
                2L, Arrays.asList("art1"),
                3L, Arrays.asList("art2")
        );

        final List<RequestItem> items = repository.findLastSuppliedItems(supplierSkuKeys,
                LastSuppliedItemsFilter.builder().serviceIds(List.of(100L)).build());

        assertThat(items, notNullValue());
        assertThat(items, hasSize(3));

        final RequestItem first = items.get(0);
        assertThat(first.getId(), equalTo(1L));
        assertThat(first.getSupplierId(), equalTo(1L));

        final RequestItem second = items.get(1);
        assertThat(second.getId(), equalTo(12L));
        assertThat(second.getSupplierId(), equalTo(2L));

        final RequestItem third = items.get(2);
        assertThat(third.getId(), equalTo(28L));
        assertThat(third.getSupplierId(), equalTo(1L));
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    @ExpectedDatabase(value = "classpath:repository/request-item/before.xml", assertionMode = NON_STRICT)
    void findLastSuppliedItemsBySupplierIdAndArticleEmptyFilter() {
        final List<RequestItem> items = repository.findLastSuppliedItems(Collections.emptyMap(),
                LastSuppliedItemsFilter.builder().serviceIds(List.of(100L)).build());
        assertThat(items, notNullValue());
        assertThat(items, hasSize(0));
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    @ExpectedDatabase(value = "classpath:repository/request-item/before.xml", assertionMode = NON_STRICT)
    void findLastSuppliedItemsBySupplierIdAndArticleWithMinBoxCount() {

        Map<Long, List<String>> supplierSkuKeys = Map.of(
                1L, Arrays.asList("art1"),
                2L, Arrays.asList("art1"),
                3L, Arrays.asList("art2")
        );

        List<RequestItem> items = repository.findLastSuppliedItems(supplierSkuKeys,
                LastSuppliedItemsFilter.builder().serviceIds(List.of(100L)).minBoxCount(2).build());

        assertThat(items, notNullValue());
        assertThat(items, hasSize(1));

        RequestItem second = items.get(0);
        assertThat(second.getId(), equalTo(12L));
        assertThat(second.getSupplierId(), equalTo(2L));
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void findItemErrorsByRequestIds() {
        final Map<Long, Map<RequestItemErrorType, Long>> errorsCount =
                repository.findItemErrorsByRequestIds(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L));

        assertThat(errorsCount, notNullValue());
        assertThat(errorsCount.size(), equalTo(2));

        final Map<RequestItemErrorType, Long> errorsCountReq1 = errorsCount.get(9L);
        assertThat(errorsCountReq1, notNullValue());
        assertThat(errorsCountReq1.size(), equalTo(2));
        assertThat(errorsCountReq1.get(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND), equalTo(2L));
        assertThat(errorsCountReq1.get(RequestItemErrorType.NO_PREVIOUS_SUPPLY_WITH_ARTICLE_AND_SUPPLIER_FOUND),
                equalTo(1L));

        final Map<RequestItemErrorType, Long> errorsCountReq2 = errorsCount.get(10L);
        assertThat(errorsCountReq2, notNullValue());
        assertThat(errorsCountReq2.size(), equalTo(1));
        assertThat(errorsCountReq2.get(RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND), equalTo(1L));

    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void getItemsWrongCountInfoForRequest() {
        ItemsWrongCountInfo itemsWrongCountInfo = repository.getRequestItemsDetails(1L).getItemsWrongCountInfo();
        assertItemsWrongCountInfoCorrect(itemsWrongCountInfo, 1, 1, 0, 2);
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void getItemsWrongCountInfoForRequests() {
        Map<Long, RequestItemsDetailsDTO> requestItemsDetails =
                repository.getRequestItemsDetails(List.of(1L, 2L, 20L));
        assertions.assertThat(requestItemsDetails).hasSize(3);
        assertItemsWrongCountInfoCorrect(requestItemsDetails.get(1L).getItemsWrongCountInfo(), 1, 1, 0, 2);
        assertItemsWrongCountInfoCorrect(requestItemsDetails.get(2L).getItemsWrongCountInfo(), 0, 1, 0, 1);
        assertItemsWrongCountInfoCorrect(requestItemsDetails.get(20L).getItemsWrongCountInfo(), 0, 0, 0, 0);
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void findItemsCountForAllSuppliers() {
        ItemCount expectedItemCount = new ItemCount(1, 1, 101, 6);
        List<ItemCount> itemsCount = repository.findItemsCount(
                null, RequestType.SUPPLY, EnumSet.of(RequestStatus.FINISHED),
                LocalDateTime.of(1990, Month.JANUARY, 1, 0, 0));
        assertThat(itemsCount, hasSize(7));
        assertThat(itemsCount, hasItem(samePropertyValuesAs(expectedItemCount)));
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void findItemsCountForSupplier() {
        ItemCount expectedItemCount = new ItemCount(1, 1, 100, 3);
        List<ItemCount> itemsCount = repository.findItemsCount(
                1L, RequestType.SUPPLY, EnumSet.of(RequestStatus.FINISHED),
                LocalDateTime.of(2017, Month.MARCH, 1, 9, 9, 9));
        assertThat(itemsCount, hasSize(3));
        assertThat(itemsCount, hasItem(samePropertyValuesAs(expectedItemCount)));
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void findArticlesOfAlreadyAcceptedItems() {
        final Set<String> articles = repository
                .findArticlesOfAlreadyAcceptedItems(1L, 101L, Arrays.asList("art1", "art2", "art22"));
        assertThat(articles, notNullValue());
        assertThat(articles, equalTo(Sets.newHashSet("art1", "art2")));
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void findArticlesWithAnotherCaseByServiceIdAndSupplierId() {
        List<String> strings = new ArrayList<>();
        strings.add("ARt7");
        strings.add("ArT8");
        strings.add("art9");
        strings.add("art10");
        Set<String> articles = repository.findArticlesWithAnotherCaseByServiceIdAndSupplierId(5L,
                101L,
                strings);
        assertThat(articles, hasSize(2));
        assertThat(articles, equalTo(Sets.newHashSet("ARt7", "ArT8")));
    }

    @Test
    @Transactional
    @DatabaseSetup("classpath:repository/request-item/fetch_item_errors.xml")
    void fetchRequestItemErrors() {
        List<RequestItem> requestItems = repository.findAll();

        List<RequestItemError> requestItemErrorList = requestItems.get(0).getRequestItemErrorList();
        assertEquals(2, requestItemErrorList.size());

        assertTrue(requestItemErrorList.stream()
                .anyMatch(requestItemError ->
                        requestItemError.getErrorType() == RequestItemErrorType.NO_MARKET_SKU_MAPPING_FOUND));

        assertTrue(requestItemErrorList.stream()
                .anyMatch(requestItemError ->
                        requestItemError.getErrorType() ==
                                RequestItemErrorType.NO_PREVIOUS_SUPPLY_WITH_ARTICLE_AND_SUPPLIER_FOUND));

    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/request-item/find_without_errors.xml")
    void findAllByRequestIdNotInRequestItemError() {
        Set<RequestItem> items = repository.findAllByRequestIdNotInRequestItemError(1L);

        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(i -> i.getId().equals(2L)));
        assertTrue(items.stream().anyMatch(i -> i.getId().equals(3L)));
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void findItemsJoinFetchMarketVendorCodes() {
        Set<RequestItem> items = repository.findAllByRequestIdJoinFetchMarketVendorCodesOrderById(1L);
        RequestItem item1 = getItemById(items, 1L);
        RequestItem item2 = getItemById(items, 2L);
        RequestItem item3 = getItemById(items, 3L);

        assertEquals(3, items.size());

        assertEquals(1, item1.getMarketVendorCodeStrings().size());
        assertEquals("1", item1.getMarketVendorCodeStrings().get(0));

        assertEquals(2, item2.getMarketVendorCodeStrings().size());
        assertEquals("2", item2.getMarketVendorCodeStrings().get(0));
        assertEquals("2.1", item2.getMarketVendorCodeStrings().get(1));

        assertEquals(0, item3.getMarketVendorCodeStrings().size());
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void findItemsJoinFetchCargoTypes() {
        Set<RequestItem> items = repository.findAllByRequestIdJoinFetchCargoTypesOrderById(1L);
        RequestItem item1 = getItemById(items, 1L);
        RequestItem item2 = getItemById(items, 2L);
        RequestItem item3 = getItemById(items, 3L);

        assertEquals(3, items.size());
        assertEquals(2, item1.getImeiCount());
        assertEquals(1, item1.getSerialNumberCount());
        assertEquals("\\\\d+", item1.getImeiMask());
        assertEquals("\\\\w+", item1.getSerialNumberMask());

        assertEquals(3, item2.getRequestItemCargoTypes().size());

        assertEquals(0, item3.getRequestItemCargoTypes().size());
    }

    @Test
    @JpaQueriesCount(1)
    @DatabaseSetup("classpath:repository/request-item/before.xml")
    void findItemsJoinFetchIdentifiers() {
        Set<RequestItem> items = repository.findAllByRequestIdJoinFetchIdentifiersOrderById(1L);
        RequestItem item1 = getItemById(items, 1L);
        RequestItem item2 = getItemById(items, 2L);
        RequestItem item3 = getItemById(items, 3L);

        assertEquals(3, items.size());
        assertEquals(1, item1.getRequestItemIdentifiers().size());
        assertEquals(getDeclaredIdentifiers(1L, 1L), getIdentifierById(item1.getRequestItemIdentifiers(), 1L));

        assertEquals(2, item2.getRequestItemIdentifiers().size());
        assertEquals(getDeclaredIdentifiers(2L, 2L), getIdentifierById(item2.getRequestItemIdentifiers(), 2L));
        assertEquals(getReceivedIdentifiers(3, 2), getIdentifierById(item2.getRequestItemIdentifiers(), 3L));

        assertEquals(0, item3.getRequestItemIdentifiers().size());
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/before-xdoc.xml")
    void testLastSuppliedItemsCanBeFoundForXDocType() {
        Map<Long, List<String>> supplierSkuKeys = Map.of(
                1L, List.of("art1")
        );
        final List<RequestItem> items = repository.findLastSuppliedItems(supplierSkuKeys,
                LastSuppliedItemsFilter.builder().serviceIds(List.of(100L)).build());
        assertThat(items, notNullValue());
        assertThat(items, hasSize(1));
    }


    @Test
    @DatabaseSetup("classpath:repository/request-item/before-delete-item.xml")
    @ExpectedDatabase(value = "classpath:repository/request-item/after-delete-item.xml", assertionMode = NON_STRICT)
    void deleteErrorsWithItems() {
        var requestItems = repository.findAll(List.of(9L));
        repository.deleteInBatch(requestItems);
    }

    private Identifier getDeclaredIdentifiers(long id, long itemId) {
        return Identifier.builder()
                .id(id)
                .itemId(itemId)
                .identifiers(RegistryUnitId.of(
                        RegistryUnitIdType.CIS, itemId + "СIS01",
                        RegistryUnitIdType.CIS, itemId + "СIS02"
                ))
                .type(IdentifierType.DECLARED)
                .build();
    }

    @Test
    @DatabaseSetup("classpath:repository/request-item/find_skus_and_request_not_statuses.xml")
    void testFindBySkusAndRequestNotStatuses() {
        List<RequestItem> items = repository.findBySkusAndRequestNotStatuses(List.of(1L, 2L, 3L),
                List.of(RequestStatus.INVALID, RequestStatus.FINISHED));
        assertThat(items, notNullValue());
        assertThat(items, hasSize(1));
        assertEquals(items.get(0).getId(),  3L);
    }

    private Identifier getReceivedIdentifiers(long id, long itemId) {
        return Identifier.builder()
                .id(id)
                .itemId(itemId)
                .identifiers(RegistryUnitId.of(
                        RegistryUnitIdType.CIS, itemId + "СIS01"
                ))
                .type(IdentifierType.RECEIVED)
                .build();
    }

    private RequestItem getItemById(Set<RequestItem> items, long id) {
        return getById(items, id, RequestItem::getId);
    }

    private Identifier getIdentifierById(Set<Identifier> identifiers, long id) {
        return getById(identifiers, id, Identifier::getId);
    }

    private <T> T getById(Set<T> entities, long id, Function<T, Long> getId) {
        return entities.stream()
                .filter(item -> getId.apply(item).equals(id))
                .findFirst()
                .get();
    }

    private void assertItemsWrongCountInfoCorrect(ItemsWrongCountInfo itemsWrongCountInfo, int defect,
                                                  int shortage, int surplus, int problems) {
        assertThat(itemsWrongCountInfo, notNullValue());
        assertThat(itemsWrongCountInfo.getWithDefectsCount(), equalTo(defect));
        assertThat(itemsWrongCountInfo.getWithShortageCount(), equalTo(shortage));
        assertThat(itemsWrongCountInfo.getWithSurplusCount(), equalTo(surplus));
        assertThat(itemsWrongCountInfo.getWithProblemsCount(), equalTo(problems));
    }
}
