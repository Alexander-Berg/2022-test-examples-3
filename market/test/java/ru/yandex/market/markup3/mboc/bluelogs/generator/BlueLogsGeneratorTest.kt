package ru.yandex.market.markup3.mboc.bluelogs.generator

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
import ru.yandex.market.markup3.mboc.PriorityUtil
import ru.yandex.market.markup3.mboc.bluelogs.MbocBlueLogsConstants.GROUP_KEY
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskKey
import ru.yandex.market.markup3.mboc.offertask.dto.OfferTaskStatus
import ru.yandex.market.markup3.mboc.offertask.repository.OfferTaskRepository
import ru.yandex.market.markup3.mboc.taskOffer
import ru.yandex.market.markup3.mocks.MboCategoryServiceMock
import ru.yandex.market.markup3.tasks.TaskType.BLUE_LOGS
import ru.yandex.market.markup3.testutils.CommonTaskTest

class BlueLogsGeneratorTest : CommonTaskTest() {
    @Autowired
    private lateinit var generator: BlueLogsGenerator

    @Autowired
    private lateinit var mbocMock: MboCategoryServiceMock

    @Autowired
    private lateinit var offerTaskRepository: OfferTaskRepository

    @Before
    fun setup() {
        mbocMock.taskOffersMap.clear()
    }

    @Test
    fun `Generates tasks for each category-supplier pair`() {
        mbocMock.addTaskOffers(
            taskOffer(20, 15, 20, deadline = 100, ticketId = 0)
        )
        generator.generate()
        val ticket0BeforeTask = offerTaskRepository.findByKey(OfferTaskKey(BLUE_LOGS, GROUP_KEY, 20))
        ticket0BeforeTask!!.priority shouldBe PriorityUtil.calculatePriority(100, false)

        // Expected tasks: [20], [21], [1,2], [3,4], [5], [6, 7]
        mbocMock.addTaskOffers(
            taskOffer(20, 15, 20, deadline = 110, ticketId = 0),
            taskOffer(21, 15, 20, deadline = 110, ticketId = 0),
            taskOffer(1, 15, 1, deadline = 300, ticketId = 1),
            taskOffer(2, 15, 1, deadline = 300, ticketId = 1),
            taskOffer(3, 25, 1, deadline = 300, critical = true, ticketId = 1),
            taskOffer(4, 25, 1, deadline = 300, ticketId = 1),
            taskOffer(5, 15, 2, deadline = 300, ticketId = 1),
            taskOffer(10, 15, 10, deadline = 200, ticketId = 2),
            taskOffer(11, 15, 10, deadline = 200, ticketId = 2),
        )

        generator.generate()

        val all = offerTaskRepository.findAll()
        val byTask = all.groupBy { it.taskId }
            .mapValues { (_, offers) -> offers.map { it.offerId }.toSet() to offers.maxOf { it.priority } }
        byTask.values shouldContainExactlyInAnyOrder listOf(
            setOf(20L) to PriorityUtil.calculatePriority(110, false),
            setOf(21L) to PriorityUtil.calculatePriority(110, false),
            setOf(1L, 2L) to PriorityUtil.calculatePriority(300, false),
            setOf(3L, 4L) to PriorityUtil.calculatePriority(300, true),
            setOf(5L) to PriorityUtil.calculatePriority(300, false),
            setOf(10L, 11L) to PriorityUtil.calculatePriority(200, false),
        )
        all shouldHaveSize 9
        all.all { it.status == OfferTaskStatus.WAITING_FOR_RESULTS } shouldBe true
    }

    @Test
    fun `Cancels tasks that have inactive offers`() {
        mbocMock.addTaskOffers(
            taskOffer(1, 15, 1, deadline = 101, ticketId = 1),
            taskOffer(2, 15, 2, deadline = 100, ticketId = 2),
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

        mbocMock.addTaskOffers((0..19L).map {
            taskOffer(it, 15, 20, deadline = 99, ticketId = 1)
        })

        generator.generate()

        val byTaskId = offerTaskRepository.findAll().groupBy { it.taskId }.mapValues { (_, v) -> v.map { it.offerId } }
        byTaskId shouldHaveSize 2
        val bigger = byTaskId.maxByOrNull { it.value.size } ?: error("unexpected")
        bigger.value shouldHaveSize 15
        bigger.value shouldContainExactlyInAnyOrder (0..14L).toList()
        val smaller = byTaskId.minByOrNull { it.value.size } ?: error("unexpected")
        smaller.value shouldHaveSize 5
        smaller.value shouldContainExactlyInAnyOrder (15..19L).toList()
    }

    @Test
    fun `Changes priority of tasks`() {
        mbocMock.addTaskOffers(
            taskOffer(2, 15, 2, deadline = 101, ticketId = 1),
            taskOffer(3, 15, 3, deadline = 100, ticketId = 2),
        )

        generator.generate()
        val tasksBefore = offerTaskRepository.findAll().associateBy { it.ticketId }
        tasksBefore[1]!!.priority shouldBeLessThan tasksBefore[2]!!.priority
        val taskId = tasksBefore[1]!!.taskId
        taskEventDbService.findByTaskId(taskId).all { it.eventType != "ChangePriorityEvent" } shouldBe true

        mbocMock.addTaskOffers(
            taskOffer(2, 15, 2, deadline = 99, ticketId = 1)
        )

        generator.generate()
        val tasksAfter = offerTaskRepository.findAll().associateBy { it.ticketId }
        tasksAfter[1]!!.priority shouldBeGreaterThan tasksAfter[2]!!.priority
        taskEventDbService.findByTaskId(taskId).all { it.eventType != "ChangePriorityEvent" } shouldBe false
    }

    @After
    fun cleanup() {
        offerTaskRepository.deleteAll()
        mbocMock.taskOffersMap.clear()
    }
}
