package ru.yandex.market.ff.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.ff.base.CommonTest;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.model.bo.SimpleStockInfo;
import ru.yandex.market.ff.model.bo.StockInfo;
import ru.yandex.market.ff.model.bo.SupplierSkuKey;
import ru.yandex.market.ff.model.converter.LgwClientInboundConverter;
import ru.yandex.market.ff.model.converter.LgwClientStatusConverter;
import ru.yandex.market.ff.model.converter.StockStorageClientConverter;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.entity.Supplier;
import ru.yandex.market.ff.model.entity.SupplierBusinessType;
import ru.yandex.market.ff.repository.NewMovementFlowRequestsRepository;
import ru.yandex.market.ff.repository.RequestRealSupplierInfoRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.implementation.StockServiceImpl;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOutboundClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageSearchClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.CurrentStock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.FailedFreezeStock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.OutboundItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.OutboundMeta;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Sku;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Stock;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.StockUnitId;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageDuplicateFreezeException;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.Pagination;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuFilter;
import ru.yandex.market.fulfillment.stockstorage.client.entity.request.search.SearchSkuRequest;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.StockFreezingResponse;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.ResultPagination;
import ru.yandex.market.fulfillment.stockstorage.client.entity.response.search.SearchSkuResponse;
import ru.yandex.market.logistic.gateway.common.model.common.DateTimeInterval;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff.client.enums.StockType.FIT;
import static ru.yandex.market.ff.util.TestUtils.createSkuWithSingleFitStock;

/**
 * Unit тесты для {@link StockService}.
 *
 * @author avetokhin 28/10/17.
 */
@ExtendWith(MockitoExtension.class)
class StockServiceTest extends CommonTest {

    private static final long SUPPLIER_ID_1 = 10L;
    private static final long SUPPLIER_ID_2 = 11L;

    private static final Long SERVICE_ID_1 = 100L;

    private static final long REQUEST_ID = 11L;

    private static final long ITEM_ID_1 = 1L;
    private static final long ITEM_ID_2 = 2L;
    private static final long ITEM_ID_3 = 3L;
    private static final long ITEM_ID_4 = 4L;

    private static final String SKU_1 = "sku1";
    private static final String SKU_2 = "sku2";
    private static final String SKU_3 = "sku3";
    private static final String SKU_4 = "sku4";

    private static final SearchSkuFilter FILTER =
            SearchSkuFilter.builder(SUPPLIER_ID_1, SERVICE_ID_1.intValue()).withEnabled(true).build();

    private static final long CROSSDOCK_WAREHOUSE_ID = 172;

    private static final OutboundMeta OUTBOUND = OutboundMeta.of(REQUEST_ID, SERVICE_ID_1.intValue(),
            StockType.EXPIRED, DateTimeInterval.fromFormattedValue("2018-01-01T10:10:10/2018-01-01T10:10:10"));

    private static final ShopRequest SUPPLY_REQUEST =
            createShopRequest(RequestType.SUPPLY, new Supplier(SUPPLIER_ID_1, null, null, null, null,
                    new SupplierBusinessType()));
    private static final ShopRequest CUSTOMER_RETURN_REQUEST =
            createShopRequest(RequestType.CUSTOMER_RETURN_SUPPLY, null);
    private static final ShopRequest WITHDRAW_REQUEST =
            createShopRequest(RequestType.WITHDRAW, new Supplier(SUPPLIER_ID_1, null, null, null, null,
                    new SupplierBusinessType()));
    private static final ShopRequest CROSSDOCK_REQUEST =
            createShopRequest(RequestType.CROSSDOCK, new Supplier(SUPPLIER_ID_1, null, null, null, null,
                            new SupplierBusinessType()),
                    FIT);

    private static final int FIXED_COUNT = 10;
    private static final Pagination PAGINATION = Pagination.of(2, 0);

    @Mock
    private ShopRequestFetchingService shopRequestFetchingService;

    @Mock
    private StockStorageOutboundClient stockStorageOutboundClient;

    @Mock
    private StockStorageSearchClient stockStorageSearchClient;

    @Mock
    private RequestRealSupplierInfoRepository requestRealSupplierInfoRepository;

    ConcreteEnvironmentParamService concreteEnvironmentParamService =
            Mockito.mock(ConcreteEnvironmentParamService.class);

    @Mock
    private DateTimeService dateTimeService;

    @Mock
    private AssortmentService assortmentService;

    @Mock
    private FulfillmentInfoService fulfillmentInfoService;

    private final LgwClientInboundConverter clientConverter =
            new LgwClientInboundConverter(concreteEnvironmentParamService, new LgwClientStatusConverter(),
                    shopRequestFetchingService, requestRealSupplierInfoRepository, mock(RequestSubTypeService.class),
                    mock(ShopRequestRepository.class), fulfillmentInfoService);

