package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.PlanFactGroup
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.additionaldata.ExpressAssembleNotificationAdditionalData
import ru.yandex.market.logistics.mqm.entity.additionaldata.NotificationState
import ru.yandex.market.logistics.mqm.entity.additionaldata.PlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.rules.payloads.CabinetInfo
import ru.yandex.market.logistics.mqm.entity.rules.payloads.ChannelNotificationSettings
import ru.yandex.market.logistics.mqm.entity.rules.payloads.ExpressAssembleNotificationsPayload
import ru.yandex.market.logistics.mqm.entity.rules.payloads.Range
import ru.yandex.market.logistics.mqm.monitoringevent.base.producer.SendTelegramMessageEventProducer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.SendTelegramMessagePayload
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.ExpressAssembleNotificationProcessor.Companion.DEFAULT_UPDATE_INTERVAL
import ru.yandex.money.common.dbqueue.api.EnqueueParams

@ExtendWith(MockitoExtension::class)
class ExpressAssembleNotificationProcessorTest {

    @Mock
    private lateinit var notificationsProducer: SendTelegramMessageEventProducer

    @Mock
    private lateinit var planFactService: PlanFactService

    lateinit var clock: Clock

    lateinit var processor: ExpressAssembleNotificationProcessor

    @BeforeEach
    fun setUp() {
        clock = Clock.fixed(NOW_TIME, DateTimeUtils.MOSCOW_ZONE)
        processor = setupProcessor()
    }

    @Test
    @DisplayName("Процессор ничего не делает, если нет payload")
    fun ignoreIfNoPayload() {
        val testGroup = setupGroup()

        processor.process(QualityRule(), testGroup) shouldBe NOW_TIME.plus(DEFAULT_UPDATE_INTERVAL)

        verifyZeroInteractions(notificationsProducer)
        verifyZeroInteractions(planFactService)
        getProcessorState(testGroup) shouldBe null
    }

    @Test
    @DisplayName("Процессор ничего не делает, если нотификации выключены")
    fun ignoreIfDisabled() {
        val testGroup = setupGroup(planFacts = TEST_LIMIT)

        processor.process(mockRule(enabled = false), testGroup) shouldBe NOW_TIME.plus(TEST_NOTIFICATION_INTERVAL)

        verifyZeroInteractions(notificationsProducer)
        getProcessorState(testGroup) shouldBe ExpressAssembleNotificationAdditionalData()
    }

    @Test
    @DisplayName("Присылаем сообщение, если превысили лимит")
    fun sendMessageIfAboveLimit() {
        val testGroup = setupGroup(planFacts = TEST_LIMIT)
        val testExpectedMessage = setupTelegramMessage(
            message = "Скопилось большое количество заказов с задержкой в сборке $TEST_LIMIT/$TEST_LIMIT.\n" +
                TEST_COMMON_PARTNER_NAME
        )
        doNothing().whenever(planFactService).enrichEntities(testGroup.planFacts)

        processor.process(mockRule(), testGroup) shouldBe NOW_TIME.plus(TEST_NOTIFICATION_INTERVAL)

        verify(notificationsProducer).enqueue(testExpectedMessage)
        verify(planFactService).enrichEntities(testGroup.planFacts)
        getProcessorState(testGroup) shouldBe additionalData(
            lastNotificationTime = NOW_TIME,
            lastCheckValue = TEST_LIMIT,
        )
    }

    @Test
    @DisplayName("Не присылаем сообщение, если количество заказов продолжает превышать лимит")
    fun doNotSendMessageIfContinueAboveLimit() {
        val testCurrentData = additionalData(
            lastNotificationTime = NOW_TIME.minus(TEST_NOTIFICATION_INTERVAL).minusSeconds(1),
            lastCheckValue = TEST_LIMIT,
        )
        val testGroup = setupGroup(
            planFacts = TEST_LIMIT,
            data = testCurrentData
        )

        processor.process(mockRule(), testGroup) shouldBe NOW_TIME.plus(TEST_NOTIFICATION_INTERVAL)

        verifyZeroInteractions(notificationsProducer)
        verify(planFactService).enrichEntities(testGroup.planFacts)
        getProcessorState(testGroup) shouldBe testCurrentData
    }

