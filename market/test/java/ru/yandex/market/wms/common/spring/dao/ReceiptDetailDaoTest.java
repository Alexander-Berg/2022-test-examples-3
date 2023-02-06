package ru.yandex.market.wms.common.spring.dao;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.ReceiptStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.ExpectedAndReceivedSku;
import ru.yandex.market.wms.common.spring.dao.entity.Lot;
import ru.yandex.market.wms.common.spring.dao.entity.LotWithUnnamedAttributes;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetail;
import ru.yandex.market.wms.common.spring.dao.entity.Sku;
import ru.yandex.market.wms.common.spring.dao.entity.SkuId;
import ru.yandex.market.wms.common.spring.dao.entity.UnnamedLotAttributes;
import ru.yandex.market.wms.common.spring.dao.implementation.ReceiptDetailDao;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailCopyParams;
import ru.yandex.market.wms.common.spring.pojo.ReceiptDetailKey;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class ReceiptDetailDaoTest extends IntegrationTest {

    private static final String FIRST_RECEIPT_KEY = "0000018569";

    private static final UnnamedLotAttributes FULLY_FILLED_LOT_ATTRIBUTES = UnnamedLotAttributes.builder()
            .lottable01("copy_lottable01")
            .lottable02("copy_lottable02")
            .lottable03("copy_lottable03")
            .lottable04("2020-05-17 12:00:00")
            .lottable05("2020-06-22 15:00:00")
            .lottable06("copy_lottable06")
            .lottable07("copy_lottable07")
            .lottable08("copy_lottable08")
            .lottable09("copy_lottable09")
            .lottable10("copy_lottable10")
            .lottable11("2020-01-17 12:00:00")
            .lottable12("2020-11-17 12:00:00")
            .build();

    private static final ReceiptDetail EXPECTED_RECEIPT_DETAIL_IN_GET = ReceiptDetail.builder()
            .serialKey("1")
            .receiptKey("0000018569")
            .receiptDetailKey(new ReceiptDetailKey("0000018569", "01152"))
            .skuId(new SkuId("465852", "ROV0000000000000000359"))
            .quantityExpected(new BigDecimal("10.00000"))
            .quantityReceived(new BigDecimal("0.00000"))
            .unitPrice(new BigDecimal("100.0"))
            .effectiveDate(Instant.parse("2020-05-13T14:05:05.000Z"))
            .status(ReceiptStatus.RECEIVED_COMPLETE)
            .toId("CART035")
            .toLoc("STAGE36")
            .supplierKey("000083")
            .externOrderKey("101")
            .subreceipt("101")
            .externReceiptKey("281881")
            .name("")
            .sourceContainerId(null)
            .build();

    @Autowired
    private ReceiptDetailDao receiptDetailDao;
    @Autowired
    private Clock clock;

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getNextReceiptLineNumber() {
        String nextReceiptLineNumber = receiptDetailDao.getNextReceiptLineNumber("0000018569");
        assertions.assertThat(nextReceiptLineNumber).isEqualTo("01154");
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void findBySerialKeyWhenItExists() {
        Optional<ReceiptDetail> maybeReceiptDetailKey = receiptDetailDao.findBySerialKey("1");
        assertions.assertThat(maybeReceiptDetailKey).isPresent();
        ReceiptDetail receiptDetail = maybeReceiptDetailKey.get();
        assertions.assertThat(receiptDetail).isEqualTo(EXPECTED_RECEIPT_DETAIL_IN_GET);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void findBySerialKeyWhenItNotExists() {
        Optional<ReceiptDetail> maybeReceiptDetailKey = receiptDetailDao.findBySerialKey("4");
        assertions.assertThat(maybeReceiptDetailKey).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void findByKeyWhenItExists() {
        Optional<ReceiptDetail> maybeReceiptDetailKey = receiptDetailDao.findByKey(new ReceiptDetailKey("0000018569",
                "01152"));
        assertions.assertThat(maybeReceiptDetailKey).isPresent();
        ReceiptDetail receiptDetail = maybeReceiptDetailKey.get();
        assertions.assertThat(receiptDetail).isEqualTo(EXPECTED_RECEIPT_DETAIL_IN_GET);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void findByKeyWhenItNotExists() {
        Optional<ReceiptDetail> maybeReceiptDetailKey = receiptDetailDao.
                findByKey(new ReceiptDetailKey("0000018569", "00001"));
        assertions.assertThat(maybeReceiptDetailKey).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getSkuIdWhenItExists() {
        Optional<SkuId> maybeSkuId = receiptDetailDao.getSkuId("1");
        assertions.assertThat(maybeSkuId).isPresent();
        SkuId skuId = maybeSkuId.get();
        assertions.assertThat(skuId.getStorerKey()).isEqualTo("465852");
        assertions.assertThat(skuId.getSku()).isEqualTo("ROV0000000000000000359");
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getSkuIdWhenItNotExists() {
        Optional<SkuId> maybeSkuId = receiptDetailDao.getSkuId("4");
        assertions.assertThat(maybeSkuId).isNotPresent();
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/after-copy-with-all-params.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void copyReceiptDetailWithAllCopyParams() {
        ReceiptDetailCopyParams copyParams = ReceiptDetailCopyParams.builder()
                .status(ReceiptStatus.VERIFIED_CLOSED)
                .dateReceived(Instant.parse("2020-04-17T12:00:00.000Z"))
                .quantityExpected(20.0)
                .quantityReceived(10.0)
                .toLoc("STAGE38")
                .toId("CART038")
                .addWho("TEST3")
                .editWho("TEST4")
                .newReceiptDetailKey(new ReceiptDetailKey("0000018569", "01154"))
                .originalReceiptDetail(ReceiptDetail.builder()
                        .serialKey("2")
                        .receiptDetailKey(new ReceiptDetailKey("0000018569", "01153"))
                        .build())
                .addDate(LocalDateTime.now(clock))
                .build();
        receiptDetailDao.copyReceiptDetail("WMS00020", copyParams);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/after-copy-without-params.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void copyReceiptDetailWithEmptyCopyParams() {
        ReceiptDetailCopyParams copyParams = ReceiptDetailCopyParams.builder()
                .newReceiptDetailKey(new ReceiptDetailKey("0000018569", "01154"))
                .originalReceiptDetail(ReceiptDetail.builder()
                        .serialKey("2")
                        .receiptDetailKey(new ReceiptDetailKey("0000018569", "01153"))
                        .build())
                .addDate(LocalDateTime.now(clock))
                .build();
        receiptDetailDao.copyReceiptDetail("WMS00020", copyParams);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/after-copy-without-received-quantity.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void copyReceiptDetailWithoutReceivedQuantity() {
        jdbc.execute("ALTER SEQUENCE RECEIPTDETAIL_SERIAL_KEY_SEQ RESTART WITH 4;");
        ReceiptDetailCopyParams copyParams = ReceiptDetailCopyParams.builder()
                .status(ReceiptStatus.VERIFIED_CLOSED)
                .dateReceived(Instant.parse("2020-04-17T12:00:00.000Z"))
                .quantityExpected(30.0)
                .toLoc("STAGE38")
                .toId("CART038")
                .addWho("TEST3")
                .editWho("TEST4")
                .newReceiptDetailKey(new ReceiptDetailKey("0000018569", "01154"))
                .originalReceiptDetail(ReceiptDetail.builder()
                        .serialKey("2")
                        .receiptDetailKey(new ReceiptDetailKey("0000018569", "01153"))
                        .build())
                .addDate(LocalDateTime.now(clock))
                .build();
        receiptDetailDao.copyReceiptDetail("WMS00020", copyParams);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-get-expected-and-received.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before-get-expected-and-received.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getExpectedAndReceivedInReceiptWhenExists() {
        List<ExpectedAndReceivedSku> expectedAndReceivedSkus = receiptDetailDao.
                getExpectedAndReceivedInReceipt(FIRST_RECEIPT_KEY);
        Map<SkuId, ExpectedAndReceivedAmount> idsToExpectedAmounts = ImmutableMap.of(
                new SkuId("465852", "ROV0000000000000000359"),
                new ExpectedAndReceivedAmount(BigDecimal.valueOf(10), BigDecimal.valueOf(8)),
                new SkuId("465852", "ROV0000000000000000358"),
                new ExpectedAndReceivedAmount(BigDecimal.valueOf(32), BigDecimal.valueOf(8))
        );
        assertExpectedAndReceivedSkusCorrect(idsToExpectedAmounts, expectedAndReceivedSkus);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-get-expected-and-received.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before-get-expected-and-received.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getExpectedAndReceivedInReceiptWhenNotExists() {
        List<ExpectedAndReceivedSku> expectedAndReceivedSkus = receiptDetailDao.
                getExpectedAndReceivedInReceipt("0000018566");
        assertions.assertThat(expectedAndReceivedSkus).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-get-expected-and-received.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before-get-expected-and-received.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getExpectedAndReceivedForOneReceiptDetailWhenExists() {
        Optional<ExpectedAndReceivedSku> maybeExpectedAndReceived =
                receiptDetailDao.getExpectedAndReceivedForOneReceiptDetail("1");
        assertions.assertThat(maybeExpectedAndReceived).isPresent();
        ExpectedAndReceivedSku expectedAndReceivedSku = maybeExpectedAndReceived.get();
        SkuId skuId = expectedAndReceivedSku.getKey().getSkuId();
        assertions.assertThat(skuId).isEqualTo(new SkuId("465852", "ROV0000000000000000359"));
        assertions.assertThat(expectedAndReceivedSku.getQuantityExpected()).
                isEqualByComparingTo(BigDecimal.valueOf(10));
        assertions.assertThat(expectedAndReceivedSku.getQuantityReceived()).
                isEqualByComparingTo(BigDecimal.valueOf(4));
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-get-expected-and-received.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before-get-expected-and-received.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getExpectedAndReceivedForOneReceiptDetailWhenNotExists() {
        Optional<ExpectedAndReceivedSku> maybeExpectedAndReceived =
                receiptDetailDao.getExpectedAndReceivedForOneReceiptDetail("11");
        assertions.assertThat(maybeExpectedAndReceived).isNotPresent();
    }


    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getReceiptDetails() {
        List<ReceiptDetail> receiptDetails = receiptDetailDao.getExpectedReceiptDetails(FIRST_RECEIPT_KEY);
        assertions.assertThat(receiptDetails).isNotEmpty();
        assertions.assertThat(receiptDetails.size()).isEqualTo(2);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-get-by-boxId.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before-get-by-boxId.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getBoxReceiptDetailsWithBoxIdExists() {
        Optional<ReceiptDetail> receiptDetail = receiptDetailDao
                .getBoxReceiptDetailsWithBoxId(FIRST_RECEIPT_KEY, EXPECTED_RECEIPT_DETAIL_IN_GET.getToId());

        assertions.assertThat(receiptDetail.isPresent()).isTrue();
        assertions.assertThat(receiptDetail.get().getToId()).isEqualTo(EXPECTED_RECEIPT_DETAIL_IN_GET.getToId());
        assertions.assertThat(receiptDetail.get().getReceiptDetailKey().getReceiptKey()).isEqualTo(FIRST_RECEIPT_KEY);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getReceiptDetail() {
        ReceiptDetailKey key = new ReceiptDetailKey(FIRST_RECEIPT_KEY, "01153");
        ReceiptDetail receiptDetail = receiptDetailDao.findByKey(key).orElse(null);
        assertions.assertThat(receiptDetail).isNotNull();
        assertions.assertThat(receiptDetail.getReceiptDetailKey())
                .isEqualTo(new ReceiptDetailKey(FIRST_RECEIPT_KEY, "01153"));
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-no-uit.xml")
    public void getExpectedReceiptDetailsWithUitNoUit() {
        Optional<ReceiptDetail> detailsWithUit = receiptDetailDao.getExpectedReceiptDetailWithUit("3", "1");
        assertions.assertThat(detailsWithUit.isPresent()).isFalse();
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-uit-expected-is-zero.xml")
    public void getExpectedReceiptDetailsWithUitExpectedIsZero() {
        assertions.assertThat(receiptDetailDao.getExpectedReceiptDetailWithUit("3", "1").isPresent()).isFalse();
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-uit-exists.xml")
    public void getExpectedReceiptDetailsWithUitExists() {
        Optional<ReceiptDetail> detailsWithUit = receiptDetailDao.getExpectedReceiptDetailWithUit("3", "1");
        assertions.assertThat(detailsWithUit.isPresent()).isTrue();
        assertions.assertThat(detailsWithUit.get().getReceiptDetailKey().getReceiptKey()).isEqualTo("1");
        assertions.assertThat(detailsWithUit.get().getReceiptDetailKey().getReceiptLineNumber()).isEqualTo("2");
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    public void findDetailsByStorerAndSkuNotFound() {
        List<ReceiptDetail> detail = receiptDetailDao.findDetailsByStorerAndSku("123", "123", "123");
        assertions.assertThat(detail).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-get-expected-and-received.xml")
    public void findDetailsByStorerAndSku() {
        List<ReceiptDetail> details =
                receiptDetailDao.findDetailsByStorerAndSku("0000018569", "465852", "ROV0000000000000000359");
        assertions.assertThat(details.size()).isEqualTo(3);
        assertions.assertThat(details).allMatch(receiptDetail -> {
            assertions.assertThat(receiptDetail.getReceiptDetailKey().getReceiptKey()).isEqualTo("0000018569");
            assertions.assertThat(receiptDetail.getSkuId().getStorerKey()).isEqualTo("465852");
            assertions.assertThat(receiptDetail.getSkuId().getSku()).isEqualTo("ROV0000000000000000359");
            assertions.assertThat(receiptDetail.getQuantityReceived()).isGreaterThan(BigDecimal.ZERO);
            return true;
        });
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/after-update-lot-attributes.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void updateLotAttributesLotsAreUpdated() {
        LotWithUnnamedAttributes attributes = LotWithUnnamedAttributes.builder()
                .lot(Lot.builder().lot("LOT").build())
                .lotAttributes(FULLY_FILLED_LOT_ATTRIBUTES)
                .build();
        receiptDetailDao.updateDetailsWithAttributes(new ReceiptDetailKey("0000018569", "01152"),
                attributes, 0.0, "user",
                LocalDateTime.now(clock));
    }


    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/after-update-order-key.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void updateExternOrderKeyAreUpdate() {
        receiptDetailDao.updateExternOrderKey(new ReceiptDetailKey("0000018569", "01152"),
                "999",
                "USERTEST2",
                LocalDateTime.now(clock));
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-with-attributes.xml")
    void getAttributesBySerial() {
        LotWithUnnamedAttributes lots = receiptDetailDao.getAttributesBySerial("1").get();
        assertions.assertThat(lots.getLot().getLot()).isEqualTo("TOLOT");
        UnnamedLotAttributes lotAttributes = lots.getLotAttributes();
        assertions.assertThat(lotAttributes.getLottable01()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable01());
        assertions.assertThat(lotAttributes.getLottable02()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable02());
        assertions.assertThat(lotAttributes.getLottable03()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable03());
        assertions.assertThat(lotAttributes.getLottable04()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable04());
        assertions.assertThat(lotAttributes.getLottable05()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable05());
        assertions.assertThat(lotAttributes.getLottable06()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable06());
        assertions.assertThat(lotAttributes.getLottable07()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable07());
        assertions.assertThat(lotAttributes.getLottable08()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable08());
        assertions.assertThat(lotAttributes.getLottable09()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable09());
        assertions.assertThat(lotAttributes.getLottable10()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable10());
        assertions.assertThat(lotAttributes.getLottable11()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable11());
        assertions.assertThat(lotAttributes.getLottable12()).isEqualTo(FULLY_FILLED_LOT_ATTRIBUTES.getLottable12());
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-find-container.xml")
    void findActiveContainer() {
        Optional<ReceiptDetail> cart035 = receiptDetailDao.findActiveContainer("CART035");
        assertions.assertThat(cart035).isPresent();
        assertions.assertThat(cart035.get().getToId()).isEqualTo("CART035");
        assertions.assertThat(cart035.get().getStatus()).isEqualTo(ReceiptStatus.PALLET_ACCEPTANCE);
    }

    @Test
    @ExpectedDatabase(value = "/db/dao/receipt-detail/after-create-receipt-detail.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void createReceiptDetail() {
        ReceiptDetailCopyParams copyParams = ReceiptDetailCopyParams.builder()
                .status(ReceiptStatus.IN_RECEIVING)
                .dateReceived(Instant.now(clock))
                .quantityExpected(1.0)
                .quantityReceived(1.0)
                .toLoc("TOLOC")
                .toId("TOID")
                .addWho("USER")
                .editWho("USER")
                .newReceiptDetailKey(new ReceiptDetailKey("12345", "00001"))
                .originalReceiptDetail(null)
                .sku(Sku.builder()
                        .sku("SKU")
                        .storerKey("456")
                        .build())
                .build();
        receiptDetailDao.createReceiptDetail("123", copyParams);
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-get-expected-and-received-returns.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before-get-expected-and-received-returns.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getExpectedReceiptDetailWithSkuAndOrder() {
        Optional<ReceiptDetail> expected =
                receiptDetailDao.getExpectedReceiptDetailWithSkuAndOrder("0000018569",
                        SkuId.of("465852", "ROV0000000000000000359"), "123456");
        assertions.assertThat(expected).isPresent();
        ReceiptDetail receiptDetail = expected.get();
        SkuId skuId = receiptDetail.getSkuId();
        assertions.assertThat(skuId).isEqualTo(new SkuId("465852", "ROV0000000000000000359"));
        assertions.assertThat(receiptDetail.getExternOrderKey()).isEqualTo("123456");
        assertions.assertThat(receiptDetail.getQuantityExpected()).
                isEqualByComparingTo(BigDecimal.valueOf(10));
        assertions.assertThat(receiptDetail.getReceiptDetailKey()).satisfies(receiptDetailKey -> {
            assertions.assertThat(receiptDetailKey.getReceiptKey()).isEqualTo("0000018569");
            assertions.assertThat(receiptDetailKey.getReceiptLineNumber()).isEqualTo("01152");
        });
    }

    @Test
    @DatabaseSetup("/db/dao/receipt-detail/before-get-expected-and-received-returns.xml")
    @ExpectedDatabase(value = "/db/dao/receipt-detail/before-get-expected-and-received-returns.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getExpectedReceiptDetailWithSkusAndOrder() {
        List<ReceiptDetail> expected =
                receiptDetailDao.getExpectedReceiptDetailWithSkusAndOrder("0000018569",
                        Set.of(SkuId.of("465852", "ROV0000000000000000359")), "123456");
        assertions.assertThat(expected).hasOnlyOneElementSatisfying(receiptDetail -> {
            SkuId skuId = receiptDetail.getSkuId();
            assertions.assertThat(skuId).isEqualTo(new SkuId("465852", "ROV0000000000000000359"));
            assertions.assertThat(receiptDetail.getExternOrderKey()).isEqualTo("123456");
            assertions.assertThat(receiptDetail.getQuantityExpected()).
                    isEqualByComparingTo(BigDecimal.valueOf(10));
            assertions.assertThat(receiptDetail.getReceiptDetailKey()).satisfies(receiptDetailKey -> {
                assertions.assertThat(receiptDetailKey.getReceiptKey()).isEqualTo("0000018569");
                assertions.assertThat(receiptDetailKey.getReceiptLineNumber()).isEqualTo("01152");
            });
        });
    }


    private void assertExpectedAndReceivedSkusCorrect(Map<SkuId, ExpectedAndReceivedAmount> idsToExpectedAmounts,
                                                      List<ExpectedAndReceivedSku> actualAmounts) {
        assertions.assertThat(actualAmounts).hasSize(idsToExpectedAmounts.size());
        for (ExpectedAndReceivedSku actualAmountForSku : actualAmounts) {
            SkuId skuId = actualAmountForSku.getKey().getSkuId();
            ExpectedAndReceivedAmount expectedAmounts = idsToExpectedAmounts.get(skuId);
            assertions.assertThat(actualAmountForSku.getQuantityExpected()).
                    isEqualByComparingTo(expectedAmounts.expectedAmount);
            assertions.assertThat(actualAmountForSku.getQuantityReceived()).
                    isEqualByComparingTo(expectedAmounts.receivedAmount);
        }
    }

    private static class ExpectedAndReceivedAmount {
        private final BigDecimal expectedAmount;
        private final BigDecimal receivedAmount;

        private ExpectedAndReceivedAmount(BigDecimal expectedAmount, BigDecimal receivedAmount) {
            this.expectedAmount = expectedAmount;
            this.receivedAmount = receivedAmount;
        }
    }
}
