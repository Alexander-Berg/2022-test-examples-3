package ru.yandex.market.logistics.iris.service.item;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.ir.http.MdmIrisPayload;
import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.complex.Dimension;
import ru.yandex.market.logistics.iris.core.index.complex.Dimensions;
import ru.yandex.market.logistics.iris.core.index.dummy.TestPredefinedField;
import ru.yandex.market.logistics.iris.core.index.field.PredefinedFields;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.entity.EmbeddableItemNaturalKey;
import ru.yandex.market.logistics.iris.entity.EmbeddableSource;
import ru.yandex.market.logistics.iris.entity.item.EmbeddableItemIdentifier;
import ru.yandex.market.logistics.iris.entity.item.ItemEntity;
import ru.yandex.market.logistics.iris.repository.ItemRepository;
import ru.yandex.market.logistics.iris.service.logbroker.producer.LogBrokerPushService;
import ru.yandex.market.request.trace.RequestContext;
import ru.yandex.market.request.trace.RequestContextHolder;

import static com.github.springtestdbunit.annotation.DatabaseOperation.DELETE_ALL;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.Mockito.when;

public class ItemServiceTest extends AbstractContextualTest {

    @MockBean
    private LogBrokerPushService logBrokerPushService;

    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private ChangeTrackingReferenceIndexer referenceIndexer;

    @Captor
    private ArgumentCaptor<byte[]> eventsCaptor;

    @Before
    public void init() {
        when(yt.tables()).thenReturn(Mockito.mock(YtTables.class));
        RequestContextHolder.clearContext();
    }

    @After
    public void destroy() {
        Mockito.reset(yt.tables());
    }

    /**
     * Сценарий #1:
     * <p>
     * Сохраняем информацию по изменениям нового товара в БД.
     * У него были проставлены значения полей dummy и yummy для складов 145 и 147 соответственно.
     * <p>
     * В результате должны увидеть 1 новую запись в таблице item, а так же две записи в таблице item_change.
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_service/1.xml", type = DELETE_ALL)
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_service/1.xml", assertionMode = NON_STRICT_UNORDERED)
    public void saveNewItemWithChanges() {
        RequestContextHolder.setContext(new RequestContext("111"));

        ItemEntity entity = new ItemEntity().setNaturalKey(
            new EmbeddableItemNaturalKey()
                .setSource(new EmbeddableSource("147", SourceType.WAREHOUSE))
                .setIdentifier(new EmbeddableItemIdentifier("1", "sku1"))
        );

        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(TestPredefinedField.DUMMY, "dummy_value", ZonedDateTime.of(LocalDateTime.of(2011, 12, 3, 10, 15, 30), ZoneId.of("-05:00")));
        index.set(TestPredefinedField.YUMMY, "yummy_value", ZonedDateTime.of(LocalDateTime.of(2012, 12, 3, 11, 15, 30), ZoneId.of("-05:00")));

        itemService.save(entity, index);
    }

    /**
     * Сценарий #2:
     * <p>
     * Пытаемся сохранить изменения по существующему товару в БД.
     * <p>
     * Удаляем оба поля.
     * <p>
     * В результате в таблице товара - должны записать пустой индекс.
     * В таблице изменений должны быть записаны два дополнительных изменения.
     */
    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_service/2.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_service/2.xml", assertionMode = NON_STRICT_UNORDERED)
    public void saveExistingItemWithChanges() {
        RequestContextHolder.setContext(new RequestContext("222"));

        ItemEntity itemEntity = itemRepository.findById(1L).get();

        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(itemEntity.getReferenceIndex());
        index.remove(TestPredefinedField.DUMMY);
        index.remove(TestPredefinedField.YUMMY);

        itemService.save(itemEntity, index);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_service/3.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_service/3.xml", assertionMode = NON_STRICT_UNORDERED)
    public void saveExistingItemAndSendDimensionsToLogBroker() throws InvalidProtocolBufferException {
        RequestContextHolder.setContext(new RequestContext("222"));

        ItemEntity itemEntity = itemRepository.findById(1L).get();

        ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(itemEntity.getReferenceIndex());
        index.set(PredefinedFields.DIMENSIONS, new Dimensions(
                Dimension.of(BigDecimal.valueOf(200)),
                Dimension.of(BigDecimal.valueOf(30)),
                Dimension.of(BigDecimal.valueOf(10))),
                ZonedDateTime.of(LocalDateTime.parse("2021-01-22T12:50:37.409"), ZoneOffset.UTC));

        itemService.save(itemEntity, index);

        Mockito.verify(logBrokerPushService).push(eventsCaptor.capture());
        String parsedBatchString = MdmIrisPayload.ItemBatch.parseFrom(eventsCaptor.getValue()).toString();
        assertions().assertThat(parsedBatchString).contains(
                extractFileContent("fixtures/data/item_service/logbroker_payload.txt"));
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/setup/item_service/7.xml")
    @ExpectedDatabase(value = "classpath:fixtures/expected/item_service/7.xml", assertionMode = NON_STRICT_UNORDERED)
    public void saveNewItemWithChangesAllServices() {
        RequestContextHolder.setContext(new RequestContext("111"));

        ItemEntity entity = new ItemEntity().setNaturalKey(
                new EmbeddableItemNaturalKey()
                        .setSource(new EmbeddableSource("147", SourceType.WAREHOUSE))
                        .setIdentifier(new EmbeddableItemIdentifier("1", "sku1"))
        );

        ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
        index.set(TestPredefinedField.DUMMY, "dummy_value", ZonedDateTime.of(LocalDateTime.of(2011, 12, 3, 10, 15, 30), ZoneId.of("-05:00")));
        index.set(TestPredefinedField.YUMMY, "yummy_value", ZonedDateTime.of(LocalDateTime.of(2012, 12, 3, 11, 15, 30), ZoneId.of("-05:00")));

        itemService.save(entity, index);
    }
}
