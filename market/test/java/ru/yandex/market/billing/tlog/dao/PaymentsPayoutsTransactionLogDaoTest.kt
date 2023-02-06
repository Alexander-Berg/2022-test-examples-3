package ru.yandex.market.billing.tlog.dao

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.FunctionalTest
import ru.yandex.market.common.test.db.DbUnitDataSet
import java.time.LocalDate
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
@DisplayName("Тесты для PaymentsPayoutsTransactionLogDao")
@DbUnitDataSet(before = ["PaymentsPayoutsTransactionLogDaoTest.before.csv"])
class PaymentsPayoutsTransactionLogDaoTest : FunctionalTest() {

    @Autowired
    private lateinit var paymentsPayoutsTransactionLogDao: PayoutsTransactionLogDao

    @Test
    @DisplayName("Получение списка транзакций")
    fun transactionsToExport() {
        val transactionIds = paymentsPayoutsTransactionLogDao.getTransactionLogItems(2, 3)
            .map { it.transactionId }
        Assertions.assertIterableEquals(listOf(3L, 4L, 5L), transactionIds)
    }

    @Test
    @DisplayName("Обновление даты экспорта в YT")
    @DbUnitDataSet(after = ["PaymentsPayoutsTransactionLogDaoTest.updateExportDate.after.csv"])
    fun updateExportDate() {
        paymentsPayoutsTransactionLogDao.updateExportDate(listOf(2L, 3L, 5L), LocalDate.of(2021, 7, 27))
    }
}
