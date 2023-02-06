package ru.yandex.market.logistics.iris.jobs.sync.expiration;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Expiration;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemExpiration;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Stock;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.configuration.LGWExchangeConfiguration;
import ru.yandex.market.logistics.iris.configuration.LmsClientConfiguration;
import ru.yandex.market.logistics.iris.configuration.queue.DbQueueConfiguration;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.jobs.consumers.sync.StockLifetimeSyncService;
import ru.yandex.market.logistics.iris.jobs.model.IdentifiableExecutionQueueItemPayload;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime.fromLocalDateTime;

@Import({DbQueueConfiguration.class, LmsClientConfiguration.class, LGWExchangeConfiguration.class})
public class StockLifetimeSyncTest extends AbstractContextualTest {
    private static final Source FIRST_WAREHOUSE = new Source("1", SourceType.WAREHOUSE);

    private static final List<UnitId> UNIT_IDS = Collections.singletonList(new UnitId(null, 1L, "sku"));

    private static final DateTime MANUFACTURED_DATE = fromLocalDateTime(LocalDateTime.of(1970, 1, 1, 0, 0));

    private static final DateTime DEFAULT_UPDATED_VALUE = fromLocalDateTime(LocalDateTime.of(1970, 1, 1, 0, 0));

    private static final ItemExpiration ITEM_EXPIRATION = new ItemExpiration(
        new UnitId("", 1L, "sku"),
        Collections.singletonList(new Expiration(MANUFACTURED_DATE, Collections.singletonList(new Stock(StockType.FIT, 10, DEFAULT_UPDATED_VALUE)))));

    private static final Partner FIRST_WAREHOUSE_PARTNER = new Partner(1L);

    @Autowired
    private StockLifetimeSyncService syncService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DatabaseSetup("classpath:fixtures/setup/sync/expiration/1.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/sync/expiration/1.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void stockLifeTimeHasBeenSynced() {

        doReturn(Collections.singletonList(ITEM_EXPIRATION)).when(stocksService).getExpirationItems(
            null, null, UNIT_IDS, FIRST_WAREHOUSE_PARTNER
        );

        transactionTemplate.execute(tx -> {
            syncService.processPayload(new IdentifiableExecutionQueueItemPayload("", 1L, 2L, FIRST_WAREHOUSE));
            return null;
        });

        verify(stocksService).getExpirationItems(eq(null), eq(null), eq(UNIT_IDS), eq(FIRST_WAREHOUSE_PARTNER));
    }
}