    @Test
    @DisplayName("Присылаем сообщение, если превысили порог и изменилось количество заказов")
    fun sendMessageIfAboveThresholdAndCountChanged() {
        val testCurrentData = additionalData(
            lastNotificationTime = NOW_TIME.minus(TEST_NOTIFICATION_INTERVAL).minusSeconds(1),
            lastCheckValue = TEST_THRESHOLD,
        )
        val testNewDeadlinesCount = TEST_THRESHOLD + 1
        val testGroup = setupGroup(
            planFacts = testNewDeadlinesCount,
            data = testCurrentData
        )
        val testExpectedMessage = setupTelegramMessage(
            message = "*Экспресс - cборка заказов*\n" +
                "\u2757\u2757\u2757Задержка сборки: *$testNewDeadlinesCount* заказов\u2757\u2757\u2757\n" +
                "$TEST_COMMON_PARTNER_NAME\n" +
                "\n" +
                "$TEST_PARTNER_CABINET_NAME(id: $TEST_PARTNER_CABINET_ID) - *11* заказов\n" +
                "$TEST_PARTNER_NAME_SAFE - *$testNewDeadlinesCount*\n" +
                "\t[5 - 10] минут - *11*(0, 1)"
        )

        processor.process(mockRule(), testGroup) shouldBe NOW_TIME.plus(TEST_NOTIFICATION_INTERVAL)

        verify(notificationsProducer).enqueue(testExpectedMessage)
        verify(planFactService).enrichEntities(testGroup.planFacts)
        getProcessorState(testGroup) shouldBe additionalData(
            lastNotificationTime = NOW_TIME,
            lastCheckValue = testNewDeadlinesCount,
        )
    }

    @Test
    @DisplayName("Присылаем сообщение c двумя кабинетами")
    fun sendMessageWithTwoCabinets() {
        val testCurrentData = additionalData(
            lastNotificationTime = NOW_TIME.minus(TEST_NOTIFICATION_INTERVAL).minusSeconds(1),
            lastCheckValue = TEST_THRESHOLD,
        )
        var testNewDeadlinesCount = TEST_THRESHOLD + 1
        val testGroup = setupGroup(
            planFacts = testNewDeadlinesCount,
            data = testCurrentData
        )

        testGroup.planFacts.add(
            generatePlanFact(
                id = 1111,
                partnerId = TEST_ANOTHER_PARTNER_ID,
                partnerName = TEST_ANOTHER_PARTNER_NAME
            )
        )
        testNewDeadlinesCount = testNewDeadlinesCount.inc()

        val testExpectedMessage = setupTelegramMessage(
            message = "*Экспресс - cборка заказов*\n" +
                "\u2757\u2757\u2757Задержка сборки: *$testNewDeadlinesCount* заказов\u2757\u2757\u2757\n" +
                "$TEST_COMMON_PARTNER_NAME\n" +
                "\n" +
                "$TEST_PARTNER_CABINET_NAME(id: $TEST_PARTNER_CABINET_ID) - *11* заказов\n" +
                "$TEST_PARTNER_NAME_SAFE - *11*\n" +
                "\t[5 - 10] минут - *11*(0, 1)\n" +
                "\n" +
                "$TEST_ANOTHER_PARTNER_CABINET_NAME(id: $TEST_ANOTHER_PARTNER_CABINET_ID) - *1* заказ\n" +
                "$TEST_ANOTHER_PARTNER_NAME_SAFE - *1*\n" +
                "\t[5 - 10] минут - *1*(1111)"
        )

        processor.process(mockRuleWithTwoCabinets(), testGroup) shouldBe NOW_TIME.plus(TEST_NOTIFICATION_INTERVAL)

        verify(notificationsProducer).enqueue(testExpectedMessage)
        verify(planFactService).enrichEntities(testGroup.planFacts)
        getProcessorState(testGroup) shouldBe additionalData(
            lastNotificationTime = NOW_TIME,
            lastCheckValue = testNewDeadlinesCount,
        )
    }

