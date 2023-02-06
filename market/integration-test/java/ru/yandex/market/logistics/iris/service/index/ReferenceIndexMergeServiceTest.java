package ru.yandex.market.logistics.iris.service.index;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.iris.configuration.AbstractContextualTest;
import ru.yandex.market.logistics.iris.converter.ItemNaturalKeyConverter;
import ru.yandex.market.logistics.iris.core.domain.source.SourceType;
import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;
import ru.yandex.market.logistics.iris.entity.EmbeddableItemNaturalKey;
import ru.yandex.market.logistics.iris.jobs.model.index.ReferenceIndexMergePayloadItem;
import ru.yandex.market.logistics.iris.jobs.model.index.ReferenceIndexMergeQueuePayload;
import ru.yandex.market.logistics.iris.model.ItemIdentifierDTO;
import ru.yandex.market.logistics.iris.model.ItemNaturalKeyDTO;
import ru.yandex.market.logistics.iris.model.SourceDTO;
import ru.yandex.market.request.trace.RequestContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public class ReferenceIndexMergeServiceTest extends AbstractContextualTest {
    private final String PAYLOAD = "{\"dimensions\": {\"value\": {\"width\": {\"value\": 15.2, " +
            "\"data_quality_score\": 10}, \"height\": {\"value\": 19, \"data_quality_score\": 0}}, \"utcTimestamp\": " +
            "\"2010-01-23T12:34:56\"}}";

    private final String PAYLOAD_WITH_MEASUREMENT_STATE = "{\"dimensions\": {\"value\": {\"width\": {\"value\": 15.2," +
            " \"data_quality_score\": 10}, \"height\": {\"value\": 19, \"data_quality_score\": 0}}, \"utcTimestamp\":" +
            " \"2010-01-23T12:34:56\"}, \"measurement_state\": {\"value\": {\"already_measured\": true," +
            "\"last_measured_timestamp\": 1567811008826},\"utcTimestamp\": \"2019-09-06T23:03:28.826\"}}";
    private final String PAYLOAD_WITH_TRUSTWORTHY_VERSION = "{\"dimensions\": {\"value\": {\"width\": " +
            "{\"value\": 15.2, \"data_quality_score\": 10}, \"height\": {\"value\": 19, \"data_quality_score\": 0}}, " +
            "\"utcTimestamp\": \"2010-01-23T12:34:56\"}, \"weight_gross\":{\"value\":{\"value\":120.000," +
            "\"data_quality_score\":0},\"utcTimestamp\":\"2020-12-01T10:27:03.752\"}, " +
            "\"trustworthy_version\":{\"value\":1606870637523,\"utcTimestamp\":\"1970-01-01T00:00:00.001\"}}";
    private final String SMALL_PAYLOAD = "{\"box_capacity\": {\"value\": 5,\"utcTimestamp\": \"1970-01-02T00:00:00\"}}";


    private final String CHANGE_MSKU_PAYLOAD = "{\"msku\" : {\"value\": 888,\"utcTimestamp\": \"2021-06-16T08:48:20\"}," + " \"box_capacity\": {\"value\": 4,\"utcTimestamp\": \"2021-06-16T08:48:20\"}}";
    private final String REQUEST_ID = "TestRequestId";

    @Autowired
    private ReferenceIndexMergeServiceImpl referenceIndexMergeService;

    @Autowired
    private ChangeTrackingReferenceIndexer indexer;

    @Before
    public void init() {
        RequestContextHolder.createContext(REQUEST_ID);
    }

    /**
     * Проверяем, что при мердже Item-а, пришедшего с типом MDM и отсутствующего в БД, будет:
     * 1. Создан Item с типом MDM
     * 2. Выполнится мердж индекса в созданный Item
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/index/merge/1/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/1/items.xml")
    public void shouldMergedWithCreatingMdmItem() {
        ReferenceIndexMergeQueuePayload payload = createPayload(PAYLOAD, getMdmSource());

        referenceIndexMergeService.processPayload(payload);
    }

    /**
     * Проверяем, что при мердже Item-а, пришедшего с типом MDM и имеющий изменения в ВГХ,
     * изменения буду вмерджены в существующий Item.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/index/merge/2/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/2/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/2/queue_tasks.xml")
    public void shouldMergeItemWithChangesInDimensionWidth() {
        ReferenceIndexMergeQueuePayload payload = createPayload(PAYLOAD, getMdmSource());

        referenceIndexMergeService.processPayload(payload);
    }

    /**
     * Проверяем, что при мердже Item-а с measurement_state, пришедшего с типом MDM и отсутствующего в бд будет:
     * 1. Создан Item с типом MDM
     * 2. Выполнится мердж индекса в созданный Item
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/index/merge/1/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/8/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/queue_tasks.xml")
    public void shouldCreateItemWithChangesInMeasurementState() {
        ReferenceIndexMergeQueuePayload payload = createPayload(PAYLOAD_WITH_MEASUREMENT_STATE, getMdmSource());

        referenceIndexMergeService.processPayload(payload);
    }

    /**
     * Проверяем, что при мердже Item-а, пришедшего с типом MDM и имеющий изменения в measurement_state
     * alreadyMeasured false -> true,
     * изменения буду вмерджены в существующий Item.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/index/merge/8/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/8/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/queue_tasks.xml")
    public void shouldMergeItemWithChangesInMeasurementState() {
        ReferenceIndexMergeQueuePayload payload = createPayload(PAYLOAD_WITH_MEASUREMENT_STATE, getMdmSource());

        referenceIndexMergeService.processPayload(payload);
    }

    /**
     * Проверяем, что при мердже Item-а, пришедшего с типом MDM и имеющий изменения в measurement_state если изменился
     * только lastMeasuredTimestamp
     * изменения буду вмерджены в существующий Item.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/index/merge/8/items_old_timestamp.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/8/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/queue_tasks.xml")
    public void shouldMergeItemWithChangesInMeasurementUtcTimestamp() {
        ReferenceIndexMergeQueuePayload payload = createPayload(PAYLOAD_WITH_MEASUREMENT_STATE, getMdmSource());

        referenceIndexMergeService.processPayload(payload);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/index/merge/3/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/3/items.xml")
    public void shouldMergeItemWithoutChanges() {
        ReferenceIndexMergeQueuePayload payload = createPayload(PAYLOAD, getMdmSource());

        referenceIndexMergeService.processPayload(payload);
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/index/merge/4/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/4/items.xml")
    public void shouldSuccessMergeItemWithTrustworthyVersion() {
        ReferenceIndexMergeQueuePayload payload = createPayload(PAYLOAD_WITH_TRUSTWORTHY_VERSION, getMdmSource());

        referenceIndexMergeService.processPayload(payload);
    }

    /**
     * Проверяем, что при async мердже Item-а, пришедшего с типом DataCamp и имеющие изменений,
     * мердж выполнен не будет.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/index/merge/5/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/5/items.xml")
    public void shouldNotMergeItemAsyncFilteringSource() {

        ChangeTrackingReferenceIndex index = indexer.fromJson(SMALL_PAYLOAD);
        EmbeddableItemNaturalKey key = getItemNaturalKey(getDataCampSskuSource());

        referenceIndexMergeService.mergeAsync(Map.of(key, index));
    }

    /**
     * Проверяем, что при sync мердже Item-а, пришедшего с типом DataCamp и имеющие изменений,
     * мердж выполнен не будет.
     */
    @Test
    @DatabaseSetup("classpath:fixtures/setup/index/merge/6/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/6/items.xml")
    public void shouldNotMergeItemFilteringSource() {

        ChangeTrackingReferenceIndex index = indexer.fromJson(SMALL_PAYLOAD);
        EmbeddableItemNaturalKey key = getItemNaturalKey(getDataCampSskuSource());

        referenceIndexMergeService.merge(Map.of(key, index));
    }

    @Test
    @DatabaseSetup("classpath:fixtures/setup/index/merge/7/items.xml")
    @ExpectedDatabase(assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
            value = "classpath:fixtures/expected/index/merge/7/items.xml")
    public void shouldChangeMsku() {
        ChangeTrackingReferenceIndex index = indexer.fromJson(CHANGE_MSKU_PAYLOAD);
        EmbeddableItemNaturalKey key = getItemNaturalKey(getDataCampSskuSource());
        referenceIndexMergeService.merge(Map.of(key, index));
    }

    private ReferenceIndexMergeQueuePayload createPayload(String payload, SourceDTO sourceDTO) {
        return new ReferenceIndexMergeQueuePayload(
                null,
                createPayloadItems(payload, sourceDTO)
        );
    }

    private List<ReferenceIndexMergePayloadItem> createPayloadItems(String payload, SourceDTO sourceDTO) {
        return Collections.singletonList(createPayloadItem(payload, sourceDTO));
    }

    private ReferenceIndexMergePayloadItem createPayloadItem(String payload, SourceDTO sourceDTO) {
        return new ReferenceIndexMergePayloadItem(payload, getKey(sourceDTO));
    }

    @Nonnull
    private ItemNaturalKeyDTO getKey(SourceDTO sourceDTO) {
        return new ItemNaturalKeyDTO(
                new ItemIdentifierDTO("1", "partner_sku_1"),
                sourceDTO
        );
    }

    @Nonnull
    private EmbeddableItemNaturalKey getItemNaturalKey(SourceDTO dataCampSskuSource) {
        return ItemNaturalKeyConverter.fromDTOToEmbeddable(getKey(dataCampSskuSource));
    }

    @Nonnull
    private SourceDTO getMdmSource() {
        return new SourceDTO("1", SourceType.MDM);
    }

    @Nonnull
    private SourceDTO getDataCampSskuSource() {
        return new SourceDTO("1", SourceType.DATACAMP_SSKU);
    }
}