    @Mock
    private NewMovementFlowRequestsRepository newMovementFlowRequestsRepository;

    private StockStorageClientConverter stockStorageClientConverter = new StockStorageClientConverter();

    private StockService stockService;

    @Mock
    private ConcreteEnvironmentParamService environmentParamService;

    @BeforeEach
    @SuppressWarnings("HiddenField")
    void init() {
        StockServiceImpl stockService = new StockServiceImpl(stockStorageOutboundClient,
                stockStorageSearchClient, clientConverter,
                stockStorageClientConverter, dateTimeService, newMovementFlowRequestsRepository,
                environmentParamService);

        stockService.setBatchSize(2);
        when(environmentParamService.getNotFrozenStocksBatchSize()).thenReturn(2);
        this.stockService = stockService;
        Mockito.when(assortmentService.getChildSkusMap(any())).thenReturn(Collections.emptyMap());
    }

    @Test
    void testGetNotFrozenStocksLastPageEmpty() {

        doReturn(SearchSkuResponse.of(asList(
                createSkuWithSingleFitStock(SKU_1, 10),
                createSkuWithSingleFitStock(SKU_2, 12)),
                ResultPagination.builder(PAGINATION.getLimit(), PAGINATION.getOffset(), 2, -1).withLastId(2L).build(),
                FILTER))
                .when(stockStorageSearchClient)
                .searchSku(argThat(arg -> arg.getPagination().getOffsetId() == 0L));

        doReturn(SearchSkuResponse.of(Collections.emptyList(),
                ResultPagination.builder(PAGINATION.getLimit(), PAGINATION.getOffset(), 0, -1).withLastId(0L).build(),
                FILTER))
                .when(stockStorageSearchClient)
                .searchSku(argThat(arg -> arg.getPagination().getOffsetId() == 2L));

        final List<SimpleStockInfo> stocks = stockService.getNotFrozenStocks(SUPPLIER_ID_1, SERVICE_ID_1, FIT);

        assertThat(stocks, equalTo(asList(
                new SimpleStockInfo(new SupplierSkuKey(1, SKU_1), 10),
                new SimpleStockInfo(new SupplierSkuKey(1, SKU_2), 12)
        )));
        verify(stockStorageSearchClient, times(2)).searchSku(any());
    }

    @Test
    void testGetNotFrozenStocksLastPageNonEmpty() {

        doReturn(SearchSkuResponse.of(asList(
                createSkuWithSingleFitStock(SKU_1, 10),
                createSkuWithSingleFitStock(SKU_2, 11)),
                ResultPagination.builder(PAGINATION.getLimit(), PAGINATION.getOffset(), 2, -1).withLastId(2L).build(),
                FILTER))
                .when(stockStorageSearchClient)
                .searchSku(argThat(arg -> arg.getPagination().getOffsetId() == 0L));

        doReturn(SearchSkuResponse.of(List.of(createSkuWithSingleFitStock(SKU_3, 12)),
                ResultPagination.builder(PAGINATION.getLimit(), PAGINATION.getOffset(), 1, -1).withLastId(0L).build(),
                FILTER))
                .when(stockStorageSearchClient)
                .searchSku(argThat(arg -> arg.getPagination().getOffsetId() == 2L));

        final List<SimpleStockInfo> stocks = stockService.getNotFrozenStocks(SUPPLIER_ID_1, SERVICE_ID_1, FIT);

        assertThat(stocks, equalTo(asList(
                new SimpleStockInfo(new SupplierSkuKey(1, SKU_1), 10),
                new SimpleStockInfo(new SupplierSkuKey(1, SKU_2), 11),
                new SimpleStockInfo(new SupplierSkuKey(1, SKU_3), 12)
        )));
        verify(stockStorageSearchClient, times(2)).searchSku(any());
    }

    @Test
    void testFreezeOnStockSuccess() throws StockStorageDuplicateFreezeException {
        final RequestItem item1 = item(ITEM_ID_1, SKU_1);
        final RequestItem item2 = item(ITEM_ID_2, SKU_2);

        when(stockStorageOutboundClient.freezeStocks(any(), any()))
                .thenReturn(StockFreezingResponse.success(REQUEST_ID));

        final List<SimpleStockInfo> notEnoughToFreeze =
                stockService.freezeOnStock(WITHDRAW_REQUEST, asList(item1, item2));

        verify(stockStorageOutboundClient).freezeStocks(OUTBOUND, asList(
                OutboundItem.of(SUPPLIER_ID_1, SKU_1, FIXED_COUNT),
                OutboundItem.of(SUPPLIER_ID_1, SKU_2, FIXED_COUNT)
        ));

        assertThat(notEnoughToFreeze, empty());
    }

