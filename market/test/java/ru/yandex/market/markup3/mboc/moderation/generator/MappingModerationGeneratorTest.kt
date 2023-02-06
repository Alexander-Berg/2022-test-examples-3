package ru.yandex.market.markup3.mboc.moderation.generator

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.markup3.api.Markup3ApiService
import ru.yandex.market.markup3.mboc.category.info.service.CategoryInfoService
import ru.yandex.market.markup3.mboc.moderation.MbocMappingModerationConstants.YANG_GROUP_KEY
import ru.yandex.market.markup3.mboc.moderation.generator.AbstractMappingModerationGenerator.Companion.convertMbocPriority
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskKey
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskStatus
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.mboc.offertask.service.OfferTaskService
import ru.yandex.market.markup3.mboc.taskOffer
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.tasks.TaskType.YANG_MAPPING_MODERATION
import ru.yandex.market.markup3.testutils.CommonTaskTest
import ru.yandex.market.mboc.common.infrastructure.sql.TransactionHelper
import ru.yandex.market.mboc.http.SupplierOffer.SkuType.TYPE_PARTNER


class MappingModerationGeneratorTest : CommonTaskTest() {
    @Autowired
    lateinit var categoryInfoService: CategoryInfoService

    @Autowired
    private lateinit var offerTaskRepository: OfferTaskRepository

    @Autowired
    private lateinit var offerTaskService: OfferTaskService

    @Autowired
    private lateinit var mbocMock: MboCategoryServiceMock

    @Autowired
    private lateinit var markup3ApiService: Markup3ApiService

    lateinit var generator: MappingModerationGenerator

    @Before
    fun setUp() {
        mbocMock.taskOffersMap.clear()

        generator = MappingModerationGenerator(
            keyValueService,
            mbocMock,
            offerTaskService,
            TransactionHelper.MOCK,
            categoryInfoService,
            taskGroupRegistry,
            markup3ApiService
        )
    }

    @Test
    fun `Generates tasks for each category and skuType`() {
        mbocMock.addTaskOffers(
            taskOffer(20, 15, 20, priority = 100.0, ticketId = 0).setTargetSkuId(12)
        )
        generator.generate()
        val task1 = offerTaskRepository.findByKey(OfferTaskKey(YANG_MAPPING_MODERATION, YANG_GROUP_KEY, 20))
        task1!!.priority shouldBe convertMbocPriority(100.0).toDouble()


        // Expected tasks: [20], [21], [1,2, 3,4], [5], [6, 7]
        mbocMock.addTaskOffers(
            taskOffer(20, 15, 20, skuType = TYPE_PARTNER, priority = 100.0, ticketId = 0).setTargetSkuId(12),
            taskOffer(21, 15, 20, skuType = TYPE_PARTNER, priority = 120.0, ticketId = 0).setTargetSkuId(12),
            taskOffer(1, 15, 1, priority = 270.0, ticketId = 1).setTargetSkuId(12),
            taskOffer(2, 15, 1, priority = 280.0, ticketId = 1).setTargetSkuId(12),
            taskOffer(3, 25, 1, priority = 290.0, ticketId = 2).setTargetSkuId(12),
            taskOffer(4, 25, 1, priority = 300.0, ticketId = 2).setTargetSkuId(12),
            taskOffer(5, 15, 2, priority = 300.0, ticketId = 2).setTargetSkuId(12),
            taskOffer(10, 15, 2, skuType = TYPE_PARTNER, priority = 200.0, ticketId = 3).setTargetSkuId(12),
            taskOffer(11, 16, 2, skuType = TYPE_PARTNER, priority = 200.0, ticketId = 4).setTargetSkuId(12),
        )

        generator.generate()

        val all = offerTaskRepository.findAll()
        val byTask = all.groupBy { it.taskId }
            .mapValues { (_, offers) -> offers.map { it.offerId }.toSet() to offers.maxOf { it.priority } }
        byTask.values shouldContainExactlyInAnyOrder listOf(
            setOf(20L) to convertMbocPriority(100.0).toDouble(),
            setOf(21L) to convertMbocPriority(120.0).toDouble(),
            setOf(1L, 2L, 3L, 4L) to convertMbocPriority(300.0).toDouble(),
            setOf(5L) to convertMbocPriority(300.0).toDouble(),
            setOf(10L, 11L) to convertMbocPriority(200.0).toDouble(),
        )
        all shouldHaveSize 9
        all.all { it.status == OfferTaskStatus.WAITING_FOR_RESULTS } shouldBe true
    }

