package ru.yandex.market.fulfillment.stockstorage;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.fulfillment.stockstorage.service.stocks.LastPageStockJobQueueSync;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

public class LastPageStockSyncTest extends AbstractContextualTest {

    @Autowired
    private LastPageStockJobQueueSync lastPageStockJobQueueSync;

    @Autowired
    private FulfillmentClient lgwClient;

    @Test
    @DatabaseSetup("classpath:database/states/last_page_stock_sync/1.xml")
    public void consumeEmptyDatabaseBatch() {
        lastPageStockJobQueueSync.consume();
        Mockito.verify(lgwClient).getStocks(eq(20), eq(0), any(Partner.class));
    }

    @Test
    public void onEmptyDatabaseNoRequestsMade() {
        lastPageStockJobQueueSync.consume();
        Mockito.verify(lgwClient, Mockito.never()).getStocks(anyInt(), anyInt(), any(Partner.class));
    }
}
