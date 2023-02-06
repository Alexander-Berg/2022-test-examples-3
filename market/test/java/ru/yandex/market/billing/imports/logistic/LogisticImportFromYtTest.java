package ru.yandex.market.billing.imports.logistic;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.imports.logistic.dao.LogisticOrdersDao;
import ru.yandex.market.billing.imports.logistic.dao.LogisticOrdersYtDao;
import ru.yandex.market.billing.imports.logistic.model.LogisticOrder;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.sorting.model.SortingIntakeType;
import ru.yandex.market.core.util.DateTimes;

@ExtendWith(MockitoExtension.class)
public class LogisticImportFromYtTest extends FunctionalTest {

    private static final LocalDate DATE_2019_12_31 =
            LocalDate.of(2019, 12, 31);
    private static final Instant DATE_2019_12_31_START =
            DateTimes.toInstantAtDefaultTz(LocalDate.of(2019, 12, 31).atStartOfDay());
    private static final Instant DATE_2019_12_30_START =
            DateTimes.toInstantAtDefaultTz(LocalDate.of(2019, 12, 30).atStartOfDay());
    @Autowired
    private LogisticOrdersDao logisticOrdersDao;
    @Mock
    private LogisticOrdersYtDao logisticOrdersYtDao;
    private LogisticOrdersImportService logisticOrdersImportService;

    @BeforeEach
    public void setup() {
        Mockito.when(logisticOrdersYtDao.getLogisticOrders(DATE_2019_12_31)).thenReturn(List.of(
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
        ));
        logisticOrdersImportService =
                new LogisticOrdersImportService(logisticOrdersYtDao, logisticOrdersDao);
    }

    @Test
    @DisplayName("Тестирование импорта из YT")
    @DbUnitDataSet(
            before = "LogisticOrdersDaoTest.yt.before.csv",
            after = "LogisticOrdersDaoTest.yt.after.csv"
    )
    public void importFromYtTest() {
        logisticOrdersImportService.importOrders(DATE_2019_12_31);
    }
}
