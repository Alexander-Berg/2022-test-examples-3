package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.properties.StatisticsReportGrafanaProperties
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.SendPlanFactPeakStatisticsConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.base.producer.SendTelegramMessageEventProducer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.SendPlanFactPeakStatisticsPayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.SendTelegramMessagePayload
import ru.yandex.market.logistics.mqm.monitoringevent.processorconfig.SendPlanFactPeakStatisticsProcessorConfig
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.repository.MonitoringEventProcessorRepository
import ru.yandex.market.logistics.mqm.service.statisticsreport.MaxOverdue
import ru.yandex.market.logistics.mqm.service.statisticsreport.PlanFactStatisticsReportService
import ru.yandex.market.logistics.mqm.service.statisticsreport.querybuilder.AbstractStatisticsReportQueryBuilder
import ru.yandex.money.common.dbqueue.api.EnqueueParams
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class SendPlanFactPeakStatisticsConsumerTest : AbstractContextualTest() {

    @Autowired
    private lateinit var queueRegister: QueueRegister

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var monitoringEventProcessorRepository: MonitoringEventProcessorRepository

    @Autowired
    private lateinit var builders: List<AbstractStatisticsReportQueryBuilder>

    @Autowired
    private lateinit var grafanaProperties: StatisticsReportGrafanaProperties

    @Mock
    private lateinit var statisticsReportService: PlanFactStatisticsReportService

    @Mock
    private lateinit var sendTelegramMessageEventProducer: SendTelegramMessageEventProducer

    private lateinit var sendPlanFactPeakStatisticsConsumer: SendPlanFactPeakStatisticsConsumer


    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        clock.setFixed(Instant.parse("2021-08-27T10:00:00.00Z"), MOSCOW_ZONE)

        sendPlanFactPeakStatisticsConsumer = SendPlanFactPeakStatisticsConsumer(
            queueRegister,
            objectMapper,
            monitoringEventProcessorRepository,
            sendTelegramMessageEventProducer,
            statisticsReportService,
            builders
        )

        doReturn(
            listOf(
                Pair("[id=1]Partner1", 3),
                Pair("[id=2]Partner2", 2),
                Pair("[id=3]Partner3", 1),
                Pair("[id=4]Partner4", 1)
            )
        )
            .whenever(statisticsReportService)
            .findPartnerWithMaxOverduePlanFactsAtInstant(eq(INSTANT), any())
    }

    @Test
    fun buildMessageWithMaxDataTest() {

        doReturn(MaxOverdue(10, INSTANT))
            .whenever(statisticsReportService)
            .getMaxOverduePlanFactsForPeriod(any(), any(), any(), any())

        sendPlanFactPeakStatisticsConsumer.processPayload(
            SendPlanFactPeakStatisticsPayload(
                clock.instant().minus(1, ChronoUnit.DAYS),
                clock.instant()
            ),
            SendPlanFactPeakStatisticsProcessorConfig(CHANNEL)
        )

        val argumentCaptor = argumentCaptor<EnqueueParams<SendTelegramMessagePayload>>()

        verify(sendTelegramMessageEventProducer).enqueue(argumentCaptor.capture())

        val result = argumentCaptor.firstValue.payload!!

        assertSoftly {
            result.channel shouldBe CHANNEL
            result.message shouldBe "Отчет по графикам за период *26 Aug 13:00* - *27 Aug 13:00*\n\n" +
                    "*Сборка FULFILLMENT:*\n$STATS_WITH_MAX_BY_PARTNER" +
                    GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffAssemblyUrl) +
                    "\n\n*Отгрузка FULFILLMENT:*\n$STATS_WITH_MAX_BY_PARTNER" +
                    GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffShipmentUrl) +
                    "\n\n*Приемка FULFILLMENT - DELIVERY:*\n$STATS_WITH_MAX_BY_PARTNER" +
                    GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffDsIntakeUrl) +
                    "\n\n*Приемка МК:*\n$STATS_WITH_MAX_BY_PARTNER" +
                    GRAFANA_URL_TEMPLATE.format(grafanaProperties.mcIntakeUrl)
        }
    }

    @Test
    fun buildMessageWithAllDataTest() {

        doReturn(MaxOverdue(10, INSTANT, 8, INSTANT))
            .whenever(statisticsReportService)
            .getMaxOverduePlanFactsForPeriod(any(), any(), any(), any())

        sendPlanFactPeakStatisticsConsumer.processPayload(
            SendPlanFactPeakStatisticsPayload(
                clock.instant().minus(1, ChronoUnit.DAYS),
                clock.instant()
            ),
            SendPlanFactPeakStatisticsProcessorConfig(CHANNEL)
        )

        val argumentCaptor = argumentCaptor<EnqueueParams<SendTelegramMessagePayload>>()

        verify(sendTelegramMessageEventProducer).enqueue(argumentCaptor.capture())

        val result = argumentCaptor.firstValue.payload!!

        assertSoftly {
            result.channel shouldBe CHANNEL
            result.message shouldBe "Отчет по графикам за период *26 Aug 13:00* - *27 Aug 13:00*\n\n" +
                    "*Сборка FULFILLMENT:*\n$STATS_WITH_ALL_BY_PARTNER" +
                    GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffAssemblyUrl) +
                    "\n\n*Отгрузка FULFILLMENT:*\n$STATS_WITH_ALL_BY_PARTNER" +
                    GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffShipmentUrl) +
                    "\n\n*Приемка FULFILLMENT - DELIVERY:*\n$STATS_WITH_ALL_BY_PARTNER" +
                    GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffDsIntakeUrl) +
                    "\n\n*Приемка МК:*\n$STATS_WITH_ALL_BY_PARTNER" +
                    GRAFANA_URL_TEMPLATE.format(grafanaProperties.mcIntakeUrl)
        }
    }

    @Test
    fun buildMessageWithNoDataTest() {
        doReturn(MaxOverdue(0, null))
            .whenever(statisticsReportService)
            .getMaxOverduePlanFactsForPeriod(any(), any(), any(), any())

        sendPlanFactPeakStatisticsConsumer.processPayload(
            SendPlanFactPeakStatisticsPayload(
                clock.instant().minus(1, ChronoUnit.DAYS),
                clock.instant()
            ),
            SendPlanFactPeakStatisticsProcessorConfig(CHANNEL)
        )

        val argumentCaptor = argumentCaptor<EnqueueParams<SendTelegramMessagePayload>>()

        verify(sendTelegramMessageEventProducer).enqueue(argumentCaptor.capture())

        val result = argumentCaptor.firstValue.payload!!

        assertSoftly {
            result.channel shouldBe CHANNEL
            result.message shouldBe "Отчет по графикам за период *26 Aug 13:00* - *27 Aug 13:00*\n\n" +
                    "*Сборка FULFILLMENT:* нет данных\n\n" +
                    "*Отгрузка FULFILLMENT:* нет данных\n\n" +
                    "*Приемка FULFILLMENT - DELIVERY:* нет данных\n\n" +
                    "*Приемка МК:* нет данных"
        }
    }

    companion object {
        private val INSTANT =
            LocalDateTime.of(2021, 8, 17, 18, 18, 18, 101)
                .atZone(MOSCOW_ZONE).toInstant()

        private const val CHANNEL = "MQM_CHANNEL"
        private const val STATS_WITH_MAX_BY_PARTNER = "В *18:18:18* обнаружено наибольшее суммарное количество просрочек " +
                "за сутки: *10 заказов*, из них:\n" +
                "3 заказа -``` [id=1]Partner1 ```\n" +
                "2 заказа -``` [id=2]Partner2 ```\n" +
                "1 заказ -``` [id=3]Partner3 ```\n"
        private const val STATS_WITH_ALL_BY_PARTNER = "В *18:18:18* обнаружено наибольшее суммарное количество просрочек " +
                "за сутки: *10 заказов*. Этот пик спал до *8 заказов* в *18:18:18*. Данные:\n" +
                "3 заказа -``` [id=1]Partner1 ```\n" +
                "2 заказа -``` [id=2]Partner2 ```\n" +
                "1 заказ -``` [id=3]Partner3 ```\n"
        private const val GRAFANA_URL_TEMPLATE = "[График](%s&from=1629972000000&to=1630058400000)"
    }
}
