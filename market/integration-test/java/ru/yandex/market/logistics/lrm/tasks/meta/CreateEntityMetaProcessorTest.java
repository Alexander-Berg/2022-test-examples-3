package ru.yandex.market.logistics.lrm.tasks.meta;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.queue.payload.meta.CreateEntityMetaPayload;
import ru.yandex.market.logistics.lrm.queue.payload.meta.ReturnSegmentShipmentChangedMetaWrapper;
import ru.yandex.market.logistics.lrm.queue.processor.CreateEntityMetaProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.model.ReturnSegmentShipmentChangedMeta;
import ru.yandex.market.ydb.integration.YdbTableDescription;

@DisplayName("Сохранение мета-информации для сущностей в YDB")
class CreateEntityMetaProcessorTest extends AbstractIntegrationYdbTest {
    private static final int RETURN_SEGMENT_11_HASH = -1057768114;
    private static final int RETURN_SEGMENT_21_HASH = -760182950;

    @Autowired
    private EntityMetaTableDescription entityMetaTable;

    @Autowired
    private CreateEntityMetaProcessor createEntityMetaProcessor;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTable);
    }

    @BeforeEach
    void setUp() {
        clock.setFixed(Instant.parse("2022-01-01T12:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE);
    }

    @Test
    @DisplayName("Успех: мета сохранена в YDB")
    @DatabaseSetup("/database/tasks/meta/before/return_segments.xml")
    void success() {
        execute(EntityType.RETURN_SEGMENT);

        softly.assertThat(getReturnSegmentShipmentChangedMeta(11L, RETURN_SEGMENT_11_HASH)).contains(
            ReturnSegmentShipmentChangedMeta.builder()
                .datetime(Instant.now(clock))
                .build()
        );

        softly.assertThat(getReturnSegmentShipmentChangedMeta(21L, RETURN_SEGMENT_21_HASH)).contains(
            ReturnSegmentShipmentChangedMeta.builder()
                .datetime(Instant.now(clock))
                .build()
        );
    }

    private void execute(EntityType entityType) {
        createEntityMetaProcessor.execute(createEntityMetaPayload(entityType));
    }

    @Nonnull
    private CreateEntityMetaPayload createEntityMetaPayload(EntityType entityType) {
        return CreateEntityMetaPayload.builder()
            .entityType(entityType)
            .entityMetaWrappers(
                List.of(
                    ReturnSegmentShipmentChangedMetaWrapper.builder()
                        .entityId(11L)
                        .entityMeta(createShipmentChangedMeta(Instant.now(clock)))
                        .build(),
                    ReturnSegmentShipmentChangedMetaWrapper.builder()
                        .entityId(21L)
                        .entityMeta(createShipmentChangedMeta(Instant.now(clock)))
                        .build()
                )
            )
            .build();
    }

    @Nonnull
    private ReturnSegmentShipmentChangedMeta createShipmentChangedMeta(Instant datetime) {
        return ReturnSegmentShipmentChangedMeta.builder()
            .datetime(datetime)
            .build();
    }

    @Nonnull
    private Optional<ReturnSegmentShipmentChangedMeta> getReturnSegmentShipmentChangedMeta(long entityId, int hash) {
        return getEntityMetaRecord(hash, "RETURN_SEGMENT", entityId, "return-segment-shipment-changed")
            .map(EntityMetaTableDescription.EntityMetaRecord::value)
            .map(v -> readValue(v, ReturnSegmentShipmentChangedMeta.class));
    }
}
