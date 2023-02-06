package ru.yandex.market.billing.sorting;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.sorting.model.LogisticOrder;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.sorting.model.SortingIntakeType;
import ru.yandex.market.core.util.DateTimes;

/**
 * Тесты для {@link LogisticOrdersDao}.
 */
class LogisticOrdersDaoTest extends FunctionalTest {

    private static final Instant DATE_2019_12_31_START =
            DateTimes.toInstantAtDefaultTz(LocalDate.of(2019, 12, 31).atStartOfDay());
    private static final Instant DATE_2019_12_31_END =
            DateTimes.toInstantAtDefaultTz(LocalDateTime.of(LocalDate.of(2019, 12, 31),
                    LocalTime.MAX.minus(Duration.ofNanos(999))));
    private static final Instant DATE_2019_12_30_START =
            DateTimes.toInstantAtDefaultTz(LocalDate.of(2019, 12, 30).atStartOfDay());

    @Autowired
    private LogisticOrdersImportService logisticOrdersImportService;

    @DbUnitDataSet(
            before = "LogisticOrdersDaoTest.insert.before.csv",
            after = "LogisticOrdersDaoTest.insert.after.csv"
    )
    @DisplayName("Проверяем, что импортируются только новые заказы.")
    @Test
    void test_shouldInsertOnlyNewLogisticOrders() {
        logisticOrdersImportService.persistNewOrders(List.of(
                LogisticOrder.builder()
                        .setSupplierId(1L)
                        .setOrderId(1L)
                        .setCheckoutOrderId(1L)
                        .setWaybillSegmentId(1L)
                        .setStatusUpdatedAt(DATE_2019_12_31_START)
                        .setIntakeType(SortingIntakeType.INTAKE)
                        .setTrackerCheckpointId(1L)
                        .build(),
                LogisticOrder.builder()
                        .setSupplierId(2L)
                        .setOrderId(2L)
                        .setCheckoutOrderId(2L)
                        .setWaybillSegmentId(2L)
                        .setStatusUpdatedAt(DATE_2019_12_31_END)
                        .setIntakeType(SortingIntakeType.SELF_DELIVERY)
                        .setTrackerCheckpointId(2L)
                        .build(),
                LogisticOrder.builder()
                        .setSupplierId(1L)
                        .setOrderId(5L)
                        .setCheckoutOrderId(5L)
                        .setWaybillSegmentId(5L)
                        .setStatusUpdatedAt(DateTimes.toInstantAtDefaultTz(LocalDateTime.of(2019, 12, 31, 0, 0, 0)))
                        .setIntakeType(SortingIntakeType.INTAKE)
                        .setTrackerCheckpointId(5L)
                        .build()
                )
        );
    }

    @Test
    @DisplayName("Проверяем что данные в таблице logistic_orders обновляются, но не все.")
    @DbUnitDataSet(
            before = "LogisticOrdersDaoTest.upsert.before.csv",
            after = "LogisticOrdersDaoTest.upsert.after.csv"
    )
    void test_shouldUpdateExistingAndInsertNew() {
        logisticOrdersImportService.persistNewOrders(
                List.of(
                        //2 заказа на обновление
                        // Не должна изменится дата, но должны изменится все остальные поля
                        LogisticOrder.builder()
                                .setSupplierId(1L)
                                .setOrderId(5L)
                                .setCheckoutOrderId(5L)
                                .setWaybillSegmentId(15L)
                                .setStatusUpdatedAt(DATE_2019_12_30_START)
                                .setIntakeType(SortingIntakeType.SELF_DELIVERY)
                                .setTrackerCheckpointId(15L)
                                .build(),
                        // Не должен изменится supplier, но должны измениться остальные поля
                        LogisticOrder.builder()
                                .setSupplierId(2L)
                                .setOrderId(3L)
                                .setCheckoutOrderId(3L)
                                .setWaybillSegmentId(13L)
                                .setStatusUpdatedAt(DATE_2019_12_30_START)
                                .setIntakeType(SortingIntakeType.SELF_DELIVERY)
                                .setTrackerCheckpointId(13L)
                                .build(),
                        // 2 новых заказа
                        LogisticOrder.builder()
                                .setSupplierId(1L)
                                .setOrderId(6L)
                                .setCheckoutOrderId(6L)
                                .setWaybillSegmentId(6L)
                                .setStatusUpdatedAt(DATE_2019_12_31_START)
                                .setIntakeType(SortingIntakeType.INTAKE)
                                .setTrackerCheckpointId(6L)
                                .build(),
                        LogisticOrder.builder()
                                .setSupplierId(2L)
                                .setOrderId(7L)
                                .setCheckoutOrderId(7L)
                                .setWaybillSegmentId(7L)
                                .setStatusUpdatedAt(DATE_2019_12_30_START)
                                .setIntakeType(SortingIntakeType.SELF_DELIVERY)
                                .setTrackerCheckpointId(7L)
                                .build()
                )
        );
    }
}
