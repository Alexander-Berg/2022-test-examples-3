package ru.yandex.market.billing.tlog.dao

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.FunctionalTest
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.util.MbiAsserts
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@DisplayName("Тесты для PayoutsTransactionLogYtDao")
class PayoutsTransactionLogYtDaoTest : FunctionalTest() {

    @Autowired
    private lateinit var paymentsPayoutsTransactionLogDao: PayoutsTransactionLogDao

    @Autowired
    private lateinit var expensesPayoutsTransactionLogDao: PayoutsTransactionLogDao

    @Autowired
    private lateinit var paymentsPayoutsTransactionLogYtDao: PayoutsTransactionLogYtDao

    @Autowired
    private lateinit var expensesPayoutsTransactionLogYtDao: PayoutsTransactionLogYtDao

    @Test
    @DisplayName("Сериализация в JSON записи с payload из payments_payouts_transaction_log")
    @DbUnitDataSet(before = ["PayoutsTransactionLogYtDaoTest.paymentsItemWithPayloadToJsonNodes.csv"])
    fun testToJsonNodesWithPayloadColumn() {
        val eventTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:12"), ZoneId.systemDefault())
        val transactionTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:15"), ZoneId.systemDefault())
        val expected = """
            [
              {
                "transaction_id": 2,
                "event_time": "${DATE_TIME_FORMATTER.format(eventTime)}",
                "transaction_time": "${DATE_TIME_FORMATTER.format(transactionTime)}",
                "record_type": "accrual",
                "factoring": "market",
                "client_id": "8",
                "partner_id": null,
                "entity_id": null,
                "entity_type": null,
                "service_id": 609,
                "contract_id": 876124,
                "product": "partner_payment",
                "transaction_type": "payment",
                "paysys_type_cc": "acc_sberbank",
                "paysys_partner_id": null,
                "secured_payment": false,
                "service_transaction_id": "1",
                "currency": "RUB",
                "amount": "234.56",
                "ignore_in_balance": false,
                "ignore_in_oebs": false,
                "payload": "{\"end_datetime\":\"2020-03-31T15:30:00.000000+10:00\",\"start_datetime\":\"2020-03-01T15:30:00.000000+10:00\",\"type\":\"type2\"}",
                "previous_transaction_id": null,
                "order_id": null,
                "checkouter_id": null,
                "org_id": null,
                "terminal_id": 121,
                "terminal_contract_id": "121SLS"
              }
            ]
            """
        val payoutsTransactionLogItems = paymentsPayoutsTransactionLogDao.getTransactionLogItems(0, 500)
        val jsonNodes = paymentsPayoutsTransactionLogYtDao.toJsonNodes(payoutsTransactionLogItems)
        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString())
    }

    @Test
    @DisplayName("Сериализация в JSON записи без payload из payments_payouts_transaction_log")
    @DbUnitDataSet(before = ["PayoutsTransactionLogYtDaoTest.paymentsItemWithoutPayloadToJsonNodes.csv"])
    fun testToJsonNodesWithoutPayloadColumn() {
        val eventTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:12"), ZoneId.systemDefault())
        val transactionTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:15"), ZoneId.systemDefault())
        val expected = """
            [
              {
                "transaction_id": 3,
                "event_time": "${DATE_TIME_FORMATTER.format(eventTime)}",
                "transaction_time": "${DATE_TIME_FORMATTER.format(transactionTime)}",
                "record_type": "accrual",
                "factoring": "market",
                "client_id": "4",
                "partner_id": null,
                "entity_id": null,
                "entity_type": null,
                "service_id": 610,
                "contract_id": 7346,
                "product": "partner_payment",
                "transaction_type": "payment",
                "paysys_type_cc": "acc_google_pay",
                "paysys_partner_id": 821645,
                "secured_payment": false,
                "service_transaction_id": "1",
                "currency": "RUB",
                "amount": "345.67",
                "ignore_in_balance": false,
                "ignore_in_oebs": false,
                "payload": "{}",
                "previous_transaction_id": null,
                "order_id": null,
                "checkouter_id": null,
                "org_id": null,
                "terminal_id": null,
                "terminal_contract_id": null
              }
            ]
            """
        val payoutsTransactionLogItems = paymentsPayoutsTransactionLogDao.getTransactionLogItems(0, 500)
        val jsonNodes = paymentsPayoutsTransactionLogYtDao.toJsonNodes(payoutsTransactionLogItems)
        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString())
    }

    @Test
    @DisplayName("Сериализация в JSON записи без payload из expenses_payouts_transaction_log")
    @DbUnitDataSet(before = ["PayoutsTransactionLogYtDaoTest.expensesItemWithoutPayloadToJsonNodes.csv"])
    fun testExpensesTransactionLogItemToJsonNodesWithoutPayloadColumn() {
        val eventTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:12"), ZoneId.systemDefault())
        val transactionTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:15"), ZoneId.systemDefault())
        val expected = """
            [
              {
                "transaction_id": 5,
                "event_time": "${DATE_TIME_FORMATTER.format(eventTime)}",
                "transaction_time": "${DATE_TIME_FORMATTER.format(transactionTime)}",
                "record_type": "payment",
                "factoring": "market",
                "client_id": "41",
                "partner_id": null,
                "entity_id": null,
                "entity_type": null,
                "service_id": 609,
                "contract_id": 143086,
                "product": "partner_payment",
                "transaction_type": "payment",
                "paysys_type_cc": "yamarketplus",
                "paysys_partner_id": 126497,
                "secured_payment": false,
                "service_transaction_id": "1",
                "currency": "RUB",
                "amount": "654.32",
                "ignore_in_balance": false,
                "ignore_in_oebs": false,
                "payload": "{}",
                "previous_transaction_id": null,
                "order_id": null,
                "checkouter_id": null,
                "org_id": null,
                "terminal_id": null,
                "terminal_contract_id": null
              }
            ]
            """
        val payoutsTransactionLogItems = expensesPayoutsTransactionLogDao.getTransactionLogItems(0, 500)
        val jsonNodes = expensesPayoutsTransactionLogYtDao.toJsonNodes(payoutsTransactionLogItems)
        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString())
    }

    @Test
    @DisplayName("Сериализация в JSON записи payments_payouts_transaction_log для factoring == null")
    @DbUnitDataSet(before = ["PayoutsTransactionLogYtDaoTest.testToJsonNodesPaymentsWithFactoringIsNull.before.csv"])
    fun testToJsonNodesPaymentsWithFactoringIsNull() {
        val eventTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:12"), ZoneId.systemDefault())
        val transactionTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:15"), ZoneId.systemDefault())
        val expected = """
            [
              {
                "transaction_id": 3,
                "event_time": "${DATE_TIME_FORMATTER.format(eventTime)}",
                "transaction_time": "${DATE_TIME_FORMATTER.format(transactionTime)}",
                "record_type": "accrual",
                "factoring": null,
                "client_id": "4",
                "partner_id": null,
                "entity_id": null,
                "entity_type": null,
                "service_id": 610,
                "contract_id": 7346,
                "product": "partner_payment",
                "transaction_type": "payment",
                "paysys_type_cc": "acc_google_pay",
                "paysys_partner_id": 821645,
                "secured_payment": false,
                "service_transaction_id": "1",
                "currency": "RUB",
                "amount": "345.67",
                "ignore_in_balance": false,
                "ignore_in_oebs": false,
                "payload": "{}",
                "previous_transaction_id": null,
                "order_id": null,
                "checkouter_id": null,
                "org_id": null,
                "terminal_id": null,
                "terminal_contract_id": null
              }
            ]
            """
        val payoutsTransactionLogItems = paymentsPayoutsTransactionLogDao.getTransactionLogItems(0, 500)
        val jsonNodes = paymentsPayoutsTransactionLogYtDao.toJsonNodes(payoutsTransactionLogItems)
        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString())
    }

    @Test
    @DisplayName("Сериализация в JSON записи expenses_payouts_transaction_log для factoring == null")
    @DbUnitDataSet(before = ["PayoutsTransactionLogYtDaoTest.testToJsonNodesExpensesWithFactoringIsNull.before.csv"])
    fun testToJsonNodesExpensesWithFactoringIsNull() {
        val eventTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:12"), ZoneId.systemDefault())
        val transactionTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:15"), ZoneId.systemDefault())
        val expected = """
            [
              {
                "transaction_id": 5,
                "event_time": "${DATE_TIME_FORMATTER.format(eventTime)}",
                "transaction_time": "${DATE_TIME_FORMATTER.format(transactionTime)}",
                "record_type": "payment",
                "factoring": null,
                "client_id": "41",
                "partner_id": null,
                "entity_id": null,
                "entity_type": null,
                "service_id": 609,
                "contract_id": 143086,
                "product": "partner_payment",
                "transaction_type": "payment",
                "paysys_type_cc": "yamarketplus",
                "paysys_partner_id": 126497,
                "secured_payment": false,
                "service_transaction_id": "1",
                "currency": "RUB",
                "amount": "654.32",
                "ignore_in_balance": false,
                "ignore_in_oebs": false,
                "payload": "{}",
                "previous_transaction_id": null,
                "order_id": null,
                "checkouter_id": null,
                "org_id": null,
                "terminal_id": null,
                "terminal_contract_id": null
              }
            ]
            """
        val payoutsTransactionLogItems = expensesPayoutsTransactionLogDao.getTransactionLogItems(0, 500)
        val jsonNodes = expensesPayoutsTransactionLogYtDao.toJsonNodes(payoutsTransactionLogItems)
        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString())
    }

    @Test
    @DisplayName("Сериализация в JSON записи expenses_payouts_transaction_log для factoring == null")
    @DbUnitDataSet(before = ["PayoutsTransactionLogYtDaoTest.testOrderIdAndCheckouterIdInsert.before.csv"])
    fun testOrderId() {
        val eventTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:12"), ZoneId.systemDefault())
        val transactionTime = ZonedDateTime.of(LocalDateTime.parse("2021-07-26T12:00:15"), ZoneId.systemDefault())
        val expected = """
            [
              {
                "transaction_id": 5,
                "event_time": "${DATE_TIME_FORMATTER.format(eventTime)}",
                "transaction_time": "${DATE_TIME_FORMATTER.format(transactionTime)}",
                "record_type": "payment",
                "factoring": null,
                "client_id": "41",
                "partner_id": null,
                "entity_id": null,
                "entity_type": null,
                "service_id": 609,
                "contract_id": 143086,
                "product": "partner_payment",
                "transaction_type": "payment",
                "paysys_type_cc": "yamarketplus",
                "paysys_partner_id": 126497,
                "secured_payment": false,
                "service_transaction_id": "1",
                "currency": "RUB",
                "amount": "654.32",
                "ignore_in_balance": false,
                "ignore_in_oebs": false,
                "payload": "{}",
                "previous_transaction_id": null,
                "order_id": 1,
                "checkouter_id": 2,
                "org_id": null,
                "terminal_id": 121,
                "terminal_contract_id": "121SLS"
              }
            ]
            """
        val payoutsTransactionLogItems = expensesPayoutsTransactionLogDao.getTransactionLogItems(0, 500)
        val jsonNodes = expensesPayoutsTransactionLogYtDao.toJsonNodes(payoutsTransactionLogItems)
        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString())
    }
}
