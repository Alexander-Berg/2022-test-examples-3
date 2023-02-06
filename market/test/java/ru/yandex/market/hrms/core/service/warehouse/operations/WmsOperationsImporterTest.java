package ru.yandex.market.hrms.core.service.warehouse.operations;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.warehouse.jdbc.WmsOperationsStat;
import ru.yandex.market.hrms.core.domain.warehouse.jdbc.WmsPerformanceHistoryClickhouseRepo;
import ru.yandex.market.hrms.core.service.util.HrmsDateTimeUtil;
import ru.yandex.market.hrms.core.service.yt.WmsOperationsImporter;

import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "WmsOperationsImporterTest.before.csv")
@DbUnitDataSet(after = "WmsOperationsImporterTest.after.csv")
public class WmsOperationsImporterTest extends AbstractCoreTest {

    @Autowired
    private Clock clock;

    @Autowired
    private WmsOperationsImporter wmsOperationsImporter;

    @MockBean
    private WmsPerformanceHistoryClickhouseRepo wmsPerformanceHistoryClickhouseRepo;

    @Test
    public void importWmsOperations() {
        LocalDate from = LocalDate.of(2021, 8, 1);
        LocalDate to = LocalDate.of(2021, 8, 4);
        ZoneId zoneIdFrom = ZoneId.of("Europe/Moscow");
        String login = "sof-bazooka";
        String warehouseCode = "SOF";

        when(wmsPerformanceHistoryClickhouseRepo.loadDataPerDate(LocalDate.of(2021, 8, 1), warehouseCode, zoneIdFrom))
                .thenReturn(List.of(createWmsOperation(warehouseCode, login,
                        "Тестирование", Instant.parse("2021-08-01T05:00:00.000Z"))));
        when(wmsPerformanceHistoryClickhouseRepo.loadDataPerDate(LocalDate.of(2021, 8, 2), warehouseCode, zoneIdFrom))
                .thenReturn(List.of(createWmsOperation(warehouseCode, login,
                        "Мытьё полов", Instant.parse("2021-08-02T02:00:00.000Z"))));
        when(wmsPerformanceHistoryClickhouseRepo.loadDataPerDate(LocalDate.of(2021, 8, 3), warehouseCode, zoneIdFrom))
                .thenReturn(List.of(
                        createWmsOperation(warehouseCode, login,
                        "Ничегонеделанье", Instant.parse("2021-08-03T03:00:00.000Z")),
                        createWmsOperation(warehouseCode, login,
                        "Распаковка", Instant.parse("2021-08-03T15:00:00.000Z"))));
        when(wmsPerformanceHistoryClickhouseRepo.loadDataPerDate(LocalDate.of(2021, 8, 4), warehouseCode, zoneIdFrom))
                .thenReturn(List.of(createWmsOperation(warehouseCode, login,
                        "Запаковка", Instant.parse("2021-08-04T21:00:00.000Z"))));

        wmsOperationsImporter.importData(from, to);
    }

    private WmsOperationsStat createWmsOperation(String warehouseCode, String wmsLogin,
                                                 String operationType, Instant operationDateTime) {
        return new WmsOperationsStat(
                warehouseCode,
                wmsLogin,
                operationType,
                null,
                operationDateTime,
                operationDateTime,
                null,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                null,
                0,
                0,
                "spider-man",
                HrmsDateTimeUtil.toLocalDate(operationDateTime),
                clock.instant()
        );
    }
}
