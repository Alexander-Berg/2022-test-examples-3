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
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.configuration.properties.StatisticsReportGrafanaProperties
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.SendPlanFactStatisticsPerReceivingPartnersConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.base.producer.SendTelegramMessageEventProducer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.SendPlanFactStatisticsPerReceivingPartnersPayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.SendTelegramMessagePayload
import ru.yandex.market.logistics.mqm.monitoringevent.processorconfig.SendPlanFactStatisticsPerReceivingPartnersProcessorConfig
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.repository.MonitoringEventProcessorRepository
import ru.yandex.market.logistics.mqm.service.statisticsreport.PlanFactStatisticsReportService
import ru.yandex.market.logistics.mqm.service.statisticsreport.querybuilder.AbstractStatisticsReportQueryBuilder
import ru.yandex.money.common.dbqueue.api.EnqueueParams
import java.time.Instant
import java.time.LocalDateTime

class SendPlanFactStatisticsPerReceivingPartnersConsumerTest : AbstractContextualTest() {

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

    private lateinit var consumer: SendPlanFactStatisticsPerReceivingPartnersConsumer


    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        clock.setFixed(Instant.parse("2021-08-27T10:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)

        consumer = SendPlanFactStatisticsPerReceivingPartnersConsumer(
            queueRegister,
            objectMapper,
            monitoringEventProcessorRepository,
            sendTelegramMessageEventProducer,
            statisticsReportService,
            builders
        )
    }

    @Test
    fun buildMessageWithAllDataTest() {
        doReturn(listOf(
            Pair("[id=1]Partner1", 3),
            Pair("[id=2]Partner2", 2),
            Pair("[id=3]Partner3", 1),
            Pair("[id=4]Partner4", 1)
        ))
            .whenever(statisticsReportService)
            .findPartnerWithMaxOverduePlanFactsAtInstant(eq(INSTANT), any())

        doReturn(listOf(
            Pair("[id=5]RecPartner1", 3),
            Pair("[id=6]RecPartner2", 2),
            Pair("[id=7]RecPartner3", 1),
            Pair("[id=8]RecPartner4", 1)
        ))
            .whenever(statisticsReportService)
            .findReceivingPartnerWithMaxOverduePlanFactsAtInstant(eq(INSTANT), any())

        consumer.processPayload(
            SendPlanFactStatisticsPerReceivingPartnersPayload(INSTANT),
            SendPlanFactStatisticsPerReceivingPartnersProcessorConfig(CHANNEL)
        )

        val argumentCaptor = argumentCaptor<EnqueueParams<SendTelegramMessagePayload>>()

        verify(sendTelegramMessageEventProducer).enqueue(argumentCaptor.capture())

        val result = argumentCaptor.firstValue.payload!!

        assertSoftly {
            result.channel shouldBe CHANNEL
            result.message shouldBe "Показатели на *17 Aug 18:18*\n\n" +
                "*Сборка FULFILLMENT:*\n$STATS_BY_PARTNER" +
                GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffAssemblyUrl) +
                "\n\n*Отгрузка FULFILLMENT:*\n$STATS_BY_PARTNER" +
                GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffShipmentUrl) +
                "\n\n*Приемка FULFILLMENT - DELIVERY:*\n$STATS_BY_PARTNER" +
                GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffDsIntakeUrl) +
                "\n\n*Приемка МК:*\n$STATS_BY_PARTNER" +
                GRAFANA_URL_TEMPLATE.format(grafanaProperties.mcIntakeUrl)
        }
    }

    @Test
    fun buildMessageWithNoDataTest() {
        consumer.processPayload(
            SendPlanFactStatisticsPerReceivingPartnersPayload(INSTANT),
            SendPlanFactStatisticsPerReceivingPartnersProcessorConfig(CHANNEL)
        )

        val argumentCaptor = argumentCaptor<EnqueueParams<SendTelegramMessagePayload>>()

        verify(sendTelegramMessageEventProducer).enqueue(argumentCaptor.capture())

        val result = argumentCaptor.firstValue.payload!!

        assertSoftly {
            result.channel shouldBe CHANNEL
            result.message shouldBe "Показатели на *17 Aug 18:18*\n" +
                "\n*Сборка FULFILLMENT:*\n" +
                EMPTY_STATS_BY_PARTNER +
                GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffAssemblyUrl) +
                "\n\n*Отгрузка FULFILLMENT:*\n" +
                EMPTY_STATS_BY_PARTNER +
                GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffShipmentUrl) +
                "\n\n*Приемка FULFILLMENT - DELIVERY:*\n" +
                EMPTY_STATS_BY_PARTNER +
                GRAFANA_URL_TEMPLATE.format(grafanaProperties.ffDsIntakeUrl) +
                "\n\n*Приемка МК:*\n" +
                EMPTY_STATS_BY_PARTNER +
                GRAFANA_URL_TEMPLATE.format(grafanaProperties.mcIntakeUrl)
        }
    }

    companion object {
        private val INSTANT = LocalDateTime.of(2021, 8, 17, 18, 18, 18).atZone(DateTimeUtils.MOSCOW_ZONE).toInstant()

        private const val CHANNEL = "MQM_CHANNEL"
        private const val STATS_BY_PARTNER = "3 заказа -``` [id=1]Partner1 ```\n" +
            "2 заказа -``` [id=2]Partner2 ```\n" +
            "1 заказ -``` [id=3]Partner3 ```\n\n" +
            "Эти заказы ожидались на:\n" +
            "3 заказа -``` [id=5]RecPartner1 ```\n" +
            "2 заказа -``` [id=6]RecPartner2 ```\n" +
            "1 заказ -``` [id=7]RecPartner3 ```\n"
        private const val EMPTY_STATS_BY_PARTNER = "нет данных по партнерам\n\n" +
            "Эти заказы ожидались на:\n" +
            "нет данных по партнерам\n"
        private const val GRAFANA_URL_TEMPLATE = "[График](%s)"
    }
}
