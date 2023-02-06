package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.only
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.properties.StatisticsReportGrafanaProperties
import ru.yandex.market.logistics.mqm.configuration.properties.StatisticsReportStartrekProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.PlanFactGroup
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.additionaldata.ExpressNotificationAdditionalData
import ru.yandex.market.logistics.mqm.entity.additionaldata.PlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.DeliveryInterval
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.rules.payloads.ExpressCourierSearchNotificationsPayload
import ru.yandex.market.logistics.mqm.monitoringevent.base.producer.SendTelegramMessageEventProducer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.SendTelegramMessagePayload
import ru.yandex.market.logistics.mqm.repository.LomOrderRepository
import ru.yandex.market.logistics.mqm.repository.LomWaybillSegmentRepository
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.money.common.dbqueue.api.EnqueueParams


@ExtendWith(MockitoExtension::class)
class ExpressCourierSearchNotificationProcessorTest: AbstractContextualTest() {

    private val nowTime = Instant.parse("2021-09-08T18:00:00.00Z").atZone(DateTimeUtils.MOSCOW_ZONE).toInstant()

    companion object {
        private const val TEST_THRESHOLD = 10;
        private const val TEST_CHANNEL = "test_channel";
        private const val TEST_SENDER = "test_sender";
        private val TEST_RULE_SETTINGS = ExpressCourierSearchNotificationsPayload(
            threshold = TEST_THRESHOLD,
            notificationTGChannel = TEST_CHANNEL,
            notificationTGSender = TEST_SENDER,
            notificationRepeatInterval = Duration.ofMinutes(30),
            delayLevels = listOf(1, 2, 3, 4)
        );
        private val testRule = QualityRule(rule = TEST_RULE_SETTINGS)
        private val TEST_GRAFANA_URL = "test_grafana"
        private val TEST_STARTREK_URL = "test_startrek"
        private val TEST_ORDER = LomOrder(
            id = 1,
            platformClientId = PlatformClient.BERU.id,
            status = OrderStatus.ENQUEUED,
            deliveryInterval = DeliveryInterval(
                deliveryDateMin = LocalDate.of(2021, 9, 10),
                deliveryDateMax = LocalDate.of(2021, 9, 11),
            ),
            deliveryType = DeliveryType.MOVEMENT,
        )
    }

    @Mock
    private lateinit var notificationsProducer: SendTelegramMessageEventProducer

    @Autowired
    private lateinit var planFactService: PlanFactService

    @Autowired
    private lateinit var orderRepository: LomOrderRepository

    @Autowired
    private lateinit var lomWaybillSegmentRepository: LomWaybillSegmentRepository

    private lateinit var processor: ExpressCourierSearchNotificationProcessor

    @BeforeEach
    fun setUp() {
        clock.setFixed(nowTime, DateTimeUtils.MOSCOW_ZONE)
        processor = ExpressCourierSearchNotificationProcessor(
            clock = clock,
            notificationsProducer = notificationsProducer,
            grafanaProperties = StatisticsReportGrafanaProperties(
                expressCallCourier = TEST_GRAFANA_URL,
                ffAssemblyUrl = "",
                dropshipShipmentUrl = "",
                scShipmentUrl = "",
                scDsIntakeUrl = "",
                mcIntakeUrl = "",
                ffShipmentUrl = "",
                ffDsIntakeUrl = "",
            ),
            startrekProperties = StatisticsReportStartrekProperties(expressCallCourier = TEST_STARTREK_URL),
            planFactService = planFactService,
        )
        orderRepository.save(TEST_ORDER)
    }

    @AfterEach
    fun verify() {
        verifyNoMoreInteractions(notificationsProducer)
    }

    @Test
    @DisplayName("Процессор ничего не делает, если нет payload")
    fun ignoreIfNoPayload() {
        assertSoftly {
            processor.process(QualityRule(), PlanFactGroup()) shouldBe
                nowTime.plus(ExpressCourierSearchNotificationProcessor.UPDATE_INTERVAL)
        }
    }

    @Test
    @DisplayName("При первом запуске лимит не превышен, поэтому сообщения не отправляется")
    fun firstRunDoNotSendMessageIfBelowLimit() {
        assertSoftly {
            processor.process(testRule, setupGroup(TEST_THRESHOLD - 1)) shouldBe
                nowTime.plus(ExpressCourierSearchNotificationProcessor.UPDATE_INTERVAL)
        }
    }

