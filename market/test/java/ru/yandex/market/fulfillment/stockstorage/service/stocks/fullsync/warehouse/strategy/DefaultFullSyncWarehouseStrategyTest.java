package ru.yandex.market.fulfillment.stockstorage.service.stocks.fullsync.warehouse.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.fulfillment.stockstorage.client.entity.request.Source;
import ru.yandex.market.fulfillment.stockstorage.domain.exception.FullSyncFailedException;
import ru.yandex.market.fulfillment.stockstorage.service.FFService;
import ru.yandex.market.fulfillment.stockstorage.service.RetryingService;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.WarehouseAwareExecutionQueuePayload;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.StockUpdatingStrategy;
import ru.yandex.market.fulfillment.stockstorage.service.stocks.updating.strategy.StockUpdatingStrategyProvider;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyIntegerKey;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DefaultFullSyncWarehouseStrategyTest {

    private final FulfillmentClient lgwClient = mock(FulfillmentClient.class);
    private final StockUpdatingStrategy stockUpdatingStrategy = mock(StockUpdatingStrategy.class);
    private final StockUpdatingStrategyProvider stockUpdatingStrategyProvider =
            mock(StockUpdatingStrategyProvider.class);
    private final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
    private final DefaultFullSyncWarehouseStrategy syncWarehouseStrategy;

    {
        RetryingService retryingService = new RetryingService(2, 0, systemPropertyService);
        FFService ffService = new FFService(retryingService, lgwClient);
        syncWarehouseStrategy = new DefaultFullSyncWarehouseStrategy(ffService, stockUpdatingStrategyProvider,
                systemPropertyService);
        when(systemPropertyService.getIntegerProperty(SystemPropertyIntegerKey.MAX_LAST_PAGE_CALLS_FOR_FULL_SYNC))
                .thenReturn(10);
    }

    @Test
    public void syncNotLastBatch() {
        int pageSize = 3;
        long offset = 0;
        WarehouseAwareExecutionQueuePayload payload =
                new WarehouseAwareExecutionQueuePayload(offset, offset + pageSize, pageSize, false, 1);
        when(lgwClient.getStocks(3, (int) offset, new Partner(1L))).thenReturn(itemStocksByNormalPerson());
        when(stockUpdatingStrategyProvider.provide(1)).thenReturn(stockUpdatingStrategy);
        syncWarehouseStrategy.execute(payload);
        verify(lgwClient, times(1)).getStocks(3, 0, new Partner(1L));
        verify(stockUpdatingStrategy, times(1)).syncStock(any(Source.class), anyList());
    }

    @Test
    public void syncReallyLastBatch() {

        WarehouseAwareExecutionQueuePayload payload = new WarehouseAwareExecutionQueuePayload(7L, 10L, 3, true, 1);
        when(lgwClient.getStocks(3, 7, new Partner(1L)))
                .thenReturn(itemStocksByNormalPerson());

        when(lgwClient.getStocks(3, 10, new Partner(1L)))
                .thenReturn(Collections.emptyList());
        when(stockUpdatingStrategyProvider.provide(1)).thenReturn(stockUpdatingStrategy);

        syncWarehouseStrategy.execute(payload);
        verify(lgwClient, times(1)).getStocks(3, 7, new Partner(1L));
        verify(lgwClient, times(1)).getStocks(3, 10, new Partner(1L));
        verify(stockUpdatingStrategy, times(1)).syncStock(any(Source.class), anyList());
    }

    @Test
    public void syncLastBatch() {
        WarehouseAwareExecutionQueuePayload payload = new WarehouseAwareExecutionQueuePayload(7L, 10L, 3, true, 1);
        when(lgwClient.getStocks(3, 7, new Partner(1L)))
                .thenReturn(itemStocksByNormalPerson());
        List<ItemStocks> notFullBatch = new ArrayList<>(itemStocksByNormalPerson());
        notFullBatch.remove(0);

        when(lgwClient.getStocks(3, 10, new Partner(1L)))
                .thenReturn(notFullBatch);
        when(stockUpdatingStrategyProvider.provide(1)).thenReturn(stockUpdatingStrategy);

        syncWarehouseStrategy.execute(payload);
        verify(lgwClient, times(1)).getStocks(3, 7, new Partner(1L));
        verify(lgwClient, times(1)).getStocks(3, 10, new Partner(1L));
        verify(stockUpdatingStrategy, times(2)).syncStock(any(Source.class), anyList());
    }

    @Test
    public void syncLastBatchBatchHasError() {
        WarehouseAwareExecutionQueuePayload payload = new WarehouseAwareExecutionQueuePayload(7L, 10L, 3, true, 1);
        when(stockUpdatingStrategyProvider.provide(1)).thenReturn(stockUpdatingStrategy);
        when(lgwClient.getStocks(3, 7, new Partner(1L)))
                .thenReturn(itemStocksByNormalPerson());
        when(lgwClient.getStocks(3, 10, new Partner(1L)))
                .thenThrow(new RuntimeException("Some shit"));

        Assertions.assertThrows(FullSyncFailedException.class,
                () -> syncWarehouseStrategy.execute(payload), "Some shit");

        verify(lgwClient).getStocks(3, 7, new Partner(1L));
        verify(lgwClient, times(2)).getStocks(3, 10, new Partner(1L)); // 2 раза из-за ретраев
        verifyNoMoreInteractions(lgwClient);
    }

    private List<ItemStocks> itemStocksByNormalPerson() {
        return Arrays.asList(
                new ItemStocks(new UnitId("sku0", 1L, "sku0"), resourceId("1", "1"), Collections.emptyList()),
                new ItemStocks(new UnitId("sku1", 1L, "sku1"), resourceId("1", "1"), Collections.emptyList()),
                new ItemStocks(new UnitId("sku2", 1L, "sku2"), resourceId("1", "1"), Collections.emptyList())
        );
    }

    @SuppressWarnings("SameParameterValue")
    private static ResourceId resourceId(String partnerId, String yandexId) {
        return ResourceId.builder().setPartnerId(partnerId).setYandexId(yandexId).build();
    }


}
