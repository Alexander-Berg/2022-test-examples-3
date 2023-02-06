package ru.yandex.market.abo.cpa.pinger.accept

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.exception.ExceptionalShopsService
import ru.yandex.market.abo.cpa.order.ExpiredOrdersProcessor
import ru.yandex.market.abo.cpa.pushapi.PushApiLog
import ru.yandex.market.abo.cpa.pushapi.PushApiLogRepo
import ru.yandex.market.abo.cpa.pushapi.PushApiMethod
import ru.yandex.market.abo.cpa.pushapi.pinger.PingerStatService
import ru.yandex.market.abo.util.db.toggle.ToggleService
import ru.yandex.market.checkout.checkouter.client.CheckouterAPI
import ru.yandex.market.checkout.checkouter.order.Color
import ru.yandex.market.checkout.checkouter.order.Context
import ru.yandex.market.checkout.checkouter.order.Order
import ru.yandex.market.checkout.checkouter.order.OrderStatus
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus
import ru.yandex.market.checkout.checkouter.order.PagedOrders

class PingerAcceptManagerTest @Autowired constructor(
    pingerStatService: PingerStatService,
    val pingerAcceptOrderRepo: PingerAcceptOrderRepo,
    val pushApiLogRepo: PushApiLogRepo,
    val transactionTemplate: TransactionTemplate,
) : EmptyTest() {

    val checkouterClient: CheckouterAPI = mock()
    val expiredOrdersProcessor: ExpiredOrdersProcessor = mock()
    val dbToggleService: ToggleService = mock()
    val exceptionalShopsService: ExceptionalShopsService = mock()

    val pingerAcceptManager = PingerAcceptManager(
        pingerStatService,
        pingerAcceptOrderRepo,
        expiredOrdersProcessor,
        checkouterClient,
        exceptionalShopsService,
        dbToggleService,
        transactionTemplate,
    )

    var order: Order = mock()

    @BeforeEach
    fun setup() {
        whenever(checkouterClient.getOrder(any(), any())).thenReturn(order)
        whenever(checkouterClient.getOrdersByShop(any(), any())).thenReturn(PagedOrders(listOf(), null))
        whenever(dbToggleService.configDisabled(any())).thenReturn(false)
        whenever(exceptionalShopsService.loadShops(any())).thenReturn(setOf())
    }

    @Test
    fun `create new orders records`() {
        whenever(order.status).thenReturn(OrderStatus.PENDING)
        whenever(order.substatus).thenReturn(OrderSubstatus.AWAIT_CONFIRMATION)

        val partnerId = 1L
        assertTrue(pingerAcceptOrderRepo.findAll().isEmpty())

        pushApi {
            log(LocalDateTime.now().minusMinutes(16), partnerId, 1L, this)
            log(LocalDateTime.now(), partnerId, 1L, this)
        }

        pingerAcceptManager.processNewOrders()
        assertEquals(1, pingerAcceptOrderRepo.findAll().size)

        pushApi {
            log(LocalDateTime.now(), partnerId, 1L, this)
        }

        pingerAcceptManager.processNewOrders()
        assertEquals(1, pingerAcceptOrderRepo.findAll().size)
        verify(expiredOrdersProcessor).pingerCutoffOpen(any(), any())
    }

    @Test
    fun `process bad orders records`() {
        pingerAcceptOrderRepo.save(PingerAcceptOrder(1L, 2L))

        whenever(order.status).thenReturn(OrderStatus.PENDING)
        whenever(order.substatus).thenReturn(OrderSubstatus.AWAIT_CONFIRMATION)

        pingerAcceptManager.processBadOrders()
        assertTrue(pingerAcceptOrderRepo.findAllByFinishTimeIsNull().isNotEmpty())

        whenever(order.status).thenReturn(OrderStatus.PROCESSING)
        pingerAcceptManager.processBadOrders()
        assertTrue(pingerAcceptOrderRepo.findAllByFinishTimeIsNull().isEmpty())
        verify(expiredOrdersProcessor).pingerCutoffClose(any())
    }

    @Test
    fun `expired order`() {
        val partnerId = 1L
        val badOrderId = 2L
        pingerAcceptOrderRepo.save(PingerAcceptOrder(badOrderId, partnerId))

        whenever(order.status).thenReturn(OrderStatus.CANCELLED)

        pushApi {
            log(LocalDateTime.now().minusMinutes(16), partnerId, badOrderId + 1, this)
            log(LocalDateTime.now(), partnerId, badOrderId + 1, this)
        }

        pingerAcceptManager.processBadOrders()
        assertEquals(1, pingerAcceptOrderRepo.findAllByFinishTimeIsNull().size)
        verify(expiredOrdersProcessor, never()).switchOffPartner(any(), any(), any(), any())
    }


    @Test
    fun `expired order with substatus`() {
        val partnerId = 1L
        val badOrderId = 2L
        pingerAcceptOrderRepo.save(PingerAcceptOrder(badOrderId, partnerId))

        whenever(order.substatus).thenReturn(ExpiredOrdersProcessor.EXPIRED_SUBSTATUS)
        whenever(order.rgb).thenReturn(Color.WHITE)

        pingerAcceptManager.processBadOrders()
        assertTrue(pingerAcceptOrderRepo.findAllByFinishTimeIsNull().isEmpty())

        verify(expiredOrdersProcessor).switchOffPartner(any(), any(), any(), any())
    }

    @Test
    fun `order is successfully procesed`() {
        pingerAcceptOrderRepo.save(PingerAcceptOrder(1L, 2L))

        whenever(order.status).thenReturn(OrderStatus.PROCESSING)

        pingerAcceptManager.processBadOrders()
        assertTrue(pingerAcceptOrderRepo.findAllByFinishTimeIsNull().isEmpty())
        verify(expiredOrdersProcessor, never()).switchOffPartner(any(), any(), any(), any())
    }

    fun pushApi(init: MutableList<PushApiLog>.() -> Unit) {
        val list = arrayListOf<PushApiLog>()
        list.init()
        pushApiLogRepo.saveAll(list)
    }

    fun log(eventTime: LocalDateTime, shopId: Long, orderId: Long, list : MutableList<PushApiLog>) {
        val l = PushApiLog().apply{
            this.eventTime = eventTime
            this.shopId = shopId
            this.orderId = orderId
            this.isSuccess = false
            this.requestMethod = PushApiMethod.ORDER_ACCEPT.url
            this.context = Context.MARKET
            this.requestId = RND.nextLong().toString()
        }
        list.add(l)
    }

}