    @Test
    @DisplayName("Присылаем сообщение c двумя кабинетами")
    fun sendMessageWithTwoChannels() {
        val testCurrentData = additionalData(
            lastNotificationTime = NOW_TIME.minus(TEST_NOTIFICATION_INTERVAL).minusSeconds(1),
            lastCheckValue = TEST_THRESHOLD,
        )
        val testDeadlinesForBrand = TEST_THRESHOLD + 1
        val testDeadlinesForAnotherBrand = TEST_THRESHOLD + 2
        val testGroup = setupGroup(
            planFacts = testDeadlinesForBrand,
            data = testCurrentData
        ).apply {
            repeat(testDeadlinesForAnotherBrand) { id ->
                planFacts.add(
                    generatePlanFact(
                        partnerId = TEST_ANOTHER_PARTNER_ID,
                        partnerName = TEST_ANOTHER_PARTNER_NAME,
                        id = testDeadlinesForBrand + id.toLong(),
                    )
                )
            }
        }
        val testExpectedMessageChannel = setupTelegramMessage(
            message = "*Экспресс - cборка заказов*\n" +
                "\u2757\u2757\u2757Задержка сборки: *11* заказов\u2757\u2757\u2757\n" +
                "$TEST_COMMON_PARTNER_NAME\n" +
                "\n" +
                "$TEST_PARTNER_CABINET_NAME(id: $TEST_PARTNER_CABINET_ID) - *11* заказов\n" +
                "$TEST_PARTNER_NAME_SAFE - *11*\n" +
                "\t[5 - 10] минут - *11*(0, 1)"
        )
        val testExpectedMessageAnotherChannel = setupTelegramMessage(
            channel = TEST_ANOTHER_TELEGRAM_CHANNEL,
            message = "*Экспресс - cборка заказов*\n" +
                "\u2757\u2757\u2757Задержка сборки: *12* заказов\u2757\u2757\u2757\n" +
                "$TEST_COMMON_ANOTHER_PARTNER_NAME\n" +
                "\n" +
                "$TEST_ANOTHER_PARTNER_CABINET_NAME(id: $TEST_ANOTHER_PARTNER_CABINET_ID) - *12* заказов\n" +
                "$TEST_ANOTHER_PARTNER_NAME_SAFE - *12*\n" +
                "\t[5 - 10] минут - *12*(11, 12)"
        )

        processor.process(mockRuleWithTwoChannels(), testGroup) shouldBe NOW_TIME.plus(TEST_NOTIFICATION_INTERVAL)

        verify(notificationsProducer).enqueue(testExpectedMessageChannel)
        verify(notificationsProducer).enqueue(testExpectedMessageAnotherChannel)
        verify(planFactService).enrichEntities(testGroup.planFacts)
        getProcessorState(testGroup) shouldBe ExpressAssembleNotificationAdditionalData(
            channelsState = mutableMapOf(
                NOTIFICATION_CHANNEL_1 to NotificationState(
                    lastNotificationTime = NOW_TIME,
                    lastCheckValue = testDeadlinesForBrand,
                ),
                NOTIFICATION_CHANNEL_2 to NotificationState(
                    lastNotificationTime = NOW_TIME,
                    lastCheckValue = testDeadlinesForAnotherBrand,
                )
            )
        )
    }

    @Test
    @DisplayName("Присылаем сообщение по одному кабнету, т.к. по второму нет заказов")
    fun sendMessageWithTwoCabinetsButOrdersOnlyForOne() {
        val testCurrentData = additionalData(
            lastNotificationTime = NOW_TIME.minus(TEST_NOTIFICATION_INTERVAL).minusSeconds(1),
            lastCheckValue = TEST_THRESHOLD,
        )
        val testNewDeadlinesCount = TEST_THRESHOLD + 1
        val testGroup = setupGroup(
            planFacts = testNewDeadlinesCount,
            data = testCurrentData
        )

        val testExpectedMessage = setupTelegramMessage(
            message = "*Экспресс - cборка заказов*\n" +
                "\u2757\u2757\u2757Задержка сборки: *$testNewDeadlinesCount* заказов\u2757\u2757\u2757\n" +
                "$TEST_COMMON_PARTNER_NAME\n" +
                "\n" +
                "$TEST_PARTNER_CABINET_NAME(id: $TEST_PARTNER_CABINET_ID) - *11* заказов\n" +
                "$TEST_PARTNER_NAME_SAFE - *11*\n" +
                "\t[5 - 10] минут - *11*(0, 1)"
        )

        processor.process(mockRuleWithTwoCabinets(), testGroup) shouldBe NOW_TIME.plus(TEST_NOTIFICATION_INTERVAL)

        verify(notificationsProducer).enqueue(testExpectedMessage)
        verify(planFactService).enrichEntities(testGroup.planFacts)
        getProcessorState(testGroup) shouldBe additionalData(
            lastNotificationTime = NOW_TIME,
            lastCheckValue = testNewDeadlinesCount,
        )
    }

