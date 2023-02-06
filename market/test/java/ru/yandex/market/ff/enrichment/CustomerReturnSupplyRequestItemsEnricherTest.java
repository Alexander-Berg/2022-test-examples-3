package ru.yandex.market.ff.enrichment;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.model.bo.EnrichmentResultContainer;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

class CustomerReturnSupplyRequestItemsEnricherTest extends IntegrationTest {
    @Autowired
    CustomerReturnSupplyRequestItemsEnricher enricher;

    @Test
    @DatabaseSetup(
        "classpath:/controller/upload-request/before-enriching-customer-return-requests-with-real-supplier-info.xml")
    void enricherReceivesRealSupplierInfoFromPreviousRequests() {
        ShopRequest request = new ShopRequest();
        request.setId(3L);

        RequestItem item1 = buildRequestItem(5L, 1L, "sku1");
        RequestItem item2 = buildRequestItem(6L, 1L, "sku2");

        Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(request, Arrays.asList(item1, item2));

        assertThat(errors.size(), is(0));
        assertThat(item1.getRealSupplierId(), is(nullValue()));
        assertThat(item1.getRealSupplierName(), is(nullValue()));

        assertThat(item2.getRealSupplierId(), is(nullValue()));
        assertThat(item2.getRealSupplierName(), is(nullValue()));
    }

    @Test
    @DatabaseSetup(
        "classpath:/controller/upload-request/before-enriching-customer-return-requests-check-missing-mapping.xml")
    void shouldIgnoreMappingAbsence() {
        CustomerReturnSupplyRequestItemsEnricher enricherSpy = Mockito.spy(enricher);

        when(enricherSpy.getMarketSkuMapping(anySet())).thenReturn(Collections.emptyMap());
        ShopRequest request = new ShopRequest();
        request.setId(3L);

        RequestItem item1 = buildRequestItem(5L, 1L, "sku1");
        RequestItem item2 = buildRequestItem(6L, 1L, "sku2");
        List<RequestItem> items = Arrays.asList(item1, item2);

        Map<Long, EnrichmentResultContainer> errors = enricherSpy.enrichSafe(request, items);
        assertThat(errors.size(), is(0));
    }

    @Test
    @DatabaseSetup(
        "classpath:/controller/upload-request/before-enriching-customer-return-requests-with-real-supplier-info.xml")
    void customerReturnRequestEnrichesWithRealSupplierInfo() {
        ShopRequest request = new ShopRequest();
        request.setId(4L);

        RequestItem item1 = buildRequestItem(7L, 2L, "sku1");
        RequestItem item2 = buildRequestItem(8L, 2L, "sku2");
        List<RequestItem> items = Arrays.asList(item1, item2);

        Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(request, items);

        assertThat(errors.size(), is(0));
        assertThat(item1.getRealSupplierId(), is(notNullValue()));
        assertThat(item1.getRealSupplierName(), is(notNullValue()));

        assertThat(item2.getRealSupplierId(), is(notNullValue()));
        assertThat(item2.getRealSupplierName(), is(notNullValue()));
    }

    @Test
    @DatabaseSetup(
        "classpath:/controller/upload-request/before-enriching-customer-return-requests-with-real-supplier-info.xml")
    void tryToEnrichWithAbsentItemsInPreviousSupplies() {
        ShopRequest request = new ShopRequest();
        request.setId(4L);

        RequestItem item1 = buildRequestItem(7L, 2L, "sku1");
        RequestItem item2 = buildRequestItem(8L, 2L, "sku3");
        List<RequestItem> items = Arrays.asList(item1, item2);

        Map<Long, EnrichmentResultContainer> actual = enricher.enrichSafe(request, items);

        EnrichmentResultContainer enrichmentResultContainer = new EnrichmentResultContainer(8L);
        enrichmentResultContainer.addValidationError(
            RequestItemErrorType.NO_PREVIOUS_SUPPLY_WITH_ARTICLE_AND_SUPPLIER_FOUND);
        ImmutableMap<Long, EnrichmentResultContainer> expected = ImmutableMap.of(8L, enrichmentResultContainer);

        assertThat(actual.size(), is(expected.size()));
        assertThat(actual.keySet(), is(expected.keySet()));
        assertThat(actual.get(8L).getValidationErrors(), is(expected.get(8L).getValidationErrors()));
    }

    @Test
    @DatabaseSetup(
        "classpath:/controller/upload-request/before-enriching-customer-return-requests-with-real-supplier-info.xml")
    void enrichWithDataFromBothSupplyAndCrossdockRequests() {
        ShopRequest request = new ShopRequest();
        request.setId(5L);

        RequestItem item1 = buildRequestItem(13L, 2L, "sku6"); // item to enrich from request with type = 0
        RequestItem item2 = buildRequestItem(14L, 2L, "sku7"); // item to enrich from request with type = 4
        List<RequestItem> items = Arrays.asList(item1, item2);

        enricher.enrichSafe(request, items);

        assertThat(item1.getRealSupplierId(), is(notNullValue()));
        assertThat(item1.getRealSupplierName(), is(notNullValue()));

        assertThat(item2.getRealSupplierId(), is(notNullValue()));
        assertThat(item2.getRealSupplierName(), is(notNullValue()));
    }

    @Test
    @DatabaseSetup(
            "classpath:/controller/upload-request/" +
                    "before-enriching-customer-return-requests-with-shelf-life-params.xml")
    void enricherReceivesShelfLifeParamsFromPreviousRequests() {
        ShopRequest request = new ShopRequest();
        request.setId(3L);

        RequestItem item1 = buildRequestItem(5L, 1L, "sku1");
        RequestItem item2 = buildRequestItem(6L, 1L, "sku2");

        enricher.enrichSafe(request, Arrays.asList(item1, item2));

        assertions.assertThat(item1.getInboundRemainingLifetimeDays()).isEqualTo(30);
        assertions.assertThat(item1.getInboundRemainingLifetimePercentage()).isEqualTo(40);
        assertions.assertThat(item1.getOutboundRemainingLifetimeDays()).isEqualTo(10);
        assertions.assertThat(item1.getOutboundRemainingLifetimePercentage()).isEqualTo(20);

        assertions.assertThat(item2.getInboundRemainingLifetimeDays()).isEqualTo(80);
        assertions.assertThat(item2.getInboundRemainingLifetimePercentage()).isEqualTo(90);
        assertions.assertThat(item2.getOutboundRemainingLifetimeDays()).isEqualTo(50);
        assertions.assertThat(item2.getOutboundRemainingLifetimePercentage()).isEqualTo(60);
    }

    @NotNull
    private RequestItem buildRequestItem(Long itemId, Long supplierId, String article) {
        RequestItem result = new RequestItem();
        result.setId(itemId);
        result.setSupplierId(supplierId);
        result.setArticle(article);
        return result;
    }
}
