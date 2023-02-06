package ru.yandex.market.billing.payment.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

public class CourierTransactionProcessingServiceTest extends FunctionalTest {

    @Autowired
    private CourierTransactionProcessingService courierTransactionProcessingService;

    @Test
    @DisplayName("Тест успешного создания начислений из транзакции курьерской службы")
    @DbUnitDataSet(
            before = "CourierTransactionProcessingServiceTest.TestSuccessCase.before.csv",
            after = "CourierTransactionProcessingServiceTest.TestSuccessCase.after.csv"
    )
    void testSuccessCreationAccruals() {
        courierTransactionProcessingService.processTransactions();
    }

    @Test
    @DisplayName("Тест что обрабатываются только необработанные транзакции")
    @DbUnitDataSet(
            before = "CourierTransactionProcessingServiceTest.AlreadyProcessed.before.csv",
            after = "CourierTransactionProcessingServiceTest.AlreadyProcessed.after.csv"
    )
    void testDoNotProcessAlreadyProcessedTransactions() {
        courierTransactionProcessingService.processTransactions();
    }

    @Test
    @DisplayName("Тест что джоба не упадет при отсутствии транзакций для создания аккруалов")
    void testSuccessJobExitWhenTransactionListIsEmpty() {
        courierTransactionProcessingService.processTransactions();
    }

    @Test
    @DisplayName("Корректно проставляем тип партнера - курьерская служба или самозанятой")
    @DbUnitDataSet(
            before = "CourierTransactionProcessingServiceTest.SaveCourierTypeField.before.csv",
            after = "CourierTransactionProcessingServiceTest.SaveCourierTypeField.after.csv"
    )
    void shouldSaveSelfEmployedFieldWhenCourierTypeGiven() {
        courierTransactionProcessingService.processTransactions();
    }
}