    @Test
    @DisplayName("Не присылаем сообщение, если превысили порог, но не изменилось количество заказов")
    fun doNotSendMessageIfAboveThresholdAndCountNotChanged() {
        val currentPlanFactsCount = TEST_THRESHOLD
        val testCurrentData = additionalData(
            lastNotificationTime = NOW_TIME.minus(TEST_NOTIFICATION_INTERVAL).minusSeconds(1),
            lastCheckValue = currentPlanFactsCount,
        )
        val testGroup = setupGroup(
            planFacts = currentPlanFactsCount,
            data = testCurrentData
        )

        processor.process(mockRule(), testGroup) shouldBe NOW_TIME.plus(TEST_NOTIFICATION_INTERVAL)

        verifyZeroInteractions(notificationsProducer)
        verify(planFactService).enrichEntities(testGroup.planFacts)
        getProcessorState(testGroup) shouldBe testCurrentData
    }

    @Test
    @DisplayName("Присылаем сообщение, если количество заказов упало ниже порога")
    fun sendMessageIfBelowThreshold() {
        val testCurrentData = additionalData(
            lastNotificationTime = NOW_TIME.minus(TEST_NOTIFICATION_INTERVAL).minusSeconds(1),
            lastCheckValue = TEST_THRESHOLD,
        )
        val testNewDeadlinesCount = TEST_THRESHOLD - 1
        val testGroup = setupGroup(
            planFacts = testNewDeadlinesCount,
            data = testCurrentData,
        )
        val testExpectedMessage = setupTelegramMessage(
            message = "Массовая проблема задержки сборки заказов ушла: $testNewDeadlinesCount заказов.\n" +
                TEST_COMMON_PARTNER_NAME
        )

        processor.process(mockRule(), testGroup) shouldBe NOW_TIME.plus(TEST_NOTIFICATION_INTERVAL)

        verify(notificationsProducer).enqueue(testExpectedMessage)
        verify(planFactService).enrichEntities(testGroup.planFacts)
        getProcessorState(testGroup) shouldBe additionalData(
            lastNotificationTime = NOW_TIME,
            lastCheckValue = testNewDeadlinesCount,
        )
    }

    @Test
    @DisplayName("Не присылаем сообщение, если количество заказов продолжает находиться ниже порога")
    fun doNotSendMessageIfContinueBelowThreshold() {
        val testPlanFactsCount = TEST_THRESHOLD - 1
        val testCurrentData = additionalData(
            lastNotificationTime = NOW_TIME.minus(TEST_NOTIFICATION_INTERVAL).minusSeconds(1),
            lastCheckValue = testPlanFactsCount,
        )
        val testGroup = setupGroup(
            planFacts = testPlanFactsCount,
            data = testCurrentData,
        )

        processor.process(mockRule(), testGroup) shouldBe NOW_TIME.plus(TEST_NOTIFICATION_INTERVAL)

        verifyZeroInteractions(notificationsProducer)
        verify(planFactService).enrichEntities(testGroup.planFacts)
        getProcessorState(testGroup) shouldBe testCurrentData
    }

    @Test
    @DisplayName("Не присылаем сообщение, если нет заказов от указанных партнёров")
    fun doNotSendMessageIfNoOrdersFromPartner() {
        val testGroup = setupGroup(planFacts = TEST_THRESHOLD + 1)

        processor.process(mockRule(partnerId = 2), testGroup) shouldBe NOW_TIME.plus(TEST_NOTIFICATION_INTERVAL)

        verifyZeroInteractions(notificationsProducer)
        verify(planFactService).enrichEntities(testGroup.planFacts)
        getProcessorState(testGroup) shouldBe additionalData(lastCheckValue = 0)
    }

    private fun setupGroup(
        planFacts: Int = 1,
        data: PlanFactAdditionalData? = null,
    ): PlanFactGroup {
        val testGroup = PlanFactGroup()
        testGroup.planFacts = mutableSetOf()
        repeat(planFacts) { id -> testGroup.planFacts.add(generatePlanFact(id.toLong())) }
        if (data != null) {
            testGroup.setData(data)
        }
        return testGroup
    }

    private fun generatePlanFact(
        id: Long,
        partnerId: Long = TEST_PARTNER_ID,
        partnerName: String = TEST_PARTNER_NAME,
    ): PlanFact {
        val segment = generateWaybill(
            id = id,
            partnerId = partnerId,
            partnerName = partnerName,
        )
        return PlanFact(
            id = id,
            entityId = segment.id,
            expectedStatusDatetime = TEST_DEADLINE,
        ).apply { entity = segment }
    }

