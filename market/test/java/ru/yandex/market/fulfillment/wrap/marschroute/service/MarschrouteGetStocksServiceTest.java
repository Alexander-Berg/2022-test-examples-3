package ru.yandex.market.fulfillment.wrap.marschroute.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ru.yandex.market.fulfillment.wrap.core.transformer.FulfillmentModelTransformer;
import ru.yandex.market.fulfillment.wrap.marschroute.exception.ModelConversionException;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.MarschrouteProductsRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.MarschrouteProductsResponse;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.MarschrouteProductsResponseData;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.StockInfo;
import ru.yandex.market.fulfillment.wrap.marschroute.model.type.MarschrouteDateTime;
import ru.yandex.market.fulfillment.wrap.marschroute.service.common.FulfillmentWarehouseIdProvider;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarschrouteGetStocksServiceTest {

    static final String YANDEX_ID = "YANDEX_ID";
    static final String PARTNER_ID = "PARTNER_ID";
    private static final String NOT_VALID_ITEM_ID = "someVendorId";
    private static final String VALID_ITEM_ID = "somevndr.123123123";
    @Mock
    private FulfillmentModelTransformer modelTransformer;
    @Mock
    private FulfillmentWarehouseIdProvider warehouseIdProvider;
    @Mock
    private MarschrouteProductsService marschrouteProductsService;
    private MarschrouteGetStocksService service;

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(marschrouteProductsService, modelTransformer, warehouseIdProvider);
    }

    @ValueSource(booleans = {false, true})
    @ParameterizedTest
    void executeSuccessWithLimitOffset(boolean failEnabled) throws Exception {
        setUp(failEnabled);
        assertResult(() -> service.execute(1, 1), failEnabled);
        verify(marschrouteProductsService, times(1)).execute(anyInt(), anyInt());

    }

    @ValueSource(booleans = {false, true})
    @ParameterizedTest
    void executeSuccessWithDateTime(boolean failEnabled) throws Exception {
        setUp(failEnabled);
        assertResult(() -> service.execute(
            MarschrouteDateTime.create(LocalDateTime.now()),
            MarschrouteDateTime.create(LocalDateTime.now())
        ), failEnabled);
        verify(marschrouteProductsService, times(1)).execute(any(MarschrouteProductsRequest.class));

    }

    @ValueSource(booleans = {false, true})
    @ParameterizedTest
    void executeSuccessWithUnitIds(boolean failEnabled) throws Exception {
        setUp(failEnabled);
        assertResult(
            () -> service.execute(ImmutableSet.of(new UnitId(null, null, null))),
            failEnabled
        );
        verify(marschrouteProductsService, times(1)).execute(anySetOf(UnitId.class));

    }

    /**
     * кейс с двумя элементами вервнувшимися от маршпрута
     * первый некорректный втрой корректный
     * в тестах ожидется что при включенной настройке fulfillment.services.stock.failOnUnparseableItemId
     * будет выброшено исключение иначе в результуриющем списке будет только 1 элемент(2ой по счету)
     *
     * @throws Exception
     */
    void setUp(boolean failEnabled) throws Exception {
        service = new MarschrouteGetStocksService(modelTransformer, warehouseIdProvider, marschrouteProductsService, failEnabled);
        MarschrouteProductsResponse response = new MarschrouteProductsResponse();
        List<MarschrouteProductsResponseData> data = ImmutableList.of(
            new MarschrouteProductsResponseData() {{
                setItemId(NOT_VALID_ITEM_ID);
                setStockInfo(new StockInfo());
            }},
            new MarschrouteProductsResponseData() {{
                setItemId(VALID_ITEM_ID);
                setStockInfo(new StockInfo());
            }}
        );

        response.setData(data);

        when(marschrouteProductsService.execute(anySetOf(UnitId.class))).thenReturn(response);
        when(marschrouteProductsService.execute(anyInt(), anyInt())).thenReturn(response);
        when(marschrouteProductsService.execute(anyInt(), anyInt(), anySetOf(UnitId.class))).thenReturn(response);
        when(marschrouteProductsService.execute(any(MarschrouteProductsRequest.class))).thenReturn(response);
        when(modelTransformer.transform(anyObject(), any())).thenReturn(Collections.emptyList());
        when(warehouseIdProvider.getYandexId()).thenReturn(YANDEX_ID);
        when(warehouseIdProvider.getPartnerId()).thenReturn(PARTNER_ID);
    }

    private void assertResult(Callable<List<ItemStocks>> testCase, boolean failEnabled) throws Exception {
        if (failEnabled) {
            checkExceptionThrown(testCase);
        } else {
            checkWrongItemIdOmitted(testCase);
        }

    }

    private void checkWrongItemIdOmitted(Callable<List<ItemStocks>> testCase) throws Exception {
        List<ItemStocks> call = testCase.call();
        verify(modelTransformer, times(1)).transformToList(
            any(StockInfo.class), eq(Stock.class));

        verify(warehouseIdProvider, times(1)).getYandexId();
        verify(warehouseIdProvider, times(1)).getPartnerId();
        assertEquals(1, call.size());

    }

    private void checkExceptionThrown(Callable<List<ItemStocks>> execute) {
        try {
            execute.call();
            fail("No exception thrown. Expected " + ModelConversionException.class);
        } catch (Exception e) {
            assertTrue(ModelConversionException.class.isAssignableFrom(e.getClass()));
        }
    }


}
