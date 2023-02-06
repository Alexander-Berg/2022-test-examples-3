package ru.yandex.market.logistics.mqm.service.monitoringevent

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.monitoringevent.EventHandlerType
import ru.yandex.market.logistics.mqm.service.monitoringevent.handlers.DynamicEventHandler
import java.util.EnumSet

internal class DynamicEventServiceImplTest: AbstractContextualTest() {

    @Autowired
    private lateinit var dynamicEventService: DynamicEventService

    @Autowired
    private lateinit var eventHandlers: List<DynamicEventHandler>

    @Test
    @DisplayName("Проверка что у каждого EventHandlerType есть бин хендлера, обрабатывающего этот тип")
    fun everyHandlerTypeHasHandlerBean() {
        val typesOfHandlers = eventHandlers.map { it.getEventHandlerType() }.toSet()
        val allHandlerTypes = EnumSet.allOf(EventHandlerType::class.java)
        typesOfHandlers shouldBe allHandlerTypes
    }

    @Test
    @DisplayName("Создание задачи на отправку сообщения в телеграмм")
    @DatabaseSetup("/service/monitoringevent/dynamic_event_service/before/telegram.xml")
    @ExpectedDatabase(
        value = "/service/monitoringevent/dynamic_event_service/after/telegram.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createTaskForSendingTelegramMessage() {
        val data = mapOf(
            "orderId" to "1234",
            "date" to "01-01-2021"
        )
        dynamicEventService.routeEvent("testEventCode", data)
    }

    @Test
    @DisplayName("Создание задачи на создания тикета в startrek")
    @DatabaseSetup("/service/monitoringevent/dynamic_event_service/before/startrek.xml")
    @ExpectedDatabase(
        value = "/service/monitoringevent/dynamic_event_service/after/startrek.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun createTaskForCreatingStartrekIssue() {
        val data = mapOf(
            "orderId" to "1234",
            "date" to "01-01-2021",
            "barcode" to "12345"
        )
        dynamicEventService.routeEvent("testEventCode", data)
    }
}