    @Test
    @DisplayName("При первом запуске лимит достигнут, поэтому отправляется одно сообщение")
    fun firstRunSendMessageIfAboveLimit() {
        val testCount = TEST_THRESHOLD
        val testExpectedMessage = setupTelegramMessage(
            message = "*Экспресс - поиск курьера*\n" +
                "❗❗❗Задержка поиска курьера *10* заказов❗❗❗\n" +
                "Из них не назначен курьер:\n" +
                "более 5 минут - 4 шт\n" +
                "[Графана](test_grafana)\n" +
                "[Cтартрек](test_startrek)"
        )
        whenever(notificationsProducer.enqueue(testExpectedMessage)).thenReturn(1L)

        assertSoftly {
            processor.process(testRule, setupGroup(testCount)) shouldBe
                nowTime.plus(ExpressCourierSearchNotificationProcessor.UPDATE_INTERVAL)
        }

        verify(notificationsProducer, only()).enqueue(testExpectedMessage)
    }

    @Test
    @DisplayName("При первом запуске лимит достигнут, поэтому отправляется одно сообщение, но без детальной информации")
    fun firstRunSendMessageIfAboveLimitWithoutDetails() {
        val testCount = TEST_THRESHOLD
        val testExpectedMessage = setupTelegramMessage(
            message = "*Экспресс - поиск курьера*\n" +
                "❗❗❗Задержка поиска курьера *10* заказов❗❗❗\n" +
                "[Графана](test_grafana)\n" +
                "[Cтартрек](test_startrek)"
        )
        whenever(notificationsProducer.enqueue(testExpectedMessage)).thenReturn(1L)

        val testGroup = setupGroup(testCount, constantDelay = Duration.ofMinutes(4))
        assertSoftly {
            processor.process(testRule, testGroup) shouldBe
                nowTime.plus(ExpressCourierSearchNotificationProcessor.UPDATE_INTERVAL)
        }

        verify(notificationsProducer, only()).enqueue(testExpectedMessage)
    }

    @Test
    @DisplayName("При очередном запуске лимит не превышен, поэтому сообщение не отправляется")
    fun nextRunSendMessageIfContinueBelowLimit() {
        val testGroup = setupGroup(
            planFacts = TEST_THRESHOLD - 1,
            data = ExpressNotificationAdditionalData(
                lastNotificationTime = nowTime.minusSeconds(1),
                lastCheckValue = TEST_THRESHOLD - 2,
            )
        )

        assertSoftly {
            processor.process(testRule, testGroup) shouldBe
                nowTime.plus(ExpressCourierSearchNotificationProcessor.UPDATE_INTERVAL)
        }
    }

    @Test
    @DisplayName("При очередном запуске лимит оказался превышен, поэтому отправляется одно сообщение")
    fun nextRunSendMessageIfChangedToAboveLimit() {
        val testNewCount = TEST_THRESHOLD + 1
        val testExpectedMessage = setupTelegramMessage(
            message = "*Экспресс - поиск курьера*\n" +
                "❗❗❗Задержка поиска курьера *11* заказов❗❗❗\n" +
                "Из них не назначен курьер:\n" +
                "более 5 минут - 5 шт\n" +
                "[Графана](test_grafana)\n" +
                "[Cтартрек](test_startrek)"
        )
        whenever(notificationsProducer.enqueue(testExpectedMessage)).thenReturn(1L)
        val testGroup = setupGroup(
            planFacts = testNewCount,
            data = ExpressNotificationAdditionalData(
                lastNotificationTime = nowTime.minusSeconds(1),
                lastCheckValue = TEST_THRESHOLD - 1,
            )
        )

        assertSoftly {
            processor.process(testRule, testGroup) shouldBe
                nowTime.plus(ExpressCourierSearchNotificationProcessor.UPDATE_INTERVAL)
        }

        verify(notificationsProducer, only()).enqueue(testExpectedMessage)
    }