    private fun generateWaybill(
        id: Long,
        partnerId: Long = TEST_PARTNER_ID,
        partnerName: String = TEST_PARTNER_NAME,
    ) = WaybillSegment(
        id = id,
        segmentType = SegmentType.MOVEMENT,
        partnerId = partnerId,
        partnerName = partnerName,
    ).apply { order = generateOrder(id) }

    private fun generateOrder(id: Long) = LomOrder(
        id = id,
        barcode = id.toString(),
    )

    private fun setupTelegramMessage(
        channel: String = TEST_TELEGRAM_CHANNEL,
        message: String,
        sender: String = TEST_TELEGRAM_SENDER,
    ): EnqueueParams<SendTelegramMessagePayload> {
        val payload = EnqueueParams.create(
            SendTelegramMessagePayload(
                channel = channel,
                message = message,
                sender = sender,
            )
        )
        whenever(notificationsProducer.enqueue(payload)).thenReturn(1)
        return payload
    }

    private fun mockRule(
        partnerId: Long = TEST_PARTNER_ID,
        enabled: Boolean = true,
    ) = QualityRule(
        rule = ExpressAssembleNotificationsPayload(
            sender = TEST_TELEGRAM_SENDER,
            notificationRepeatInterval = TEST_NOTIFICATION_INTERVAL,
            detailedDelayLevels = TEST_DELAYS,
            channelsSettings = mapOf(
                NOTIFICATION_CHANNEL_1 to ChannelNotificationSettings(
                    enabled = enabled,
                    channel = TEST_TELEGRAM_CHANNEL,
                    threshold = TEST_THRESHOLD,
                    thresholdDelay = TEST_THRESHOLD_DELAY,
                    limit = TEST_LIMIT,
                    partnersInMessage = 2,
                    ordersInMessage = 2,
                    commonPartnerName = TEST_COMMON_PARTNER_NAME,
                    cabinets = listOf(
                        CabinetInfo(
                            id = TEST_PARTNER_CABINET_ID,
                            name = TEST_PARTNER_CABINET_NAME,
                            partners = listOf(partnerId),
                        )
                    ),
                )
            )
        )
    )

    private fun mockRuleWithTwoCabinets() = QualityRule(
        rule = ExpressAssembleNotificationsPayload(
            sender = TEST_TELEGRAM_SENDER,
            notificationRepeatInterval = TEST_NOTIFICATION_INTERVAL,
            detailedDelayLevels = TEST_DELAYS,
            channelsSettings = mapOf(
                NOTIFICATION_CHANNEL_1 to ChannelNotificationSettings(
                    enabled = true,
                    channel = TEST_TELEGRAM_CHANNEL,
                    threshold = TEST_THRESHOLD,
                    thresholdDelay = TEST_THRESHOLD_DELAY,
                    limit = TEST_LIMIT,
                    partnersInMessage = 2,
                    ordersInMessage = 2,
                    commonPartnerName = TEST_COMMON_PARTNER_NAME,
                    cabinets = listOf(
                        CabinetInfo(
                            id = TEST_PARTNER_CABINET_ID,
                            name = TEST_PARTNER_CABINET_NAME,
                            partners = listOf(TEST_PARTNER_ID),
                        ),
                        CabinetInfo(
                            id = TEST_ANOTHER_PARTNER_CABINET_ID,
                            name = TEST_ANOTHER_PARTNER_CABINET_NAME,
                            partners = listOf(TEST_ANOTHER_PARTNER_ID),
                        ),
                    ),
                ),
            )
        )
    )

