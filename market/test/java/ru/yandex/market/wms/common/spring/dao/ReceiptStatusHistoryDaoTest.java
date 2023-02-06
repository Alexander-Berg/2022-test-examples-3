package ru.yandex.market.wms.common.spring.dao;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.ReceiptStatus;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptStatusHistory;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptStatusHistoryDao;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.IN_RECEIVING;
import static ru.yandex.market.wms.common.model.enums.ReceiptStatus.PALLET_ACCEPTANCE;
import static ru.yandex.market.wms.common.model.enums.ReceiptType.DEFAULT;

public class ReceiptStatusHistoryDaoTest extends ReceivingIntegrationTest {

    @Autowired
    private ReceiptStatusHistoryDao receiptStatusHistoryDao;

    @Test
    @DatabaseSetup("/db/empty-db.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-status-history/after-insert.xml", assertionMode = NON_STRICT)
    public void insert() {
        ReceiptStatusHistory entiry = ReceiptStatusHistory.builder()
                .receiptKey("0000012345")
                .receiptStatus(ReceiptStatus.CLOSED)
                .source("NewReceiving")
                .addDate(Instant.parse("2020-04-30T12:03:22Z"))
                .addWho("TEST")
                .build();
        receiptStatusHistoryDao.insert(entiry, LocalDateTime.ofInstant(Instant.parse("2020-04-30T12:03:22Z"),
                ZoneOffset.UTC));
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-status-history/select_earliest_records_on_current_status.xml")
    public void getRecordsByLatestAppearanceOnStatus() {
        String source = "trReceipt.PostUpdateFire";
        String addWho = "AD2";
        ReceiptStatusHistory rsh1 = ReceiptStatusHistory.builder()
                .receiptStatus(PALLET_ACCEPTANCE)
                .receiptKey("0000000101")
                .addDate(Instant.parse("2022-04-18T18:20:33.000Z"))
                .serialKey("2")
                .addWho(addWho)
                .source(source)
                .build();
        ReceiptStatusHistory rsh2 = ReceiptStatusHistory.builder()
                .receiptStatus(PALLET_ACCEPTANCE)
                .receiptKey("0000001612")
                .addDate(Instant.parse("2022-04-18T14:00:00.000Z"))
                .serialKey("5")
                .addWho(addWho)
                .source(source)
                .build();
        List<ReceiptStatusHistory> records =
                receiptStatusHistoryDao.findRecordsByEarliestAppearanceOnStatus(List.of(PALLET_ACCEPTANCE,
                        IN_RECEIVING), PALLET_ACCEPTANCE, List.of(DEFAULT), 36500);
        Assertions.assertEquals(2, records.size());
        Assertions.assertTrue(records.containsAll(List.of(rsh1, rsh2)));
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-status-history/before-delete.xml")
    @ExpectedDatabase(value = "/db/empty-db.xml", assertionMode = NON_STRICT)
    public void delete() {
        receiptStatusHistoryDao.delete("0000012345");
    }
}
