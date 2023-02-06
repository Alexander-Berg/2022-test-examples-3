package ru.yandex.market.fulfillment.wrap.marschroute.service;

import java.util.List;
import java.util.concurrent.Callable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ru.yandex.market.fulfillment.wrap.marschroute.exception.ModelConversionException;
import ru.yandex.market.fulfillment.wrap.marschroute.model.converter.BarcodeSourceDeterminer;
import ru.yandex.market.fulfillment.wrap.marschroute.model.converter.ProductsResponseDataToItemReferenceConverter;
import ru.yandex.market.fulfillment.wrap.marschroute.model.request.stock.MarschrouteProductsRequest;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.MarschrouteProductsResponse;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.MarschrouteProductsResponseData;
import ru.yandex.market.fulfillment.wrap.marschroute.model.response.stock.StockInfo;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MarschrouteGetReferenceItemsServiceTest {

    private static final String NOT_VALID_ITEM_ID = "someVendorId";
    private static final String VALID_ITEM_ID = "somevndr.123123123";

    @Mock
    private MarschrouteProductsService marschrouteProductsService;
    @Spy
    private ProductsResponseDataToItemReferenceConverter converter = new ProductsResponseDataToItemReferenceConverter(
        new BarcodeSourceDeterminer()
    );

    private MarschrouteGetReferenceItemsService service;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(marschrouteProductsService);
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
     */
    private void setUp(boolean failEnabled) {
        service = new MarschrouteGetReferenceItemsService(
            marschrouteProductsService,
            converter,
            failEnabled
        );

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
    }

    private void assertResult(Callable<List<ItemReference>> testCase, boolean failEnabled) throws Exception {
        if (failEnabled) {
            checkExceptionThrown(testCase);
        } else {
            checkWrongItemIdOmitted(testCase);
        }

    }

    private void checkWrongItemIdOmitted(Callable<List<ItemReference>> testCase) throws Exception {
        List<ItemReference> call = testCase.call();
        verify(converter, times(2)).canConvert(any(MarschrouteProductsResponseData.class));
        verify(converter, times(1)).convert(any(MarschrouteProductsResponseData.class));

        assertEquals(1, call.size());
    }

    private void checkExceptionThrown(Callable<List<ItemReference>> execute) {
        try {
            execute.call();
            fail("No exception thrown. Expected " + ModelConversionException.class);
        } catch (Exception e) {
            assertTrue(ModelConversionException.class.isAssignableFrom(e.getClass()));
        }
    }
}
