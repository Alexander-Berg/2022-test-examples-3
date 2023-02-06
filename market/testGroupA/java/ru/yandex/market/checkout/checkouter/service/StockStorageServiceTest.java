package ru.yandex.market.checkout.checkouter.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.checkouter.color.ColorConfig;
import ru.yandex.market.checkout.checkouter.color.SingleColorConfig;
import ru.yandex.market.checkout.checkouter.event.EventService;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.shop.pushapi.PushApiShopSettingsService;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.checkouter.storage.OrderWritingDao;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStorageOrderClient;
import ru.yandex.market.fulfillment.stockstorage.client.StockStoragePreOrderClient;
import ru.yandex.market.fulfillment.stockstorage.client.entity.exception.StockStorageFreezeNotFoundException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.order.OrderPropertyType.UNFREEZE_STOCKS_TIME;
import static ru.yandex.market.checkout.test.providers.OrderProvider.getColorOrder;
import static ru.yandex.market.checkout.test.providers.OrderProvider.getFulfillmentOrderWithYandexDelivery;

/**
 * Created by asafev on 24/11/2017.
 */
public class StockStorageServiceTest {

    private StockStorageService stockStorageService;
    private StockStorageOrderClient stockStorage;
    private SingleColorConfig colorConfig;

    private Settings shopSettingsMock = mock(Settings.class);
    private StockStorageClient stockStorageClientMock = mock(StockStorageClient.class);

    @BeforeEach
    public void init() {
        stockStorage = mock(StockStorageOrderClient.class);
        colorConfig = mock(SingleColorConfig.class);
        ColorConfig colorsConfig = mock(ColorConfig.class);
        TransactionTemplate template = mock(TransactionTemplate.class);
        when(template.execute(Mockito.any())).thenAnswer((Answer) invocation -> {
            Object[] args = invocation.getArguments();
            TransactionCallback arg = (TransactionCallback) args[0];
            return arg.doInTransaction(new SimpleTransactionStatus());
        });
        var settingsCheckouterService = mock(PushApiShopSettingsService.class);
        when(settingsCheckouterService.getSettings(Mockito.anyLong(), Mockito.anyBoolean()))
                .thenReturn(shopSettingsMock);
        StockStorageServiceImpl service = new StockStorageServiceImpl(stockStorage,
                mock(StockStoragePreOrderClient.class),
                mock(OrderWritingDao.class), mock(OrderReadingDao.class), mock(EventService.class),
                Executors.newSingleThreadExecutor(), template, Clock.systemDefaultZone(),
                colorsConfig, settingsCheckouterService, stockStorageClientMock

        );

        doReturn(colorConfig).when(colorsConfig).getFor(any(Order.class));
        doCallRealMethod().when(colorConfig).atSupplierWarehouseByDefault();
        doCallRealMethod().when(colorConfig).getShopSku(any(OrderItem.class));
        stockStorageService = service;
    }

    @Test
    public void testUnfreezeStocksScheduleOnFail() throws Exception {
        doThrow(new RuntimeException("Boo!"))
                .when(stockStorage)
                .unfreezeStocks(anyString());
        doThrow(new RuntimeException("Boo!"))
                .when(stockStorage)
                .unfreezeStocks(anyString(), anyBoolean());
        Order order = prepareOrder();
        LocalDateTime before = LocalDateTime.now();
        boolean result = stockStorageService.tryUnfreezeStocksOrScheduleOnFail(order, false);
        LocalDateTime after = LocalDateTime.now();

        assertFalse(result);
        assertThat("Время в поле unfreezeStocksTime не проставлено!", order.getProperty(UNFREEZE_STOCKS_TIME),
                not(nullValue()));
        assertThat("Время в поле unfreezeStocksTime неправильное!", order.getProperty(UNFREEZE_STOCKS_TIME),
                both(greaterThanOrEqualTo(before)).and(lessThanOrEqualTo(after)));
    }


