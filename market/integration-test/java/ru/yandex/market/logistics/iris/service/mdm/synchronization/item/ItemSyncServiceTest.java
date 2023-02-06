package ru.yandex.market.logistics.iris.service.mdm.synchronization.item;

import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.index.ImmutableReferenceIndex;
import ru.yandex.market.logistics.iris.entity.EmbeddableItemNaturalKey;
import ru.yandex.market.logistics.iris.service.index.ReferenceIndexMergeService;
import ru.yandex.market.logistics.iris.util.UtcTimestampProvider;
import ru.yandex.market.request.trace.RequestContextHolder;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.times;

public class ItemSyncServiceTest extends AbstractContextualTest {

    private static final String REQUEST_ID = "TestRequestId";

    private static final ZonedDateTime UPDATED_DATE_TIME = ZonedDateTime.of(
            LocalDate.of(1970, 1, 2).atStartOfDay(),
            ZoneOffset.UTC
    );

    @Autowired
    private ItemSyncService itemSyncService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @SpyBean
    private UtcTimestampProvider utcTimestampProvider;

    @SpyBean
    private ReferenceIndexMergeService referenceIndexMergeService;

    @Captor
    private ArgumentCaptor<Map<EmbeddableItemNaturalKey, ImmutableReferenceIndex>> captor;

    @Before
    public void init() {
        RequestContextHolder.createContext(Optional.of(REQUEST_ID));
    }

    /**
     * Сценарий #1:
     * <p>
     * Подается батч c null телом.
     * <p>
     * Ожидается пустая очередь QueueTasks в БД.
     */
    @Test
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_sync_service/1.xml", assertionMode =
            NON_STRICT_UNORDERED)
    public void shouldNotCreateAnyReferenceIndexMergerQueueTask() {
        transactionTemplate.execute(tx -> {
            itemSyncService.process(createEmptyMessageBatch());
            return null;
        });
    }

    /**
     * Сценарий #2:
     * <p>
     * Подается батч  c ItemBatch в котором находятся два Item-ма.
     * <p>
     * Ожидается создание задачи на сохранение двух Item-ов в БД.
     */
    @Test
    public void shouldCreateReferenceIndexMergerQueueTaskWithTwoItems() {
        Mockito.doReturn(UPDATED_DATE_TIME).when(utcTimestampProvider).getCurrentUtcTimestamp();

        transactionTemplate.execute(tx -> {
            itemSyncService.process(createMessageBatchWithItemBatch());
            return null;
        });

        Mockito.verify(referenceIndexMergeService, times(1)).mergeAsync(captor.capture());

        Map<EmbeddableItemNaturalKey, ImmutableReferenceIndex> values = captor.getValue();
        assertSoftly(assertions -> assertions.assertThat(values.size()).isEqualTo(2));
    }

    private MessageBatch createEmptyMessageBatch() {
        return new MessageBatch("topic", 1, null);
    }

    private MessageBatch createMessageBatchWithItemBatch() {
        return new MessageBatch(
                "topic",
                1,
                Collections.singletonList(new MessageData(createRawItemBatch(), 0, createMeta()))
        );
    }

    private byte[] createRawItemBatch() {
        return MdmIrisPayload.ItemBatch.newBuilder()
                .addItem(createItem(1L, "shopSku1", 1L))
                .addItem(createItem(2L, "shopSku2", 2L))
                .build()
                .toByteArray();
    }

    private MdmIrisPayload.Item createItem(long supplierId, String shopSku, long weightNet) {
        return MdmIrisPayload.Item.newBuilder()
                .setItemId(
                        MdmIrisPayload.MdmIdentifier.newBuilder()
                                .setSupplierId(supplierId)
                                .setShopSku(shopSku)
                                .build()
                )
                .addInformation(MdmIrisPayload.ReferenceInformation.newBuilder()
                        .setSource(MdmIrisPayload.Associate.newBuilder().setType(MdmIrisPayload.MasterDataSource.MDM))
                        .setItemShippingUnit(MdmIrisPayload.ShippingUnit.newBuilder()
                                .setWeightGrossMg(MdmIrisPayload.Int64Value.newBuilder()
                                        .setValue(weightNet)
                                        .setUpdatedTs(86400000)
                                )
                        )
                )
                .build();
    }

    private MessageMeta createMeta() {
        return new MessageMeta(null, 1, 0, 0, null, null, null);
    }
}