    @Test
    void testFreezeOnStocksFail() throws StockStorageDuplicateFreezeException {
        final RequestItem item1 = item(ITEM_ID_1, SKU_1);
        final RequestItem item2 = item(ITEM_ID_2, SKU_2);

        final FailedFreezeStock failedFreezeStock = FailedFreezeStock.of(SKU_2, SUPPLIER_ID_1, 1, 6, 1);
        final StockFreezingResponse response =
                StockFreezingResponse.notEnough(REQUEST_ID, singletonList(failedFreezeStock));
        when(stockStorageOutboundClient.freezeStocks(any(), any())).thenReturn(response);

        final List<SimpleStockInfo> notEnoughToFreeze =
                stockService.freezeOnStock(WITHDRAW_REQUEST, asList(item1, item2));

        verify(stockStorageOutboundClient).freezeStocks(OUTBOUND, asList(
                OutboundItem.of(SUPPLIER_ID_1, SKU_1, FIXED_COUNT),
                OutboundItem.of(SUPPLIER_ID_1, SKU_2, FIXED_COUNT)
        ));

        assertThat(notEnoughToFreeze, notNullValue());
        assertThat(notEnoughToFreeze, hasSize(1));

        final SimpleStockInfo simpleStockInfo = notEnoughToFreeze.get(0);
        assertThat(simpleStockInfo.getCount(), equalTo(failedFreezeStock.getQuantity()));
        assertThat(simpleStockInfo.getSupplierSkuKey().getSku(), equalTo(failedFreezeStock.getSku()));
    }

    private void assertSearchClientCalls(final SearchSkuRequest req1, final SearchSkuRequest req2) {
        ArgumentCaptor<SearchSkuRequest> captor = ArgumentCaptor.forClass(SearchSkuRequest.class);
        verify(stockStorageSearchClient, times(2)).searchSku(captor.capture());
        verifyNoMoreInteractions(stockStorageSearchClient);

        final List<SearchSkuRequest> capturedRequests = captor.getAllValues();

        // Из-за того, что у пагинации не переопределен equals, приходится ее проверять отдельно
        assertThat(capturedRequests.get(0).getFilter(), samePropertyValuesAs(req1.getFilter()));
        assertThat(capturedRequests.get(1).getFilter(), samePropertyValuesAs(req2.getFilter()));
        assertThat(capturedRequests.get(0).getPagination(), samePropertyValuesAs(PAGINATION));
        assertThat(capturedRequests.get(1).getPagination(), samePropertyValuesAs(PAGINATION));

    }

    private static SSItem ssItem(String sku, long vendorId) {
        return SSItem.of(sku, vendorId, SERVICE_ID_1.intValue());
    }

    private static SearchSkuFilter skuFilter(final SSItem... ssItems) {
        return SearchSkuFilter.builder(asList(ssItems)).build();
    }

    private static Sku ssSku(final String sku, final long supplier, final List<Stock> stocks) {
        return Sku.builder()
                .withEnabled(true)
                .withUpdatable(true)
                .withUnitId(SSItem.of(sku, supplier, SERVICE_ID_1.intValue()))
                .withStocks(stocks)
                .build();
    }

    private static StockInfo stockInfo(final String article, final String name, final long fit, final long available,
                                       final long quarantine, final long defect) {
        return new StockInfo(article, name, fit, available, quarantine, defect);
    }

    private static CurrentStock stock(final String sku, final int fit, final int available, final int quarantine,
                                      final int defect, long shopId) {
        final StockUnitId unitId = new StockUnitId(sku, shopId, sku);
        return new CurrentStock(unitId, null, fit, defect, quarantine, 0, available, null);
    }

    private static ShopRequest createShopRequest(final RequestType requestType, final Supplier supplier) {
        return createShopRequest(requestType, supplier, ru.yandex.market.ff.client.enums.StockType.EXPIRED);
    }

    private static ShopRequest createShopRequest(final RequestType requestType, final Supplier supplier,
                                                 final ru.yandex.market.ff.client.enums.StockType stockType) {
        final ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(REQUEST_ID);
        shopRequest.setServiceId(SERVICE_ID_1);
        shopRequest.setType(requestType);
        shopRequest.setSupplier(supplier);
        shopRequest.setStockType(stockType);
        shopRequest.setRequestedDate(LocalDateTime.of(2018, 1, 1, 10, 10, 10));
        return shopRequest;
    }

    private static RequestItem item(final long id, final String article, final Long supplierId) {
        final RequestItem requestItem = new RequestItem();
        requestItem.setId(id);
        requestItem.setArticle(article);
        requestItem.setSupplierId(supplierId);
        requestItem.setCount(FIXED_COUNT);

        return requestItem;

    }

    private static RequestItem item(final long id, final String article) {
        return item(id, article, null);
    }
}
