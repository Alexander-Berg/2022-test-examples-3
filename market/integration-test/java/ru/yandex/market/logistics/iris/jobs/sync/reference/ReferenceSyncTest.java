package ru.yandex.market.logistics.iris.jobs.sync.reference;

import java.math.BigDecimal;
import java.util.Collections;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Barcode;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.BarcodeSource;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ItemReference;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Korobyte;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.UnitId;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.Source;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.jobs.QueueTypeConfigService;
import ru.yandex.market.logistics.iris.jobs.consumers.sync.ReferenceItemSyncService;
import ru.yandex.market.logistics.iris.jobs.model.LoopingQueueItemPayload;
import ru.yandex.market.logistics.iris.jobs.model.QueueType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class ReferenceSyncTest extends AbstractContextualTest {

    private static final Source FIRST_WAREHOUSE = new Source("1", SourceType.WAREHOUSE);

    private static final ItemReference ITEM_REFERENCE = new ItemReference(
            new UnitId("", 1L, "sku"),
            new Korobyte(50, 51, 52, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.valueOf(123)),
            100,
            Sets.newHashSet(new Barcode("code", "type", BarcodeSource.SUPPLIER)),
            null
    );
    private static final Partner FIRST_WAREHOUSE_PARTNER = new Partner(1L);

    @Autowired(required = false)
    private ReferenceItemSyncService syncService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private QueueTypeConfigService queueTypeConfigService;

    /**
     * Проверяем, что в результате синка будут:
     * 1. Успешно создана запись с новым товармом
     * 2. Будут записаны значения следующих полей
     * - lifetime
     * - ВГХ
     * - barcodes.
     */
    @Test
    @ExpectedDatabase(assertionMode = NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/sync/reference/1.xml")
    public void referenceInformationCreated() {
        executeScenario();
    }

    /**
     * Проверяем, что в результате синка будут:
     * Успешно обновлена запись с новым товаром и ее индекс
     * - lifetime
     * - ВГХ
     * - barcodes.
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/sync/reference/2.xml")
    @ExpectedDatabase(assertionMode = NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/sync/reference/2.xml")
    public void referenceInformationUpdated() {
        executeScenario();
    }

    private void executeScenario() {
        int batchSize = queueTypeConfigService.getBatchSize(QueueType.REFERENCE_SYNC);

        doReturn(Collections.singletonList(ITEM_REFERENCE)).when(fulfillmentClient).getReferenceItems(
                batchSize, 0, FIRST_WAREHOUSE_PARTNER
        );

        doReturn(Collections.emptyList()).when(fulfillmentClient).getReferenceItems(
                batchSize, batchSize, FIRST_WAREHOUSE_PARTNER
        );

        transactionTemplate.execute(tx -> {
            syncService.processPayload(new LoopingQueueItemPayload("", batchSize, FIRST_WAREHOUSE));
            return null;
        });

        verify(fulfillmentClient).getReferenceItems(eq(batchSize), eq(0), eq(FIRST_WAREHOUSE_PARTNER));
        verify(fulfillmentClient).getReferenceItems(eq(batchSize), eq(batchSize), eq(FIRST_WAREHOUSE_PARTNER));
    }
}