    @Test
    @DisplayName(
        "При очередном запуске лимит продолжает быть превышен, но сообщение не отправляется, т.к недавно уже было одно"
    )
    fun nextRunDoNotSendMessageIfTooEarlyToNotifyAgain() {
        val testGroup = setupGroup(
            planFacts = TEST_THRESHOLD + 2,
            data = ExpressNotificationAdditionalData(
                lastNotificationTime = nowTime.minus(TEST_RULE_SETTINGS.notificationRepeatInterval).plusSeconds(1),
                lastCheckValue = TEST_THRESHOLD + 1,
            )
        )

        assertSoftly {
            processor.process(testRule, testGroup) shouldBe
                nowTime.plus(ExpressCourierSearchNotificationProcessor.UPDATE_INTERVAL)
        }
    }

    @Test
    @DisplayName(
        "При очередном запуске лимит продолжает быть превышен, поэтому отправляется одно сообщение"
    )
    fun nextRunSendMessageIfContinueAboveLimit() {
        val testNewCount = TEST_THRESHOLD + 2
        val testExpectedMessage = setupTelegramMessage(
            message = "*Экспресс - поиск курьера*\n" +
                "❗❗❗Задержка поиска курьера *12* заказов❗❗❗\n" +
                "Из них не назначен курьер:\n" +
                "более 10 минут - 1 шт\n" +
                "более 5 минут - 6 шт\n" +
                "[Графана](test_grafana)\n" +
                "[Cтартрек](test_startrek)"
        )
        whenever(notificationsProducer.enqueue(testExpectedMessage)).thenReturn(1L)
        val testGroup = setupGroup(
            planFacts = testNewCount,
            data = ExpressNotificationAdditionalData(
                lastNotificationTime = nowTime.minus(TEST_RULE_SETTINGS.notificationRepeatInterval).minusSeconds(1),
                lastCheckValue = TEST_THRESHOLD + 1,
            )
        )

        assertSoftly {
            processor.process(testRule, testGroup) shouldBe
                nowTime.plus(ExpressCourierSearchNotificationProcessor.UPDATE_INTERVAL)
        }

        verify(notificationsProducer, times(1)).enqueue(testExpectedMessage)
    }

    @Test
    @DisplayName(
        "При очередном запуске лимит оказался не превышен, поэтому отправляется одно сообщение про неактуальность проблемы"
    )
    fun nextRunSendMessageIfChangedToBelowLimit() {
        val testNewCount = TEST_THRESHOLD - 1
        val testExpectedMessage = setupTelegramMessage(
            message = "*Экспресс - поиск курьера*\n" +
                "Массовая проблема ушла, задержка поиска *9* заказов"
        )
        whenever(notificationsProducer.enqueue(testExpectedMessage)).thenReturn(1L)

        val testGroup = setupGroup(
            planFacts = testNewCount,
            data = ExpressNotificationAdditionalData(
                lastNotificationTime = nowTime.minusSeconds(1),
                lastCheckValue = TEST_THRESHOLD + 1,
            )
        )

        assertSoftly {
            processor.process(testRule, testGroup) shouldBe
                nowTime.plus(ExpressCourierSearchNotificationProcessor.UPDATE_INTERVAL)
        }

        verify(notificationsProducer, times(1)).enqueue(testExpectedMessage)
    }

    private fun setupTelegramMessage(
        channel: String = TEST_CHANNEL,
        message: String,
        sender: String = TEST_SENDER,
    ) = EnqueueParams.create(
        SendTelegramMessagePayload(
            channel = channel,
            message = message,
            sender = sender,
        )
    )

    private fun setupGroup(
        planFacts: Int,
        data: PlanFactAdditionalData? = null,
        constantDelay: Duration? = null,
    ): PlanFactGroup {
        val testGroup = PlanFactGroup()
        testGroup.planFacts = mutableSetOf()
        repeat(planFacts) { id ->
            val delay = constantDelay ?: Duration.ofMinutes(id.toLong())
            val segment = generateWaybill(
                id = id.toLong(),
                callCourierTime = clock.instant().minus(delay),
            )
            testGroup.planFacts.add(
                PlanFact(
                    id = id.toLong(),
                    entityId = segment.id
                )
            )
        }
        if (data != null) {
            testGroup.setData(data)
        }
        return testGroup
    }

    private fun generateWaybill(
        id: Long,
        callCourierTime: Instant,
    ): WaybillSegment = lomWaybillSegmentRepository.save(WaybillSegment(
        id = id,
        segmentType = SegmentType.MOVEMENT,
        partnerId = 1,
        callCourierTime = callCourierTime,
    ).apply { order = TEST_ORDER })
}
