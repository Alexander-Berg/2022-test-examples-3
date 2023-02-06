package ru.yandex.market.billing.imports.tpl.courier;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static ru.yandex.market.billing.imports.tpl.courier.MoneyFlowTransactionYtTestUtil.mockYt;

public class MoneyFlowTransactionImportServiceTest extends FunctionalTest {
    private static final LocalDate CALC_DATE = LocalDate.of(2022, 4, 8);

    @Autowired
    private MoneyFlowTransactionImportService service;

    @Autowired
    private Yt yt;
    @Autowired
    private YtCluster ytCluster;

    @Test
    @DisplayName("Общий тест на несколько записей.")
    @DbUnitDataSet(
            before = "MoneyFlowTransactionImportServiceTest.before.csv",
            after = "MoneyFlowTransactionImportServiceTest.after.csv"
    )
    void importSavesData() {
        mockYt(ytCluster, yt, getTransactions());
        service.process(CALC_DATE);
    }

    @Test
    @DisplayName("Игнорируем транзакцию с ошибкой, если она в списке игнора.")
    @DbUnitDataSet(
            before = "MoneyFlowTransactionImportServiceTest.ignoreIncorrectTransaction.before.csv",
            after = "MoneyFlowTransactionImportServiceTest.ignoreIncorrectTransaction.after.csv"
    )
    void shouldIgnoreIncorrectTransactionWhenIgnoreIdGiven() {
        mockYt(ytCluster, yt, getBrokenTransactions());
        service.process(CALC_DATE);
    }

    @Test
    @DisplayName("Падаем, если есть некорректная транзакция, и она не в игноре.")
    void shouldFailWhenIncorrectTransactionGiven() {
        mockYt(ytCluster, yt, getBrokenTransactions());
        Assertions.assertThrows(
                NullPointerException.class,
                () -> service.process(CALC_DATE)
        );
    }

    @Nonnull
    private List<Map<String, Object>> getTransactions() {
        return List.of(
                Map.ofEntries(
                        Map.entry("id", 1L),
                        Map.entry("userShiftId", 1L),
                        Map.entry("transactionType", "payments"),
                        Map.entry("eventTime", "2022-04-07"),
                        Map.entry("trantime", "2022-04-08"),
                        Map.entry("partnerId", 100L),
                        Map.entry("paymentType", "PAYMENT"),
                        Map.entry("productType", "MGT"),
                        Map.entry("orderId", 10L),
                        Map.entry("amount", 1337L),
                        Map.entry("currency", "RUB"),
                        Map.entry("isCorrection", false),
                        Map.entry("orgId", 64554L),
                        Map.entry("userType", "PARTNER"),
                        Map.entry("clientId", "1234567"),
                        Map.entry("contractId", "987654"),
                        Map.entry("personId", "11223344")
                ),
                //проверка userShiftId - null
                Map.ofEntries(
                        Map.entry("id", 2L),
                        Map.entry("transactionType", "fines"),
                        Map.entry("eventTime", "2022-04-07"),
                        Map.entry("trantime", "2022-04-08"),
                        Map.entry("partnerId", 100L),
                        Map.entry("paymentType", "REFUND"),
                        Map.entry("productType", "MGT"),
                        Map.entry("orderId", 10L),
                        Map.entry("amount", 2000L),
                        Map.entry("currency", "RUB"),
                        Map.entry("isCorrection", false),
                        Map.entry("orgId", 64554L),
                        Map.entry("userType", "PARTNER"),
                        Map.entry("clientId", "1234567"),
                        Map.entry("contractId", "987654"),
                        Map.entry("personId", "11223344")
                ),
                //проверка orderId - null
                Map.ofEntries(
                        Map.entry("id", 3L),
                        Map.entry("userShiftId", 1L),
                        Map.entry("transactionType", "payments"),
                        Map.entry("eventTime", "2022-04-07"),
                        Map.entry("trantime", "2022-04-08"),
                        Map.entry("partnerId", 100L),
                        Map.entry("paymentType", "PAYMENT"),
                        Map.entry("productType", "MGT"),
                        Map.entry("amount", 1000L),
                        Map.entry("currency", "RUB"),
                        Map.entry("isCorrection", false),
                        Map.entry("orgId", 64554L),
                        Map.entry("userType", "SELF_EMPLOYED"),
                        Map.entry("clientId", "1234567"),
                        Map.entry("contractId", "987654"),
                        Map.entry("personId", "11223344")
                ),
                //проверка, что существующая транзакция в базе не обновится
                Map.ofEntries(
                        Map.entry("id", 4L),
                        Map.entry("userShiftId", 1L),
                        Map.entry("transactionType", "fines"),
                        Map.entry("eventTime", "2022-04-07"),
                        Map.entry("trantime", "2022-04-08"),
                        Map.entry("partnerId", 100L),
                        Map.entry("paymentType", "PAYMENT"),
                        Map.entry("productType", "MGT"),
                        Map.entry("amount", 1000L),
                        Map.entry("currency", "RUB"),
                        Map.entry("isCorrection", false),
                        Map.entry("orgId", 64554L),
                        Map.entry("userType", "SELF_EMPLOYED"),
                        Map.entry("clientId", "1234567"),
                        Map.entry("contractId", "987654"),
                        Map.entry("personId", "11223344")
                )
        );
    }