    @Test
    public void testUnfreezeStocksStockStorageFreezeNotFoundExceptionOk() throws Exception {
        doThrow(new StockStorageFreezeNotFoundException("Boo!", new RuntimeException()))
                .when(stockStorage)
                .unfreezeStocks(anyString());
        Order order = prepareOrder();
        boolean result = stockStorageService.tryUnfreezeStocksOrScheduleOnFail(order, false);
        assertTrue(result);
    }

    @Test
    public void testUnfreezeStocks() throws Exception {
        doNothing()
                .when(stockStorage)
                .unfreezeStocks(anyString());
        Order order = prepareOrder();
        boolean result = stockStorageService.tryUnfreezeStocksOrScheduleOnFail(order, false);

        assertTrue(result);
    }

    @Test
    public void testUnfreezeStocksUsingForceUnfreezeApi() throws Exception {
        doNothing()
                .when(stockStorage)
                .unfreezeStocks(anyString());
        Order order = prepareOrder();
        when(shopSettingsMock.isPartnerInterface()).thenReturn(true);
        boolean result = stockStorageService.tryUnfreezeStocksWithCancellationOrScheduleOnFail(
                order,
                false,
                false,
                true);

        assertTrue(result);
        verify(stockStorageClientMock, times(1))
                .forceUnfreezeStocks(
                        order.getId().toString(),
                        "checkouter: force unfreeze before processing");
    }

    @Test
    public void testUnfreezeStocksUsingForceUnfreezeApiWithPartnerInterfaceFalse() throws Exception {
        doNothing()
                .when(stockStorage)
                .unfreezeStocks(anyString());
        Order order = prepareOrder();
        when(shopSettingsMock.isPartnerInterface()).thenReturn(false);
        boolean result = stockStorageService.tryUnfreezeStocksWithCancellationOrScheduleOnFail(
                order,
                false,
                false,
                true);

        assertTrue(result);
        verify(stockStorageClientMock, never()).forceUnfreezeStocks(anyString(), anyString());
    }

    @Test
    public void testUnfreezeDsbsStocksWithoutAtSupplierWarehouse() throws Exception {
        doReturn(true).when(colorConfig).atSupplierWarehouseByDefault();
        Order order = getColorOrder(Color.WHITE);
        order.setId(123456L);
        order.getItems().forEach(item -> {
            item.setWarehouseId(1);
            item.setShopSku("111");
            item.setFitFreezed(item.getCount());
        });

        boolean result = stockStorageService.tryUnfreezeStocksOrScheduleOnFail(order, false);

        assertTrue(result);
        verify(stockStorage).unfreezeStocks("123456", false);
    }

    @Test
    public void testUnfreezeDsbsStocksWithoutWarehouseId() throws Exception {
        doReturn(true).when(colorConfig).atSupplierWarehouseByDefault();
        Order order = getColorOrder(Color.WHITE);
        order.setId(123456L);
        order.getItems().forEach(item -> {
            item.setAtSupplierWarehouse(false);
            item.setWarehouseId(null);
            item.setShopSku("111");
            item.setFitFreezed(item.getCount());
        });

        boolean result = stockStorageService.tryUnfreezeStocksOrScheduleOnFail(order, false);

        assertTrue(result);
        verifyNoInteractions(stockStorage);
    }

    @Test
    public void testUnfreezeDsbsStocksWithoutShopSku() throws Exception {
        doReturn(true).when(colorConfig).atSupplierWarehouseByDefault();
        Order order = getColorOrder(Color.WHITE);
        order.setId(123456L);
        order.getItems().forEach(item -> {
            item.setShopSku(null);
            item.setFitFreezed(item.getCount());
        });

        boolean result = stockStorageService.tryUnfreezeStocksOrScheduleOnFail(order, false);

        assertTrue(result);
        verifyNoInteractions(stockStorage);
    }

    private Order prepareOrder() {
        Order order = getFulfillmentOrderWithYandexDelivery();
        order.setId(123456L);
        FulfilmentProvider.fulfilmentize(order);
        order.getItems().forEach(oi -> {
            oi.setWarehouseId(1);
            oi.setFulfilmentWarehouseId(1L);
            oi.setFitFreezed(oi.getCount());
        });
        return order;
    }

}
