package ru.yandex.market.pvz.core.domain.oebs_receipt.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceipt;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceiptRepository;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.TestUtils.getFileContentInBytes;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OebsReceiptParserTest {

    private final OebsReceiptParser parser;
    private final OebsReceiptRepository oebsReceiptRepository;

    @Test
    void testParse() {
        parser.saveRowsFromExcelReport(getExampleFile());
        List<OebsReceipt> receipts = oebsReceiptRepository.findAll();
        assertCorrectReceipts(receipts);
    }

    @Test
    void testNoExceptionOnSameValues() {
        byte[] data = getExampleFile();
        parser.saveRowsFromExcelReport(data);
        parser.saveRowsFromExcelReport(data);
        List<OebsReceipt> receipts = oebsReceiptRepository.findAll();
        assertCorrectReceipts(receipts);
    }

    private void assertCorrectReceipts(List<OebsReceipt> receipts) {
        assertThat(receipts).containsExactlyInAnyOrderElementsOf(List.of(
                OebsReceipt.builder()
                        .oebsNumber("031443815")
                        .sum(BigDecimal.valueOf(12936L))
                        .paymentOrderNumber("1710")
                        .paymentOrderDate(LocalDate.of(2019, 4, 19))
                        .build(),

                OebsReceipt.builder()
                        .oebsNumber("031451011")
                        .sum(BigDecimal.valueOf(1729505.76))
                        .paymentOrderNumber("16273")
                        .paymentOrderDate(LocalDate.of(2019, 4, 19))
                        .build()
        ));
    }

    @SneakyThrows
    private byte[] getExampleFile() {
        return getFileContentInBytes("oebs/oebs-example.xlsx");
    }

}
