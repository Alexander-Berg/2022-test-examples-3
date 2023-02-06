package ru.yandex.market.ff.enrichment;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.client.enums.RequestItemErrorAttributeType;
import ru.yandex.market.ff.client.enums.RequestItemErrorType;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.client.enums.VatRate;
import ru.yandex.market.ff.model.bo.EnrichmentResultContainer;
import ru.yandex.market.ff.model.bo.RequestItemErrorInfo;
import ru.yandex.market.ff.model.bo.SimpleStockInfo;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.repository.RequestItemCargoTypesRepository;
import ru.yandex.market.ff.repository.RequestItemMarketBarcodeRepository;
import ru.yandex.market.ff.repository.RequestItemMarketVendorCodeRepository;
import ru.yandex.market.ff.service.RequestItemService;
import ru.yandex.market.ff.service.StockService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit тесты для {@link WithdrawRequestItemsEnricher}.
 *
 * @author avetokhin 20/04/2018.
 */
class WithdrawRequestItemsEnricherTest {

    private static final long ITEM_ID_1 = 1L;
    private static final long ITEM_ID_2 = 2L;
    private static final long ITEM_ID_3 = 3L;

    private static final String ARTICLE_1 = "art1";
    private static final String ARTICLE_2 = "art2";
    private static final String ARTICLE_3 = "art3";

    private static final ShopRequest REQUEST = createShopRequest();

    private static final RequestItem LAST_ITEM_1 = lastItem(ITEM_ID_1, ARTICLE_1, "item1", 123);
    private static final RequestItem LAST_ITEM_2 = lastItem(ITEM_ID_2, ARTICLE_2, "item2", 321);

    private RequestItemMarketVendorCodeRepository requestItemMarketVendorCodeRepository;
    private RequestItemMarketBarcodeRepository requestItemMarketBarcodeRepository;
    private RequestItemCargoTypesRepository requestItemCargoTypesRepository;
    private WithdrawRequestItemsEnricher enricher;

    private StockService stockService;

    @BeforeEach
    void init() {
        final RequestItemService requestItemService = mock(RequestItemService.class);
        when(requestItemService.findLastSuppliedItemsBySupplierIdAndArticle(any(), anyList(), any()))
                .thenReturn(Arrays.asList(LAST_ITEM_1, LAST_ITEM_2));
        requestItemMarketVendorCodeRepository = mock(RequestItemMarketVendorCodeRepository.class);
        requestItemMarketBarcodeRepository = mock(RequestItemMarketBarcodeRepository.class);
        requestItemCargoTypesRepository = mock(RequestItemCargoTypesRepository.class);
        stockService = mock(StockService.class);

        enricher = new WithdrawRequestItemsEnricher(requestItemService,
                stockService,
                requestItemMarketVendorCodeRepository,
                requestItemMarketBarcodeRepository,
                requestItemCargoTypesRepository,
                new FreezeStockEnricherService(stockService));
    }

