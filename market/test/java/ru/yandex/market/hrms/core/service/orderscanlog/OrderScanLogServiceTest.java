package ru.yandex.market.hrms.core.service.orderscanlog;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.hrms.core.AbstractCoreTest;
import ru.yandex.market.hrms.core.domain.orderscan.OrderScanLogDto;
import ru.yandex.market.hrms.core.domain.orderscan.OrderScanLogService;
import ru.yandex.market.hrms.core.domain.orderscan.OrderScanLogYtRepo;

public class OrderScanLogServiceTest extends AbstractCoreTest {

    @MockBean
    OrderScanLogYtRepo orderScanLogYtRepo;

    @Autowired
    OrderScanLogService orderScanLogService;

    @Test
    @DbUnitDataSet(after = "OrderScanLogService.ImportOrderScanLog.after.csv")
    void importOrderScanLogTest() {

        Mockito.when(orderScanLogYtRepo.importData(
                LocalDateTime.of(2021, 9, 1, 0, 0, 0),
                LocalDateTime.of(2021, 9, 1, 1, 0, 0)
        )).thenReturn(List.of(
                new OrderScanLogDto(1L,
                        1L,
                        "2021-09-01T00:11:38.872142+03:00",
                        1L,
                        "ok"),
                new OrderScanLogDto(2L,
                        2L,
                        "2021-09-01T00:11:38.872142+03:00",
                        2L,
                        "ok")
        ));
        orderScanLogService.importOrderScanLogsForHour(
                LocalDateTime.of(2021, 9, 1, 0, 0, 0));
    }
}
