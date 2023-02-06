package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.time.Instant;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.util.DateTimeUtils;
import ru.yandex.market.logistics.lrm.AbstractIntegrationYdbTest;
import ru.yandex.market.logistics.lrm.config.properties.FeatureProperties;
import ru.yandex.market.logistics.lrm.model.entity.enums.EntityType;
import ru.yandex.market.logistics.lrm.model.entity.enums.ReturnSegmentStatus;
import ru.yandex.market.logistics.lrm.model.entity.enums.StatusSource;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnSegmentStatusesPayload;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnSegmentStatusesPayload.Segment;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnSegmentStatusesPayload.Status;
import ru.yandex.market.logistics.lrm.queue.processor.ReturnSegmentStatusesProcessor;
import ru.yandex.market.logistics.lrm.repository.ydb.description.EntityMetaTableDescription;
import ru.yandex.market.logistics.lrm.service.meta.DetachedTypedEntity;
import ru.yandex.market.logistics.lrm.service.meta.EntityMetaService;
import ru.yandex.market.logistics.lrm.service.meta.model.ReturnSegmentShipmentChangedMeta;
import ru.yandex.market.ydb.integration.YdbTableDescription;

@DisplayName("Сохранение статусов сегментов возврата")
@DatabaseSetup("/database/tasks/return-segment/process-statuses/before/minimal.xml")
class ReturnSegmentStatusesProcessorTest extends AbstractIntegrationYdbTest {

    private static final Instant BASE = Instant.parse("2021-12-23T10:11:12Z");

    @Autowired
    private ReturnSegmentStatusesProcessor processor;

    @Autowired
    private FeatureProperties featureProperties;

    @Autowired
    private EntityMetaTableDescription entityMetaTable;

    @Autowired
    private EntityMetaService entityMetaService;

    @Nonnull
    @Override
    protected List<YdbTableDescription> getTablesForSetUp() {
        return List.of(entityMetaTable);
    }

    @BeforeEach
    void setUp() {
        clock.setFixed(BASE, DateTimeUtils.MOSCOW_ZONE);
    }

    @AfterEach
    void tearDown() {
        featureProperties.setEnableControlPointUseInGetRoute(false);
    }

