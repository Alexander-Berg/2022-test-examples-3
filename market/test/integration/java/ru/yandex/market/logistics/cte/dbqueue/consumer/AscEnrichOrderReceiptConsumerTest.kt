package ru.yandex.market.logistics.cte.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI
import ru.yandex.market.checkout.checkouter.receipt.Receipt
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType
import ru.yandex.market.checkout.checkouter.receipt.Receipts
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.dbqueue.payload.AscEnrichmentPayload
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import java.time.Clock
import java.time.ZonedDateTime

internal class AscEnrichOrderReceiptConsumerTest(
    @Autowired private val queueShard: QueueShard,
    @Autowired private val clock: Clock,
    @Autowired private val consumer: AscEnrichOrderReceiptConsumer,
    @Qualifier("checkouterApi") @Autowired private val checkouterClient: CheckouterAPI
) : IntegrationTest() {

    @Test
    @DatabaseSetup(
        value = ["classpath:/dbqueue/consumer/asc-enrichment-order-receipt/before.xml"]
    )
    @ExpectedDatabase(
        value = "classpath:dbqueue/consumer/asc-enrichment-order-receipt/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldSuccessfullyCreateDocumentLink() {
        whenever(checkouterClient.getOrdersReceipts(any(), any())).thenReturn(Receipts(listOf(
            Receipt().also { receipt ->
                receipt.status = ReceiptStatus.PRINTED
                receipt.type = ReceiptType.INCOME_RETURN
                receipt.items = listOf(ReceiptItem(111).also { receiptItem ->
                    receiptItem.orderId = 111
                    receiptItem.receiptId = 1111
                })
            },
            Receipt().also { receipt ->
                receipt.type = ReceiptType.INCOME_RETURN
                receipt.status = ReceiptStatus.PRINTED
                receipt.items = listOf(ReceiptItem(222).also { receiptItem ->
                    receiptItem.orderId = 222
                    receiptItem.receiptId = 2222
                })
            },
            Receipt().also { receipt ->
                receipt.type = ReceiptType.INCOME
                receipt.status = ReceiptStatus.PRINTED
                receipt.items = listOf(ReceiptItem(333).also { receiptItem ->
                    receiptItem.orderId = 333
                    receiptItem.receiptId = 3333
                })
            },
            Receipt().also { receipt -> // skip this receipt
                receipt.type = ReceiptType.OFFSET_ADVANCE_ON_DELIVERED
                receipt.status = ReceiptStatus.PRINTED
                receipt.items = listOf(ReceiptItem(4).also { receiptItem ->
                    receiptItem.orderId = 111
                    receiptItem.receiptId = 4444
                })
            },
        ))
        )
        val task = Task.builder<AscEnrichmentPayload>(queueShard.shardId)
            .withCreatedAt(ZonedDateTime.now(clock))
            .withPayload(AscEnrichmentPayload(listOf(1L, 2L, 3L), 1L))
            .build()
        consumer.execute(task)
    }

    @Test
    @DatabaseSetup(
        value = ["classpath:/dbqueue/consumer/asc-enrichment-order-receipt/before.xml"]
    )
    @ExpectedDatabase(
        value = "classpath:dbqueue/consumer/asc-enrichment-order-receipt/after-empty-checkouter-response.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldCorrectlyHandleEmptyCheckouterResponse() {
        whenever(checkouterClient.getOrdersReceipts(any(), any())).thenReturn(null)
        val task = Task.builder<AscEnrichmentPayload>(queueShard.shardId)
            .withCreatedAt(ZonedDateTime.now(clock))
            .withPayload(AscEnrichmentPayload(listOf(1L, 2L), 1L))
            .build()
        consumer.execute(task)
    }
}