    /**
     * Когда все строки заявки были в предыдущих поставках и необходимо попробовать сделать фриз.
     */
    @Test
    void enrichAllItemsWasInSupply() {
        final RequestItem item1 = item(ITEM_ID_1, ARTICLE_1, 10);
        final RequestItem item2 = item(ITEM_ID_2, ARTICLE_2, 9);
        final List<RequestItem> items = Arrays.asList(item1, item2);

        when(stockService.freezeOnStock(REQUEST, items))
                .thenReturn(Collections.singletonList(new SimpleStockInfo(new SupplierSkuKey(10, ARTICLE_1), 3)));

        final Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(REQUEST, items);

        verify(stockService).freezeOnStock(REQUEST, items);

        // Проверить ошибки
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer errorsContainer = errors.get(ITEM_ID_1);

            softly.assertThat(errorsContainer).isNotNull();
            Set<RequestItemErrorInfo> validationErrors = errorsContainer.getValidationErrors();
            softly.assertThat(validationErrors).hasSize(1);

            Optional<RequestItemErrorInfo> infoOptional = validationErrors.stream()
                    .findAny();
            softly.assertThat(infoOptional).isPresent();

            softly.assertThat(infoOptional.get()).isEqualTo(
                    RequestItemErrorInfo.of(
                            RequestItemErrorType.NOT_ENOUGH_ON_STOCK,
                            Collections.singletonMap(RequestItemErrorAttributeType.CURRENTLY_ON_STOCK, "3")
                    )
            );

        });

        // Проверить обновленные строки заявки
        assertSameIgnoringCount(item1, LAST_ITEM_1);
        assertSameIgnoringCount(item2, LAST_ITEM_2);
    }

    /**
     * Когда не все строки заявки были в предыдущих поставках и необходимости делать фриз нет.
     */
    @Test
    void enrichNotAllItemsWasInSupply() {
        final RequestItem item1 = item(ITEM_ID_1, ARTICLE_1, 10);
        final RequestItem item2 = item(ITEM_ID_2, ARTICLE_2, 9);
        final RequestItem item3 = item(ITEM_ID_3, ARTICLE_3, 12);
        final List<RequestItem> items = Arrays.asList(item1, item2, item3);

        final Map<Long, EnrichmentResultContainer> errors = enricher.enrichSafe(REQUEST, items);

        verifyZeroInteractions(stockService);

        // Проверить ошибки
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(errors).hasSize(1);
            EnrichmentResultContainer errorsContainer = errors.get(ITEM_ID_3);

            softly.assertThat(errorsContainer).isNotNull();
            Set<RequestItemErrorInfo> validationErrors = errorsContainer.getValidationErrors();
            softly.assertThat(validationErrors).hasSize(1);

            Optional<RequestItemErrorInfo> infoOptional = validationErrors.stream()
                    .findAny();
            softly.assertThat(infoOptional).isPresent();

            softly.assertThat(infoOptional.get()).isEqualTo(
                    RequestItemErrorInfo.of(RequestItemErrorType.NO_PREVIOUS_SUPPLY_WITH_ARTICLE_AND_SUPPLIER_FOUND)
            );

        });

        // Проверить обновленные строки заявки
        assertSameIgnoringCount(item1, LAST_ITEM_1);
        assertSameIgnoringCount(item2, LAST_ITEM_2);

        // Третья строка не была найдена в прошлых поставках, ее не обновили
        assertThat(item3.getName(), nullValue());
        assertThat(item3.getBarcodes(), equalTo(Collections.emptyList()));
        assertThat(item3.getVatRate(), nullValue());
        assertThat(item3.getSku(), nullValue());
        assertThat(item3.getSupplyPrice(), nullValue());
        assertThat(item3.getUntaxedPrice(), nullValue());
    }

    private static ShopRequest createShopRequest() {
        final ShopRequest shopRequest = new ShopRequest();
        shopRequest.setType(RequestType.WITHDRAW);
        shopRequest.setStockType(StockType.DEFECT);
        shopRequest.setSupplier(new Supplier(10, null, null, null, null, new SupplierBusinessType()));
        shopRequest.setServiceId(1L);
        return shopRequest;
    }

    private static RequestItem lastItem(long id, String article, String name, long sku) {
        final RequestItem item = new RequestItem();
        item.setId(id);
        item.setArticle(article);
        item.setName(name);
        item.setBarcodes(Arrays.asList("bar1", "bar2"));
        item.setVatRate(VatRate.VAT_18);
        item.setSku(sku);
        item.setSupplyPrice(new BigDecimal("110.10"));
        item.setUntaxedPrice(new BigDecimal("100"));
        item.setBoxCount(12);
        item.setHasExpirationDate(false);
        return item;
    }

    private static RequestItem item(final long id, final String article, int count) {
        final RequestItem requestItem = new RequestItem();
        requestItem.setId(id);
        requestItem.setArticle(article);
        requestItem.setCount(count);
        return requestItem;
    }

    private static void assertSameIgnoringCount(RequestItem first, RequestItem second) {
        int firstCount = first.getCount();
        int secondCount = second.getCount();
        first.setCount(0);
        second.setCount(0);
        assertThat(first, samePropertyValuesAs(second));
        first.setCount(firstCount);
        second.setCount(secondCount);
    }
}
