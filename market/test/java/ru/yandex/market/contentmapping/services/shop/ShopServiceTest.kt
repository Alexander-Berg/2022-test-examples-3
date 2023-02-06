package ru.yandex.market.contentmapping.services.shop

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.After
import org.junit.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.contentmapping.dto.model.Shop
import ru.yandex.market.contentmapping.dto.model.UpdateStatus
import ru.yandex.market.contentmapping.repository.ShopRepository
import ru.yandex.market.contentmapping.services.DatacampOfferImportService
import ru.yandex.market.contentmapping.testutils.BaseAppTestClass
import ru.yandex.market.mbo.taskqueue.TaskRecord

class ShopServiceTest : BaseAppTestClass() {
    @Autowired
    lateinit var shopService: ShopService

    @Autowired
    lateinit var shopRepository: ShopRepository

    @Autowired
    lateinit var statusService: ShopUpdateStatusService

    @Autowired
    lateinit var updateOffersProcessingHandler: UpdateOffersProcessingHandler

    @Autowired
    lateinit var datacampOfferImportService: DatacampOfferImportService

    @Qualifier("newTransactionTemplate")
    @Autowired
    lateinit var newTransactionTemplate: TransactionTemplate

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @After
    fun resetSpies() {
        Mockito.reset(newTransactionTemplate)
        Mockito.reset(datacampOfferImportService)
    }

    @Test
    fun `Shop update should create task in task queue and status update logger`() {
        val shop = shopRepository.insert(Shop(42L, "Test shop"))
        shopService.addOfferUpdateTask(shop.id)

        val updates = statusService.findStatus(shop.id)
        updates shouldNotBe null
    }

    @Test
    fun `Should report status while executing task`() {
        val shop = shopRepository.insert(Shop(42L, "Test shop"))
        val taskId = shopService.addOfferUpdateTask(shop.id) ?: throw AssertionError("Must generate id")

        fun currentStatus(): ShopUpdateStatus = statusService.findStatusByTaskId(taskId)
        currentStatus().started shouldBe null

        Mockito.doAnswer { call ->
            currentStatus().started shouldNotBe null
            val progress: (DatacampOfferImportService.Progress) -> Unit = call.getArgument(1)
            progress(DatacampOfferImportService.Progress(42))
            currentStatus().updateCount shouldBe 42
        }.whenever(datacampOfferImportService).import(any(), any())

        mockNewTransactionHandler()

        updateOffersProcessingHandler.handle(OfferUpdateTask(shop.id), TaskRecord().setId(taskId))

        currentStatus().finished shouldNotBe null
    }

    private fun mockNewTransactionHandler() {
        Mockito.doAnswer { call ->
            transactionTemplate.execute(call.getArgument(0))
        }.whenever(newTransactionTemplate).execute<Unit>(any())
    }

    @Test
    fun `Mark failed once failed`() {
        val shop = shopRepository.insert(Shop(42L, "Test shop"))
        val taskId = shopService.addOfferUpdateTask(shop.id) ?: throw AssertionError("Must generate id")

        fun currentStatus(): ShopUpdateStatus = statusService.findStatusByTaskId(taskId)
        currentStatus().asClue {
            it.started shouldBe null
            it.inQueue shouldBe true
            it.retry shouldBe 0
        }

        Mockito.doAnswer { call ->
            currentStatus().inQueue shouldBe false
            throw IllegalStateException("Something is going wrong")
        }.whenever(datacampOfferImportService).import(any(), any())

        mockNewTransactionHandler()

        shouldThrow<IllegalStateException> {
            updateOffersProcessingHandler.handle(OfferUpdateTask(shop.id), TaskRecord().setId(taskId))
        }

        currentStatus().asClue {
            it.started shouldNotBe null
            it.retry shouldBe 1
            it.inQueue shouldBe true
        }
    }

    @Test
    fun `should recovered failed task`() {
        val taskId = 1L
        statusService.startUpdate(taskId, 1L)
        (1..2).forEach { _ -> statusService.markFailed(taskId) }
        val failedStatusShops = statusService.findFailedNonRecoveredShops(2)
        failedStatusShops.asClue {
            it.size shouldBe 1
        }
        statusService.markAsRecoveredStatus(listOf(taskId))
        statusService.findFailedNonRecoveredShops(2).asClue {
            it.size shouldBe 0
        }
        statusService.findStatusByTaskId(taskId).asClue {
            it.isStatusRecovered shouldBe true
        }
    }
}
