package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.util.Collections;

import org.apache.ibatis.session.SqlSession;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.calendaring.client.dto.DailyQuotaInfoResponse;
import ru.yandex.market.logistics.calendaring.client.dto.IntervalQuotaInfoResponse;
import ru.yandex.market.logistics.calendaring.client.dto.QuotaInfoResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.QuotaType;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.exception.UserWarningException;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.service.environment.EnvironmentService;

public class DemandUpdateDateServiceTest extends FunctionalTest {

    @Autowired
    private SqlSession batchSqlSession;

    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DbUnitDataSet(before = "DemandUpdateServiceTest.UpdateDeliveryDate.before.csv",
        after = "DemandUpdateServiceTest.UpdateDeliveryDate.after.csv")
    public void updateDeliveryDate() {
        final CalendaringService calendaringService = Mockito.mock(CalendaringService.class);
        Mockito.when(calendaringService.getDailyQuota(Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.any()))
            .thenReturn(new IntervalQuotaInfoResponse(
                Collections.singletonList(new DailyQuotaInfoResponse(
                    QuotaType.SUPPLY,
                    LocalDate.of(2020, 9, 12),
                    new QuotaInfoResponse(200L, 1L, 2, 1),
                    new QuotaInfoResponse(200L, 1L, 2, 1),
                    null,
                    null
                ))
            ));

        final DemandUpdateDateService demandUpdateDateService = new DemandUpdateDateService(
            batchSqlSession,
            calendaringService,
            environmentService
        );

        demandUpdateDateService
            .updateDeliveryDate(DemandType.TYPE_1P, 1, LocalDate.of(2020, 9, 12));
    }

    @Test
    @DbUnitDataSet(before = "DemandUpdateServiceTest.UpdateDeliveryDate.before.csv")
    public void updateDeliveryDateFail() {
        final CalendaringService calendaringService = Mockito.mock(CalendaringService.class);
        Mockito.when(calendaringService.getDailyQuota(Mockito.any(), Mockito.anyLong(), Mockito.any(), Mockito.any()))
            .thenReturn(new IntervalQuotaInfoResponse(
                Collections.singletonList(new DailyQuotaInfoResponse(
                    QuotaType.SUPPLY,
                    LocalDate.of(2020, 9, 12),
                    new QuotaInfoResponse(10L, 1L, 8, 1),
                    new QuotaInfoResponse(10L, 1L, 2, 1),
                    null,
                    null
                ))
            ));

        final DemandUpdateDateService demandUpdateDateService = new DemandUpdateDateService(
            batchSqlSession,
            calendaringService,
            environmentService
        );

        Assertions.assertThrows(UserWarningException.class, () -> demandUpdateDateService
            .updateDeliveryDate(DemandType.TYPE_1P, 1, LocalDate.of(2020, 9, 12)));
    }
}
