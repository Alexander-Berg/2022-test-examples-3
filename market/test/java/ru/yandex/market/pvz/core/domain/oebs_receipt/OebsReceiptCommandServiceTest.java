package ru.yandex.market.pvz.core.domain.oebs_receipt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.configuration.global.ConfigurationGlobalCommandService;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.market.pvz.core.domain.configuration.ConfigurationProperties.OEBS_INCORRECT;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OebsReceiptCommandServiceTest {

    private final ConfigurationGlobalCommandService configurationGlobalCommandService;
    private final OebsReceiptRepository oebsReceiptRepository;

    private final OebsReceiptCommandService oebsReceiptCommandService;

    @Test
    void saveAllReceipts() {
        List<OebsReceipt> receipts = new ArrayList<>();
        var receipt1 = OebsReceipt.builder()
                .oebsNumber("1")
                .paymentOrderNumber("101")
                .paymentOrderDate(LocalDate.of(2022, 1, 14))
                .virtualAccountNumber("V_DOSTAVKA_1001")
                .sum(BigDecimal.valueOf(4500))
                .build();
        var receipt2 = OebsReceipt.builder()
                .oebsNumber("2")
                .paymentOrderNumber("201")
                .paymentOrderDate(LocalDate.of(2022, 1, 14))
                .virtualAccountNumber("V_DOSTAVKA_1002")
                .sum(BigDecimal.valueOf(41500))
                .build();
        receipts.add(receipt1);
        receipts.add(receipt2);

        oebsReceiptCommandService.saveAll(receipts);

        List<OebsReceipt> actual = oebsReceiptRepository.findAll();
        assertThat(actual).hasSize(2);
    }

    @Test
    void errorOnDuplicateOebsNumbers() {
        List<OebsReceipt> receipts = new ArrayList<>();
        var receipt1 = OebsReceipt.builder()
                .oebsNumber("1")
                .paymentOrderNumber("101")
                .paymentOrderDate(LocalDate.of(2022, 1, 14))
                .virtualAccountNumber("V_DOSTAVKA_1001")
                .sum(BigDecimal.valueOf(4500))
                .build();
        var receipt2 = OebsReceipt.builder()
                .oebsNumber("1")
                .paymentOrderNumber("101")
                .paymentOrderDate(LocalDate.of(2022, 1, 14))
                .virtualAccountNumber("V_DOSTAVKA_1002")
                .sum(BigDecimal.valueOf(41500))
                .build();
        receipts.add(receipt1);
        receipts.add(receipt2);

        assertThatThrownBy(() -> oebsReceiptCommandService.saveAll(receipts))
                .isExactlyInstanceOf(IllegalStateException.class);
    }

    @Test
    void noErrorOnDuplicateOebsNumbersWithIncorrectOebsToggle() {
        configurationGlobalCommandService.setValue(OEBS_INCORRECT, true);
        List<OebsReceipt> receipts = new ArrayList<>();
        var receipt1 = OebsReceipt.builder()
                .oebsNumber("1")
                .paymentOrderNumber("101")
                .paymentOrderDate(LocalDate.of(2022, 1, 14))
                .virtualAccountNumber("V_DOSTAVKA_1001")
                .sum(BigDecimal.valueOf(4500))
                .build();
        var receipt2 = OebsReceipt.builder()
                .oebsNumber("1")
                .paymentOrderNumber("101")
                .paymentOrderDate(LocalDate.of(2022, 1, 14))
                .virtualAccountNumber("V_DOSTAVKA_1002")
                .sum(BigDecimal.valueOf(41500))
                .build();
        receipts.add(receipt1);
        receipts.add(receipt2);

        oebsReceiptCommandService.saveAll(receipts);

        List<OebsReceipt> actual = oebsReceiptRepository.findAll();
        assertThat(actual).hasSize(1);
    }

    @Test
    void errorOnDuplicateOebsNumbersWithIncorrectOebsToggleAndDifferentPaymentData() {
        configurationGlobalCommandService.setValue(OEBS_INCORRECT, true);
        List<OebsReceipt> receipts = new ArrayList<>();
        var receipt1 = OebsReceipt.builder()
                .oebsNumber("1")
                .paymentOrderNumber("101")
                .paymentOrderDate(LocalDate.of(2022, 1, 14))
                .virtualAccountNumber("V_DOSTAVKA_1001")
                .sum(BigDecimal.valueOf(4500))
                .build();
        var receipt2 = OebsReceipt.builder()
                .oebsNumber("1")
                .paymentOrderNumber("102")
                .paymentOrderDate(LocalDate.of(2022, 1, 14))
                .virtualAccountNumber("V_DOSTAVKA_1002")
                .sum(BigDecimal.valueOf(41500))
                .build();
        receipts.add(receipt1);
        receipts.add(receipt2);

        assertThatThrownBy(() -> oebsReceiptCommandService.saveAll(receipts))
                .isExactlyInstanceOf(IllegalStateException.class);
    }
}
