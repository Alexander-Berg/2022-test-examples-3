package ru.yandex.market.billing.tlog.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

/**
 * Тесты на сервис {@link CourierTransactionLogCollectionService}
 */
public class CourierTransactionLogCollectionServiceTest extends FunctionalTest {
    @Autowired
    private CourierTransactionLogCollectionService collectionService;

    @Test
    @DisplayName("Проверка, данные собираются в таблицу")
    @DbUnitDataSet(
            before = "CourierTransactionLogCollectionServiceTest.testCollection.before.csv",
            after = "CourierTransactionLogCollectionServiceTest.testCollection.after.csv"
    )
    void testCollection() {
        collectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Проверка, что джобу можно выключить")
    @DbUnitDataSet(
            before = "CourierTransactionLogCollectionServiceTest.testJobEnablement.before.csv",
            after = "CourierTransactionLogCollectionServiceTest.testJobEnablement.after.csv"
    )
    void testJobEnablement() {
        collectionService.collectTransactionLogItemsForAllTables();
    }

    @Test
    @DisplayName("Проверка, что собранные записи не перезабираются")
    @DbUnitDataSet(
            before = "CourierTransactionLogCollectionServiceTest.testAlreadyCollected.before.csv",
            after = "CourierTransactionLogCollectionServiceTest.testAlreadyCollected.after.csv"
    )
    void testAlreadyCollected() {
        collectionService.collectTransactionLogItemsForAllTables();
    }
}