    @Test
    fun `Cancels tasks that have inactive offers`() {
        mbocMock.addTaskOffers(
            taskOffer(1, 15, 1, skuType = TYPE_PARTNER, priority = 101.0, ticketId = 1).setTargetSkuId(12),
            taskOffer(2, 15, 2, skuType = TYPE_PARTNER, priority = 100.0, ticketId = 2).setTargetSkuId(12),
        )

        generator.generate()
        offerTaskRepository.findAll().map { it.ticketId } shouldContainExactlyInAnyOrder listOf(1, 2)
        mbocMock.taskOffersMap.remove(1L)

        generator.generate()

        val tasks = offerTaskRepository.findAll()
        tasks shouldHaveSize 2
        tasks.filter { it.ticketId == 1L }.map { it.status }.first() shouldBe OfferTaskStatus.CANCELLING
    }

    @Test
    fun `Limits max offers per task`() {
        keyValueService.putValue(generator.maxOffersInTaskKey, 15)

        // 0 offerId assumes as empty (ru.yandex.market.markup3.grpc.services.api.converters.ValidationHelper#11)
        // it's ok?
        mbocMock.addTaskOffers((1..20L).map {
            taskOffer(it, 15, 1, priority = 99.0, ticketId = 1).setTargetSkuId(12)
        })

        generator.generate()

        val byTaskId = offerTaskRepository.findAll().groupBy { it.taskId }.mapValues { (_, v) -> v.map { it.offerId } }
        byTaskId shouldHaveSize 2
        val bigger = byTaskId.maxByOrNull { it.value.size } ?: error("unexpected")
        bigger.value shouldHaveSize 15
        bigger.value shouldContainExactlyInAnyOrder (1..15L).toList()
        val smaller = byTaskId.minByOrNull { it.value.size } ?: error("unexpected")
        smaller.value shouldHaveSize 5
        smaller.value shouldContainExactlyInAnyOrder (16..20L).toList()
    }

    @Test
    fun `Changes priority of tasks`() {
        mbocMock.addTaskOffers(
            taskOffer(2, 15, 2, skuType = TYPE_PARTNER, priority = 10000.0, ticketId = 1).setTargetSkuId(12),
            taskOffer(3, 15, 3, priority = 11000.0, ticketId = 2).setTargetSkuId(12),
        )

        generator.generate()
        val tasksBefore = offerTaskRepository.findAll().associateBy { it.ticketId }
        tasksBefore[1]!!.priority shouldBeLessThan tasksBefore[2]!!.priority
        val taskId = tasksBefore[1]!!.taskId
        taskEventDbService.findByTaskId(taskId).all { it.eventType != "ChangePriorityEvent" } shouldBe true


        mbocMock.addTaskOffers(
            taskOffer(2, 15, 2, skuType = TYPE_PARTNER, priority = 12000.0, ticketId = 1).setTargetSkuId(12),
        )

        generator.generate()
        val tasksAfter = offerTaskRepository.findAll().associateBy { it.ticketId }
        tasksAfter[1]!!.priority shouldBeGreaterThan tasksAfter[2]!!.priority
        taskEventDbService.findByTaskId(taskId).all { it.eventType != "ChangePriorityEvent" } shouldBe false
    }

    @After
    fun cleanup() {
        mbocMock.taskOffersMap.clear()
    }
}