    @Test
    @DisplayName("Неизвестный сегмент")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/no_statuses.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void ignoreUnknownSegment() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-2")
                        .status(Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE).build())
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Несколько статусов одного сегмента")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/multiple_statuses.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleIncomingStatuses() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segments(List.of(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE).build())
                        .build(),
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE).build())
                        .status(Status.builder().status(ReturnSegmentStatus.OUT).timestamp(BASE.plusSeconds(1)).build())
                        .build()
                ))
                .build()
        );
    }

    @Test
    @DisplayName("Cтатус добавленный вручную")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/manual_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void manualStatus() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segments(List.of(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(Status.builder().status(ReturnSegmentStatus.OUT).timestamp(BASE).build())
                        .build()
                ))
                .statusSource(StatusSource.MANUAL)
                .historyMessage("Очень нужен статус")
                .build()
        );
    }

    @Test
    @DisplayName("Несколько сегментов")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/second_return.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/multiple_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleSegments() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segments(List.of(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE).build())
                        .build(),
                    Segment.builder()
                        .uniqueId("segment-2")
                        .status(Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE).build())
                        .status(Status.builder().status(ReturnSegmentStatus.OUT).timestamp(BASE.plusSeconds(1)).build())
                        .build()
                ))
                .build()
        );
    }

    @Test
    @DisplayName("Уже существовавший статус игнорируется")
    @DatabaseSetup("/database/tasks/return-segment/process-statuses/before/existing_status.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/existing_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void existingStatusIgnored() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE).build())
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Статус из прошлого")
    @DatabaseSetup("/database/tasks/return-segment/process-statuses/before/existing_status.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/old_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void oldStatus() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(
                            Status.builder()
                                .status(ReturnSegmentStatus.CREATED)
                                .timestamp(BASE.minusSeconds(1))
                                .build()
                        )
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Несколько статусов в один момент времени")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/multiple_statuses_same_datetime.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleStatusesSameDatetime() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .statusHistory(List.of(
                            Status.builder().status(ReturnSegmentStatus.CREATED).timestamp(BASE).build(),
                            Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE).build()
                        ))
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Не запрашиваем маршрут для события сортировки из ПВЗ")
    @DatabaseSetup(
        type = DatabaseOperation.UPDATE,
        value = "/database/tasks/return-segment/process-statuses/before/pickup_segment.xml"
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/pickup_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transitPreparedFromPickup() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(Status.builder().status(ReturnSegmentStatus.TRANSIT_PREPARED).timestamp(BASE).build())
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Событие сортировки на последнем сегменте")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared_last_segment.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transitPreparedLastSegment() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(Status.builder().status(ReturnSegmentStatus.TRANSIT_PREPARED).timestamp(BASE).build())
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Событие сортировки на последней миле магазина")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/second_segment_shop.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared_last_mile.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transitPreparedLastMileShop() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(Status.builder().status(ReturnSegmentStatus.TRANSIT_PREPARED).timestamp(BASE).build())
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Событие сортировки на последней миле ФФ")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/second_segment_ff.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared_last_mile.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transitPreparedLastMileFulfillment() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(Status.builder().status(ReturnSegmentStatus.TRANSIT_PREPARED).timestamp(BASE).build())
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Событие сортировки на средней миле")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/second_segment.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transitPrepared() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .statusHistory(List.of(
                            Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE.minusSeconds(1)).build(),
                            Status.builder().status(ReturnSegmentStatus.TRANSIT_PREPARED).timestamp(BASE).build()
                        ))
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Событие отгрузки на средней миле")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/second_segment.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/out.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void out() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .statusHistory(List.of(
                            Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE.minusSeconds(1)).build(),
                            Status.builder().status(ReturnSegmentStatus.OUT).timestamp(BASE.plusSeconds(1)).build()
                        ))
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Событие отгрузки после сортировки на средней миле")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/second_segment.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup("/database/tasks/return-segment/process-statuses/before/transit_prepared.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/out_existing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void outOnExistingTransferPrepared() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(
                            Status.builder()
                                .status(ReturnSegmentStatus.OUT)
                                .timestamp(BASE.plusSeconds(1))
                                .build()
                        )
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Несколько событий сортировки на средней миле")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/second_segment.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared_multiple.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void multipleTransitPrepared() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .statusHistory(List.of(
                            Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE.minusSeconds(1)).build(),
                            Status.builder().status(ReturnSegmentStatus.TRANSIT_PREPARED).timestamp(BASE).build(),
                            Status.builder()
                                .status(ReturnSegmentStatus.TRANSIT_PREPARED)
                                .timestamp(BASE.plusSeconds(1))
                                .build()
                        ))
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Была пересортировка (Дропофф): TRANSIT_PREPARED_OLD - SHIPMENT_CHANGED - TRANSIT_PREPARED_NEW")
    @DatabaseSetup(
        value = {
            "/database/tasks/return-segment/process-statuses/before/second_segment_dropoff.xml"
        },
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup("/database/tasks/return-segment/process-statuses/before/transit_prepared.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared_existing_before_resort.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transitPreparedOld_ChangedShipmentEvent_transitPreparedNew_Dropoff() {
        entityMetaService.save(
            new DetachedTypedEntity(EntityType.RETURN_SEGMENT, 1L),
            ReturnSegmentShipmentChangedMeta.builder().datetime(BASE.plusSeconds(1)).build()
        );

        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .statusHistory(List.of(
                            Status.builder()
                                .status(ReturnSegmentStatus.TRANSIT_PREPARED)
                                .timestamp(BASE.plusSeconds(2))
                                .build()
                        ))
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Была пересортировка: TRANSIT_PREPARED_OLD - SHIPMENT_CHANGED - TRANSIT_PREPARED_NEW")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/second_segment.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup("/database/tasks/return-segment/process-statuses/before/transit_prepared.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared_existing_before_resort.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transitPreparedOld_ChangedShipmentEvent_transitPreparedNew() {
        entityMetaService.save(
            new DetachedTypedEntity(EntityType.RETURN_SEGMENT, 1L),
            ReturnSegmentShipmentChangedMeta.builder().datetime(BASE.plusSeconds(1)).build()
        );

        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .statusHistory(List.of(
                            Status.builder()
                                .status(ReturnSegmentStatus.TRANSIT_PREPARED)
                                .timestamp(BASE.plusSeconds(2))
                                .build()
                        ))
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Была пересортировка: SHIPMENT_CHANGED - TRANSIT_PREPARED_OLD - TRANSIT_PREPARED_NEW")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/second_segment.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup("/database/tasks/return-segment/process-statuses/before/transit_prepared.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared_existing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changedShipmentEvent_transitPreparedOld_transitPreparedNew() {
        entityMetaService.save(
            new DetachedTypedEntity(EntityType.RETURN_SEGMENT, 1L),
            ReturnSegmentShipmentChangedMeta.builder().datetime(BASE.minusSeconds(1)).build()
        );

        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .statusHistory(List.of(
                            Status.builder()
                                .status(ReturnSegmentStatus.TRANSIT_PREPARED)
                                .timestamp(BASE.plusSeconds(2))
                                .build()
                        ))
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Была пересортировка: TRANSIT_PREPARED_OLD - TRANSIT_PREPARED_NEW - SHIPMENT_CHANGED")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/second_segment.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup("/database/tasks/return-segment/process-statuses/before/transit_prepared.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared_existing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void transitPreparedOld_transitPreparedNew_changedShipmentEvent() {
        entityMetaService.save(
            new DetachedTypedEntity(EntityType.RETURN_SEGMENT, 1L),
            ReturnSegmentShipmentChangedMeta.builder().datetime(BASE.plusSeconds(3)).build()
        );

        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .statusHistory(List.of(
                            Status.builder()
                                .status(ReturnSegmentStatus.TRANSIT_PREPARED)
                                .timestamp(BASE.plusSeconds(2))
                                .build()
                        ))
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Существующее событие сортировки на средней миле")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/second_segment.xml",
        type = DatabaseOperation.REFRESH
    )
    @DatabaseSetup("/database/tasks/return-segment/process-statuses/before/transit_prepared.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared_existing.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void existingTransitPrepared() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(
                            Status.builder()
                                .status(ReturnSegmentStatus.TRANSIT_PREPARED)
                                .timestamp(BASE.plusSeconds(2))
                                .build()
                        )
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Отмена лишних СЦ сегментов при приёмке невыкупа на первом СЦ")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/cancellation_setup.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/cancellation_in.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelledIn() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-2")
                        .status(Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE.minusSeconds(1)).build())
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Отмена лишних СЦ сегментов при приёмке невыкупа на первом СЦ без следующего сегмента")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/cancellation_setup.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/cancellation_in_no_next.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelledInNoNext() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-3")
                        .status(Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE.minusSeconds(1)).build())
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Отмена лишних СЦ сегментов при приёмке невыкупа на первом СЦ, когда от СЦ пришло 2 IN статуса")
    @DatabaseSetup(
        value = "/database/tasks/return-segment/process-statuses/before/cancellation_setup.xml",
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/cancellation_in.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelledDoubleIn() {
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-2")
                        .status(Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE.minusSeconds(1)).build())
                        .build()
                )
                .segment(
                    Segment.builder()
                        .uniqueId("segment-2")
                        .status(Status.builder().status(ReturnSegmentStatus.IN).timestamp(BASE.minusSeconds(2)).build())
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Не запрашивать маршрут, а создавать грузоместо, если следующий сегмент на контрольной точке")
    @DatabaseSetup(
        value = {
            "/database/tasks/return-segment/process-statuses/before/second_segment.xml",
            "/database/tasks/return-segment/process-statuses/before/control_point_on_second_sc.xml"
        },
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared_before_control_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void noCallGetRouteIfNextSegmentOnControlPoint() {
        featureProperties.setEnableControlPointUseInGetRoute(true);
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(Status.builder().status(ReturnSegmentStatus.TRANSIT_PREPARED).timestamp(BASE).build())
                        .build()
                )
                .build()
        );
    }

    @Test
    @DisplayName("Форсированно создавать грузоместо, если следующий сегмент на контрольной точке")
    @DatabaseSetup(
        value = {
            "/database/tasks/return-segment/process-statuses/before/second_segment.xml",
            "/database/tasks/return-segment/process-statuses/before/control_point_on_second_sc.xml"
        },
        type = DatabaseOperation.REFRESH
    )
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/process-statuses/after/transit_prepared_before_control_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void forceCreateStorageUnitsIfNextSegmentOnControlPoint() {
        featureProperties.setEnableControlPointUseInGetRoute(false);
        processor.execute(
            ReturnSegmentStatusesPayload.builder()
                .segment(
                    Segment.builder()
                        .uniqueId("segment-1")
                        .status(Status.builder().status(ReturnSegmentStatus.TRANSIT_PREPARED).timestamp(BASE).build())
                        .build()
                )
                .build()
        );
    }
}
