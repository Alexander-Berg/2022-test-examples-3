package ru.yandex.market.ff.service.implementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.repository.implementation.RequestItemRepositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SkuArticleValidationServiceTest {

    private RequestItemRepositoryImpl requestItemRepository = mock(RequestItemRepositoryImpl.class);
    private SkuArticleValidationService skuArticleValidationService =
        new SkuArticleValidationService(requestItemRepository);

    /**
     * Проверяет, что для 201 RequestItem сгенерируется 3 батча и результаты каждого вернутся в
     * итоговом списке errors.
     */
    @Test
    void checkThereWereNoSameArticlesWithAnotherCaseBefore() {
        doReturn(getQueryResult("article1", "article2", "article3"))
            .when(requestItemRepository).findArticlesWithAnotherCaseByServiceIdAndSupplierId(
                anyLong(),
                anyLong(),
                argThat(argument -> argument.contains("article1")));

        doReturn(getQueryResult("article500", "article501", "article502"))
            .when(requestItemRepository).findArticlesWithAnotherCaseByServiceIdAndSupplierId(
            anyLong(),
            anyLong(),
            argThat(argument -> argument.contains("article500")));

        doReturn(getQueryResult("article1000"))
            .when(requestItemRepository).findArticlesWithAnotherCaseByServiceIdAndSupplierId(
            anyLong(),
            anyLong(),
            argThat(argument -> argument.contains("article1000")));

        Set<Long> ids = skuArticleValidationService
            .findItemIdsWithArticlesDuplicatedIgnoringCaseOnlyWithinPreviousOrCurrentRequest(
                getShopRequest(),
                getItems("article")
            );

        verify(requestItemRepository, times(3)).
            findArticlesWithAnotherCaseByServiceIdAndSupplierId(eq(1L), eq(100L), any());
        assertThat(ids).hasSize(7);
        assertThat(ids).contains(1L, 2L, 3L, 500L, 501L, 502L, 1000L);
    }

    @Test
    void checkThereWereNoDatabaseQueriesIfAllElementsAreCaseInsensitive() {
        Set<Long> ids = skuArticleValidationService
                .findItemIdsWithArticlesDuplicatedIgnoringCaseOnlyWithinPreviousOrCurrentRequest(
                        getShopRequest(),
                        getItems("")
                );

        verify(requestItemRepository, never()).
                findArticlesWithAnotherCaseByServiceIdAndSupplierId(anyLong(), anyLong(), any());
        assertThat(ids).isEmpty();
    }

    @Test
    void checkThereWereDatabaseQueryOnlyForCaseSensitiveItem() {
        doReturn(getQueryResult("article1"))
                .when(requestItemRepository).findArticlesWithAnotherCaseByServiceIdAndSupplierId(
                anyLong(),
                anyLong(),
                eq(List.of("article1")));

        List<RequestItem> items = getItems("");
        RequestItem requestItem = new RequestItem();
        requestItem.setId(5000L);
        requestItem.setArticle("article1");
        items.add(requestItem);
        Set<Long> ids = skuArticleValidationService
                .findItemIdsWithArticlesDuplicatedIgnoringCaseOnlyWithinPreviousOrCurrentRequest(
                        getShopRequest(),
                        items
                );

        verify(requestItemRepository).
                findArticlesWithAnotherCaseByServiceIdAndSupplierId(1L, 100L, List.of("article1"));
        assertThat(ids).hasSize(1);
        assertThat(ids).contains(5000L);
    }

    private Set<String> getQueryResult(String... strings) {
        return new HashSet<>(Arrays.asList(strings));
    }

    private List<RequestItem> getItems(String articlePrefix) {
        ArrayList<RequestItem> requestItems = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            RequestItem ri = new RequestItem();
            ri.setId((long) i);
            ri.setArticle(articlePrefix + i);
            requestItems.add(ri);
        }
        return requestItems;
    }

    private ShopRequest getShopRequest() {
        ShopRequest shopRequest = new ShopRequest();
        Supplier supplier = new Supplier();
        supplier.setId(1);
        shopRequest.setSupplier(supplier);
        shopRequest.setServiceId((long) 100);
        return shopRequest;
    }

}
