package ru.yandex.market.billing.fulfillment;

import java.time.LocalDate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.fulfillment.supplies.dao.FulfillmentSupplyYtDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.WithdrawDao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;

class WithdrawsImportServiceTest extends FunctionalTest {

    private static final LocalDate DATE_2018_05_17 = LocalDate.of(2018, 5, 17);
    private static final LocalDate DATE_2018_05_18 = LocalDate.of(2018, 5, 18);
    private static final LocalDate DATE_2018_05_19 = LocalDate.of(2018, 5, 19);

    @Autowired
    private WithdrawDao withdrawDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private FulfillmentSupplyYtDao fulfillmentSupplyYtDao;

    private WithdrawsImportService withdrawsImportService;

    @BeforeEach
    void setUp() {
        withdrawsImportService = new WithdrawsImportService(
                withdrawDao, transactionTemplate, fulfillmentSupplyYtDao);
    }

    @Test
    @DisplayName("Проверка успешных кейсов импорта изъятий")
    @DbUnitDataSet(
            before = "WithdrawsImportServiceTest.before.csv",
            after = "WithdrawsImportServiceTest.after.csv"
    )
    void testImportWithdraws() {
        withdrawsImportService.importWithdraws(DATE_2018_05_17);
        withdrawsImportService.importWithdraws(DATE_2018_05_18);
    }

    @Test
    @DisplayName("Проверка не успешных кейсов импорта изъятий")
    @DbUnitDataSet(
            before = "WithdrawsImportServiceTest.before.csv",
            after = "WithdrawsImportServiceTest.before.csv"
    )
    void shouldThrowExceptionOnNegativeFactCount() {
        RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
                () -> withdrawsImportService.importWithdraws(
                        DATE_2018_05_19)
        );
        assertThat(exception.getMessage(),
                equalToIgnoringCase("Count should be greater or equal than zero, withdrawId=103")
        );
    }
}
