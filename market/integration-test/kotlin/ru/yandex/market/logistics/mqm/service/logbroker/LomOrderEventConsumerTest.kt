package ru.yandex.market.logistics.mqm.service.logbroker

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.properties.LomOrderEventConsumerProperties
import ru.yandex.market.logistics.mqm.configuration.properties.ShootingProperties
import ru.yandex.market.logistics.mqm.entity.lom.AuthorDto
import ru.yandex.market.logistics.mqm.entity.lom.LomEventDto
import ru.yandex.market.logistics.mqm.entity.lom.enums.LomEntityType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.repository.LomOrderEventsRepository
import ru.yandex.market.logistics.mqm.utils.SHOOTING_UID
import ru.yandex.market.logistics.mqm.utils.queue.extractFileContent

@DisplayName("Проверка чтения событий из LOM")
class LomOrderEventConsumerTest : AbstractContextualTest() {
    @Autowired
    lateinit var lomOrderEventConsumer: LomOrderEventConsumer
    @Autowired
    lateinit var lomOrderEventsRepository: LomOrderEventsRepository
    @Autowired
    lateinit var objectMapper: ObjectMapper
    @Autowired
    lateinit var shootingProperties: ShootingProperties
    @Autowired
    lateinit var lomOrderEventConsumerProperties: LomOrderEventConsumerProperties

    @DisplayName("Успешная обработка события")
    @Test
    @ExpectedDatabase(
        value = "/service/logbroker/lom_order_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun lomEventParsing() {
        val diff = objectMapper.readTree(extractFileContent("service/logbroker/lom_order_event_diff.json"))
        val snapshot = getSnapshot()
        val event = getEvent(diff, snapshot)
        lomOrderEventConsumer.accept(listOf(event))
    }

    @DisplayName("Фэйковый заказ")
    @Test
    fun lomEventParsingFake() {
        val diff = objectMapper.readTree(extractFileContent("service/logbroker/lom_order_event_diff.json"))
        val snapshot = getSnapshot()
        (snapshot as ObjectNode).put("fake", true)
        val event = getEvent(diff, snapshot)
        lomOrderEventConsumer.accept(listOf(event))
        assertSoftly {
            lomOrderEventsRepository.findAll() shouldBe emptyList()
        }
    }

    @DisplayName("Заказ не обрабатывается, если это не BERU, DBS, FAAS")
    @ParameterizedTest
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["BERU", "DBS", "FAAS"]
    )
    fun lomEventParsingNotBeru(platformClient: PlatformClient) {
        val diff = objectMapper.readTree(extractFileContent("service/logbroker/lom_order_event_diff.json"))
        val snapshot = getSnapshot(platformClient)
        val event = getEvent(diff, snapshot)
        lomOrderEventConsumer.accept(listOf(event))
        assertSoftly {
            lomOrderEventsRepository.findAll() shouldBe emptyList()
        }
    }

    @DisplayName("Обработка YANDEX_GO заказа включена")
    @Test
    @ExpectedDatabase(
        value = "/service/logbroker/yandex_go_lom_order_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun lomEventParsingYandexGoEnabled() {
        try {
            lomOrderEventConsumerProperties.yandexGoPlatformClientEnabled = true
            val diff = objectMapper.readTree(extractFileContent("service/logbroker/lom_order_event_diff.json"))
            val snapshot = getSnapshot(PlatformClient.YANDEX_GO)
            val event = getEvent(diff, snapshot)
            lomOrderEventConsumer.accept(listOf(event))
        } finally {
            lomOrderEventConsumerProperties.yandexGoPlatformClientEnabled = false
        }
    }

    @DisplayName("Заказ со стрельб, обработка включена")
    @Test
    @ExpectedDatabase(
        value = "/service/logbroker/lom_order_event_shooting.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun lomEventParsingShootingEnabled() {
        val diff = objectMapper.readTree(extractFileContent("service/logbroker/lom_order_event_diff.json"))
        val snapshot = getSnapshot()
        (snapshot as ObjectNode).put("recipient", objectMapper.readTree("{\"uid\": \"$SHOOTING_UID\"}"))
        shootingProperties.processShootingOrders = true
        val event = getEvent(diff, snapshot)
        lomOrderEventConsumer.accept(listOf(event))
    }

    @DisplayName("Заказ со стрельб, обработка выключена")
    @Test
    fun lomEventParsingShootingDisabled() {
        val diff = objectMapper.readTree(extractFileContent("service/logbroker/lom_order_event_diff.json"))
        val snapshot = getSnapshot()
        (snapshot as ObjectNode).put("recipient", objectMapper.readTree("{\"uid\": \"$SHOOTING_UID\"}"))
        shootingProperties.processShootingOrders = false
        val event = getEvent(diff, snapshot)
        lomOrderEventConsumer.accept(listOf(event))
        assertSoftly {
            lomOrderEventsRepository.findAll() shouldBe emptyList()
        }
    }

    @DisplayName("Событие изменения change order request")
    @Test
    @ExpectedDatabase(
        value = "/service/logbroker/lom_order_cor_event.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun lomEventChangeOrderRequest() {
        val diff = objectMapper.readTree(extractFileContent("service/logbroker/lom_order_event_cor_diff.json"))
        val snapshot = getSnapshot()
        val event = getEvent(diff, snapshot)
        lomOrderEventConsumer.accept(listOf(event))
    }

    private fun getSnapshot(platformClient: PlatformClient): JsonNode {
        val snapshot = getSnapshot()
        (snapshot as ObjectNode).put("platformClientId", platformClient.id)
        return snapshot
    }

    private fun getSnapshot(): JsonNode {
        return objectMapper.readTree(extractFileContent("service/logbroker/lom_order_event_snapshot.json"))
    }

    private fun getEvent(diff: JsonNode, snapshot: JsonNode): LomEventDto {
        return LomEventDto(
            id = 123,
            logbrokerId = 456,
            entityType = LomEntityType.ORDER,
            entityId = 789,
            diff = diff,
            snapshot = snapshot,
            author = AuthorDto(id = 90),
            entityCreated = clock.instant(),
            created = clock.instant()
        )
    }
}
