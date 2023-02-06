package ru.yandex.market.wms.common.spring.dao;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.ReceiptStatus;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetailStatusHistory;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailStatusHistoryDao;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class ReceiptDetailStatusHistoryDaoTest extends ReceivingIntegrationTest {

    @Autowired
    private ReceiptDetailStatusHistoryDao receiptDetailStatusHistoryDao;

    @Test
    @DatabaseSetup("/db/empty-db.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail-status-history/after-insert.xml", assertionMode = NON_STRICT)
    public void insert() {
        List<ReceiptDetailStatusHistory> statusHistories = Arrays.asList(
            createStatusHistoryEntity(ReceiptStatus.NEW, Instant.parse("2020-04-30T12:03:22Z")),
            createStatusHistoryEntity(ReceiptStatus.RECEIVED_COMPLETE, Instant.parse("2020-04-30T12:04:22Z"))
        );
        receiptDetailStatusHistoryDao.insert(statusHistories);
    }

    private ReceiptDetailStatusHistory createStatusHistoryEntity(ReceiptStatus status,
                                                                 Instant addDate) {
        return ReceiptDetailStatusHistory.builder()
            .receiptKey("0000012345")
            .receiptLineNumber("00001")
            .receiptStatus(status)
            .source("NewReceiving")
            .quantity(1)
            .addDate(addDate)
            .addWho("TEST")
            .build();
    }
}
