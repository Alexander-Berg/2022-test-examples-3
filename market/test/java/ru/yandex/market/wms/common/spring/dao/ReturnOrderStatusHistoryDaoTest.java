package ru.yandex.market.wms.common.spring.dao;

import java.time.LocalDateTime;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.ReturnOrderStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.implementation.ReturnOrderStatusHistoryDao;
import ru.yandex.market.wms.common.spring.dto.ReturnOrderStatusDto;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class ReturnOrderStatusHistoryDaoTest extends IntegrationTest {

    @Autowired
    private ReturnOrderStatusHistoryDao returnOrderStatusHistoryDao;

    @Test
    @DatabaseSetup("/db/dao/return-order-status/before.xml")
    @ExpectedDatabase(value = "/db/dao/return-order-status/after.xml", assertionMode = NON_STRICT)
    public void insertNewReturnOrderStatus() {
        returnOrderStatusHistoryDao.insertNewReturnOrderStatus(
                "receiptKey",
                "order",
                ReturnOrderStatus.RECEIVING_COMPLETED);
    }

    @Test
    @DatabaseSetup("/db/dao/return-order-status/before-find.xml")
    @ExpectedDatabase(value = "/db/dao/return-order-status/before-find.xml", assertionMode = NON_STRICT)
    public void findReturnOrderStatusesByOrderKey() {
        List<ReturnOrderStatusDto> orderStatuses = returnOrderStatusHistoryDao.getReturnOrderStatuses("order1");

        assertions.assertThat(orderStatuses).containsExactlyInAnyOrder(
                ReturnOrderStatusDto.builder()
                        .receiptKey("receipt1")
                        .externOrderKey("order1")
                        .status(ReturnOrderStatus.RECEIVING_PARTIALLY_COMPLETED)
                        .date(LocalDateTime.parse("2000-01-01T00:00:00"))
                        .build(),
                ReturnOrderStatusDto.builder()
                        .receiptKey("receipt2")
                        .externOrderKey("order1")
                        .status(ReturnOrderStatus.RECEIVING_COMPLETED)
                        .date(LocalDateTime.parse("2000-01-02T00:00:00"))
                        .build()
        );
    }
}
