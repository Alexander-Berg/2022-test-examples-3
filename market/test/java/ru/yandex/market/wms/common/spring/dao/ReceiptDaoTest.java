package ru.yandex.market.wms.common.spring.dao;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.ReceiptStatus;
import ru.yandex.market.wms.common.model.enums.ReceiptType;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Receipt;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReceiptDaoTest extends IntegrationTest {

    private static final String EDIT_DATE_BEFORE = "2020-03-29 11:47:00";
    private static final String EXTERNAL_RECEIPT_KEY_3 = "1234";
    private static final String RECEIPT_KEY = "0000012345";
    private static final ReceiptType RECEIPT_TYPE = ReceiptType.DEFAULT;
    private static final String PALLET_KEY = "000001410";
    private static final String NONEXISTENT_RECEIPT_KEY = "0000000000";
    private static final String NONEXISTENT_PALLET_ID = "0000000000";

    @Autowired
    private ReceiptDao receiptDao;

    @Test
    @DatabaseSetup("/db/dao/receipt/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt/before.xml", assertionMode = NON_STRICT)
    public void getEditDateWhenReceiptPresent() {
        String actualEditDate = receiptDao.getEditDateAsString(RECEIPT_KEY);
        assertions.assertThat(actualEditDate).isEqualTo(EDIT_DATE_BEFORE);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt/before.xml", assertionMode = NON_STRICT)
    public void getEditDateWhenReceiptNotPresent() {
        assertThrows(IllegalArgumentException.class, () -> receiptDao.getEditDateAsString("0000012346"));
    }

    @Test
    @DatabaseSetup("/db/dao/receipt/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt/after-update-edit-date.xml", assertionMode = NON_STRICT)
    public void successfullyUpdatingEditDate() {
        LocalDateTime newEditDate = LocalDateTime.parse("2020-04-30T12:03:22");
        int updated = receiptDao.tryUpdateEditDate(newEditDate, "TEST", RECEIPT_KEY, EDIT_DATE_BEFORE);
        assertions.assertThat(updated).isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt/before.xml", assertionMode = NON_STRICT)
    public void unsuccessfulTryToUpdateEditDate() {
        LocalDateTime newEditDate = LocalDateTime.parse("2020-04-30T12:03:22");
        String oldEditDate = "2020-04-29 12:03:22";
        int updated = receiptDao.tryUpdateEditDate(newEditDate, "TEST", RECEIPT_KEY, oldEditDate);
        assertions.assertThat(updated).isEqualTo(0);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt/after-update-status-without-edit-date-and-edit-who.xml",
            assertionMode = NON_STRICT)
    public void updateStatusWithoutEditDateAndEditWho() {
        receiptDao.updateStatusWithoutEditDateAndEditWho(RECEIPT_KEY, ReceiptStatus.CLOSED);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt/before.xml")
    public void findReceiptByExternalReceiptKey3() {
        assertTrue(receiptDao.findReceiptByExternalReceiptKey3(EXTERNAL_RECEIPT_KEY_3).isPresent());
    }

    @Test
    @DatabaseSetup("/db/dao/receipt/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt/after-update-status.xml", assertionMode = NON_STRICT)
    public void updateStatus() {
        receiptDao.updateStatus(RECEIPT_KEY, ReceiptStatus.CLOSED, "TEST",
                LocalDateTime.ofInstant(Instant.parse("2020-04-30T12:03:22Z"), ZoneOffset.UTC));
    }

    @Test
    @DatabaseSetup("/db/dao/receipt/before.xml")
    public void shouldReturnReceiptByKey() {
        Optional<Receipt> receipt = receiptDao.findReceiptByKey(RECEIPT_KEY);
        assertTrue(receipt.isPresent(), "Receipt found");
        assertions.assertThat(receipt.get().getReceiptKey()).as("Check key").isEqualTo(RECEIPT_KEY);
        assertions.assertThat(receipt.get().getType()).as("Check type").isEqualTo(RECEIPT_TYPE);
        assertions.assertThat(receipt.get().getCarrierKey()).as("Check carrier key").isEqualTo("CKEY");
        assertions.assertThat(receipt.get().getCarrierName()).as("Check carrier name").isEqualTo("CNAME");
    }

    @Test
    @DatabaseSetup("/db/dao/receipt/before.xml")
    public void shouldReturnEmptyForNonexistentReceiptByKey() {
        Optional<Receipt> receipt = receiptDao.findReceiptByKey(NONEXISTENT_RECEIPT_KEY);
        assertFalse(receipt.isPresent(), "Receipt found");
    }

    @Test
    @DatabaseSetup("/db/dao/receipt/before.xml")
    public void shouldReturnReceiptByPalletId() {
        List<Receipt> receipts = receiptDao.findReceiptsByContainerId(PALLET_KEY);
        assertFalse(receipts.isEmpty(), "Receipt found");
        assertions.assertThat(receipts.get(0).getReceiptKey()).as("Check key").isEqualTo(RECEIPT_KEY);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt/before.xml")
    public void shouldReturnEmptyForNonexistentReceiptByPalletId() {
        List<Receipt> receipts = receiptDao.findReceiptsByContainerId(NONEXISTENT_PALLET_ID);
        assertTrue(receipts.isEmpty(), "Receipt found");
    }

    @Test
    @ExpectedDatabase(value = "/db/dao/receipt/after-create.xml", assertionMode = NON_STRICT_UNORDERED)
    public void insertTest() {
        Receipt data = Receipt.builder()
                .notes("testNotes")
                .receiptKey("KEY1")
                .status(ReceiptStatus.NEW)
                .type(ReceiptType.DEFAULT)
                .build();
        Clock clock = Clock.fixed(Instant.parse("2020-12-23T12:34:56.789Z"), ZoneOffset.UTC);
        receiptDao.insert(data, clock.instant(), "TEST_USER");

    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-get-receipt.xml")
    public void findReceiptByContainerId() {
        List<Receipt> receipts = receiptDao.findReceiptsByContainerId("CART035");
        assertions.assertThat(receipts.size()).isEqualTo(1);
        assertions.assertThat(receipts.get(0).getReceiptKey()).isEqualTo("0000018569");
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-get-receipt-no-active.xml")
    public void findReceiptByContainerId_noActive() {
        List<Receipt> receipts = receiptDao.findReceiptsByContainerId("CART035");
        assertions.assertThat(receipts).isEmpty();
    }
}