    private fun mockRuleWithTwoChannels(
        partnerId: Long = TEST_PARTNER_ID,
    ) = QualityRule(
        rule = ExpressAssembleNotificationsPayload(
            sender = TEST_TELEGRAM_SENDER,
            notificationRepeatInterval = TEST_NOTIFICATION_INTERVAL,
            detailedDelayLevels = TEST_DELAYS,
            channelsSettings = mapOf(
                NOTIFICATION_CHANNEL_1 to ChannelNotificationSettings(
                    enabled = true,
                    channel = TEST_TELEGRAM_CHANNEL,
                    threshold = TEST_THRESHOLD,
                    thresholdDelay = TEST_THRESHOLD_DELAY,
                    limit = TEST_LIMIT,
                    partnersInMessage = 2,
                    ordersInMessage = 2,
                    commonPartnerName = TEST_COMMON_PARTNER_NAME,
                    cabinets = listOf(
                        CabinetInfo(
                            id = TEST_PARTNER_CABINET_ID,
                            name = TEST_PARTNER_CABINET_NAME,
                            partners = listOf(partnerId),
                        )
                    ),
                ),
                NOTIFICATION_CHANNEL_2 to ChannelNotificationSettings(
                    enabled = true,
                    channel = TEST_ANOTHER_TELEGRAM_CHANNEL,
                    threshold = TEST_THRESHOLD,
                    thresholdDelay = TEST_THRESHOLD_DELAY,
                    limit = TEST_LIMIT,
                    partnersInMessage = 2,
                    ordersInMessage = 2,
                    commonPartnerName = TEST_COMMON_ANOTHER_PARTNER_NAME,
                    cabinets = listOf(
                        CabinetInfo(
                            id = TEST_ANOTHER_PARTNER_CABINET_ID,
                            name = TEST_ANOTHER_PARTNER_CABINET_NAME,
                            partners = listOf(TEST_ANOTHER_PARTNER_ID),
                        ),
                    ),
                )
            )
        )
    )

    private fun getProcessorState(
        group: PlanFactGroup,
    ) = group.getData(ExpressAssembleNotificationAdditionalData::class)

    private fun setupProcessor() = ExpressAssembleNotificationProcessor(
        clock = clock,
        notificationsProducer = notificationsProducer,
        planFactService = planFactService,
    )

    private fun additionalData(
        lastNotificationTime: Instant? = null,
        lastCheckValue: Int,
    ): ExpressAssembleNotificationAdditionalData {
        return ExpressAssembleNotificationAdditionalData(
            channelsState = mutableMapOf(
                NOTIFICATION_CHANNEL_1 to NotificationState(
                    lastNotificationTime = lastNotificationTime,
                    lastCheckValue = lastCheckValue,
                )
            )
        )
    }

    companion object {
        private val TEST_DELAYS = listOf(Range(from = Duration.ofMinutes(5), to = Duration.ofMinutes(10)))
        private const val NOTIFICATION_CHANNEL_1 = "NOTIFICATION_CHANNEL_1"
        private const val NOTIFICATION_CHANNEL_2 = "NOTIFICATION_CHANNEL_2"
        private const val TEST_TELEGRAM_CHANNEL = "TEST_CHANNEL"
        private const val TEST_ANOTHER_TELEGRAM_CHANNEL = "TEST_ANOTHER_TELEGRAM_CHANNEL"
        private const val TEST_TELEGRAM_SENDER = "TEST_SENDER"
        private const val TEST_PARTNER_CABINET_NAME = "TEST_CABINET_NAME"
        private const val TEST_PARTNER_CABINET_ID = 1L
        private const val TEST_ANOTHER_PARTNER_CABINET_NAME = "TEST_PARTNER_CABINET_NAME"
        private const val TEST_ANOTHER_PARTNER_CABINET_ID = 2L
        private val TEST_NOTIFICATION_INTERVAL = Duration.ofMinutes(30)
        private const val TEST_COMMON_PARTNER_NAME = "TEST_COMMON_PARTNER_NAME"
        private const val TEST_COMMON_ANOTHER_PARTNER_NAME = "TEST_COMMON_ANOTHER_PARTNER_NAME"
        private const val TEST_PARTNER_ID = 123L
        private const val TEST_PARTNER_NAME = "TEST_PARTNER_NAME"
        private const val TEST_PARTNER_NAME_SAFE = "TEST-PARTNER-NAME"
        private const val TEST_ANOTHER_PARTNER_ID = 124L
        private const val TEST_ANOTHER_PARTNER_NAME = "TEST_ANOTHER_PARTNER_NAME"
        private const val TEST_ANOTHER_PARTNER_NAME_SAFE = "TEST-ANOTHER-PARTNER-NAME"
        private const val TEST_THRESHOLD = 10
        private const val TEST_LIMIT = 20
        private val TEST_THRESHOLD_DELAY = Duration.ofMinutes(5)
        private val NOW_TIME = Instant.parse("2021-09-08T18:00:00.00Z")
        private val TEST_DEADLINE = NOW_TIME
            .minus(TEST_THRESHOLD_DELAY)
            .minusSeconds(1)
    }
}
