package ru.yandex.market.logistics.mqm.service

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.repository.LomOrderRepository
import ru.yandex.market.logistics.mqm.repository.PlanFactRepository

class PlanFactAnalyticsServiceImplTest : AbstractContextualTest() {

    @Autowired
    lateinit var transactionTemplate: TransactionOperations

    @Autowired
    lateinit var planFactRepository: PlanFactRepository

    @Autowired
    lateinit var planFactService: PlanFactService

    @Autowired
    lateinit var orderRepository: LomOrderRepository

    @Autowired
    lateinit var planFactAnalyticsService: PlanFactAnalyticsService

    @Test
    @DisplayName("Создание аналитических записей для заказа")
    @DatabaseSetup("/service/plan_fact_analytics_service/setup.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_service/analytics_prepared.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun create() {
        transactionTemplate.execute {
            planFactAnalyticsService.createAnalyticsForAllSegments(orderRepository.findById(1L).get())
        }
    }

    @Test
    @DisplayName("Проверка, что повторный вызов сохранения ни к чему не приводит")
    @DatabaseSetup(
        *[
            "/service/plan_fact_analytics_service/setup.xml",
            "/service/plan_fact_analytics_service/analytics_prepared.xml",
        ]
    )
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_service/analytics_prepared.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createDoNothingIfExists() {
        transactionTemplate.execute {
            planFactAnalyticsService.createAnalyticsForAllSegments(orderRepository.findById(1L).get())
        }
    }


    @Test
    @DisplayName("Обновление аналитической записи по данным план-факта TRACK_RECEIVED с заполнением initial")
    @DatabaseSetup(
        *[
            "/service/plan_fact_analytics_service/setup.xml",
            "/service/plan_fact_analytics_service/plan_fact_track_received.xml",
            "/service/plan_fact_analytics_service/analytics_track_received_just_created.xml",
        ]
    )
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_service/analytics_track_received_updated_plans.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateFreshTrackReceivedAnalytics() {
        transactionTemplate.execute {
            val testPlanFact = planFactRepository.findById(1L).get()
            planFactService.enrichEntities(listOf(testPlanFact))
            planFactAnalyticsService.update(testPlanFact)
        }
    }

    @Test
    @DisplayName("Обновление аналитической записи по данным план-факта TRACK_RECEIVED без обновления initial")
    @DatabaseSetup(
        *[
            "/service/plan_fact_analytics_service/setup.xml",
            "/service/plan_fact_analytics_service/plan_fact_track_received_updated_plan.xml",
            "/service/plan_fact_analytics_service/analytics_track_received_updated_plans.xml"
        ]
    )
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_service/analytics_track_received_new_last_plan.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateTrackReceivedAnalyticsWithInitial() {
        transactionTemplate.execute {
            val testPlanFact = planFactRepository.findById(1L).get()
            planFactService.enrichEntities(listOf(testPlanFact))
            planFactAnalyticsService.update(testPlanFact)
        }
    }

    @Test
    @DisplayName("Cоздание аналитической записи для нового статуса")
    @DatabaseSetup(
        *[
            "/service/plan_fact_analytics_service/setup.xml",
            "/service/plan_fact_analytics_service/plan_fact_transit_prepared.xml",
        ]
    )
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_service/analytics_transit_prepared.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateAddForNewStatus() {
        transactionTemplate.execute {
            val testPlanFact = planFactRepository.findById(1L).get()
            planFactService.enrichEntities(listOf(testPlanFact))
            planFactAnalyticsService.update(testPlanFact)
        }
    }

    @Test
    @DisplayName("Обновление аналитической записи по данным план-факта не TRACK_RECEIVED без обновления initial")
    @DatabaseSetup(
        *[
            "/service/plan_fact_analytics_service/setup.xml",
            "/service/plan_fact_analytics_service/plan_fact_intake.xml",
            "/service/plan_fact_analytics_service/analytics_intake.xml"
        ]
    )
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_service/analytics_intake_updated_last_plan.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateFreshNotTrackReceivedAnalytics() {
        transactionTemplate.execute {
            val testPlanFact = planFactRepository.findById(1L).get()
            planFactService.enrichEntities(listOf(testPlanFact))
            planFactAnalyticsService.update(testPlanFact)
        }
    }

    @Test
    @DisplayName("Обновление факта в аналитической записи")
    @DatabaseSetup(
        *[
            "/service/plan_fact_analytics_service/setup.xml",
            "/service/plan_fact_analytics_service/plan_fact_with_fact.xml",
            "/service/plan_fact_analytics_service/analytics_intake.xml"
        ]
    )
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_service/analytics_with_fact.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateFactDatetime() {
        transactionTemplate.execute {
            val testPlanFact = planFactRepository.findById(1L).get()
            planFactService.enrichEntities(listOf(testPlanFact))
            planFactAnalyticsService.update(testPlanFact)
        }
    }

    @Test
    @DisplayName("Проверка, что план-факты неподдерживаемых сегментов игнорируется и ничего не создается")
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_service/empty_analytics.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun ignoreFakeSegment() {
        transactionTemplate.execute {
            EntityType.values()
                .filterNot(EntityType.LOM_WAYBILL_SEGMENT::equals)
                .forEach { entityType -> planFactAnalyticsService.update(mockPlanFact(entityType = entityType)) }

            PlanFactStatus.values()
                .filterNot(PlanFactAnalyticsServiceImpl.STATUSES_TO_UPDATE_ANALYTICS::contains)
                .forEach { status -> planFactAnalyticsService.update(mockPlanFact(status = status)) }
        }
    }

    @Test
    @DisplayName("Игнорирование план-фактов, поддержка которых отключена")
    @DatabaseSetup(
        *[
            "/service/plan_fact_analytics_service/setup.xml",
            "/service/plan_fact_analytics_service/plan_fact_intake_ignored_producer.xml",
            "/service/plan_fact_analytics_service/analytics_intake.xml"
        ]
    )
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_service/analytics_intake.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateIgnoreAutoCancelAfterDropshipPlanFactProcessor() {
        transactionTemplate.execute {
            val testPlanFact = planFactRepository.findById(1L).get()
            planFactService.enrichEntities(listOf(testPlanFact))
            planFactAnalyticsService.update(testPlanFact)
        }
    }

    @Test
    @DisplayName("Создание аналитических записей для заказа с разными типами API для сегментов партнёра DELIVERY")
    @DatabaseSetup("/service/plan_fact_analytics_service/setup.xml")
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_service/analytics_prepared_delivery_segments.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createDeliverySegments() {
        transactionTemplate.execute {
            planFactAnalyticsService.createAnalyticsForAllSegments(orderRepository.findById(2L).get())
        }
    }

    @Test
    @DisplayName("Обновление поля с тикетом")
    @DatabaseSetup(
        "/service/plan_fact_analytics_service/setup.xml",
        "/service/plan_fact_analytics_service/two_plan_facts_with_fact.xml",
        "/service/plan_fact_analytics_service/two_analytics_intake.xml"
    )
    @ExpectedDatabase(
        value = "/service/plan_fact_analytics_service/analytics_intake_with_ticket.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun updateTicketIssue() {
        val testIssue = "TEST-134"
        transactionTemplate.execute {
            val testPlanFact = planFactRepository.findAll()
            planFactAnalyticsService.updateIssueKey(testIssue, testPlanFact)
        }
    }

    fun mockPlanFact(
        entityType: EntityType = EntityType.LOM_WAYBILL_SEGMENT,
        status: PlanFactStatus = PlanFactStatus.ACTIVE,
    ) = PlanFact(
        entityType = entityType,
        planFactStatus = status,
    )
}