    private List<Map<String, Object>> getBrokenTransactions() {
        return List.of(
                //некорректная транзакция без courier_type
                Map.ofEntries(
                        Map.entry("id", 5L),
                        Map.entry("userShiftId", 1L),
                        Map.entry("transactionType", "payments"),
                        Map.entry("eventTime", "2022-04-07"),
                        Map.entry("trantime", "2022-04-08"),
                        Map.entry("partnerId", 100L),
                        Map.entry("paymentType", "PAYMENT"),
                        Map.entry("productType", "MGT"),
                        Map.entry("amount", 7892L),
                        Map.entry("currency", "RUB"),
                        Map.entry("isCorrection", false),
                        Map.entry("orgId", 64554L),
                        Map.entry("clientId", "1234567"),
                        Map.entry("contractId", "987654"),
                        Map.entry("personId", "11223344")
                ),
                //некорректная транзакция без client_id
                Map.ofEntries(
                        Map.entry("id", 6L),
                        Map.entry("userShiftId", 1L),
                        Map.entry("transactionType", "payments"),
                        Map.entry("eventTime", "2022-04-07"),
                        Map.entry("trantime", "2022-04-08"),
                        Map.entry("partnerId", 100L),
                        Map.entry("paymentType", "PAYMENT"),
                        Map.entry("productType", "MGT"),
                        Map.entry("amount", 7892L),
                        Map.entry("currency", "RUB"),
                        Map.entry("isCorrection", false),
                        Map.entry("orgId", 64554L),
                        Map.entry("userType", "SELF_EMPLOYED"),
                        Map.entry("contractId", "987654"),
                        Map.entry("personId", "11223344")
                ),
                //некорректная транзакция без contract_id
                Map.ofEntries(
                        Map.entry("id", 7L),
                        Map.entry("userShiftId", 1L),
                        Map.entry("transactionType", "payments"),
                        Map.entry("eventTime", "2022-04-07"),
                        Map.entry("trantime", "2022-04-08"),
                        Map.entry("partnerId", 100L),
                        Map.entry("paymentType", "PAYMENT"),
                        Map.entry("productType", "MGT"),
                        Map.entry("amount", 1000L),
                        Map.entry("currency", "RUB"),
                        Map.entry("isCorrection", false),
                        Map.entry("orgId", 64554L),
                        Map.entry("userType", "SELF_EMPLOYED"),
                        Map.entry("clientId", "1234567"),
                        Map.entry("personId", "11223344")
                )
        );
    }
}
