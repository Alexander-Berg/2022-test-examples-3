package ru.yandex.market.logistics.yard.service

import com.amazon.sqs.javamessaging.SQSQueueDestination
import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.les.base.Event
import ru.yandex.market.logistics.les.tm.TripInfoEvent
import ru.yandex.market.logistics.les.tm.dto.MovementCourierDto
import ru.yandex.market.logistics.les.tm.dto.TransportationPartnerExtendedInfoDto
import ru.yandex.market.logistics.les.tm.dto.TripPointDto
import ru.yandex.market.logistics.les.tm.enums.TransportationUnitType
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard_v2.config.les.SqsSCEventConsumer
import ru.yandex.market.logistics.yard_v2.configurator.extension.toTripPointInfoEntities
import ru.yandex.market.logistics.yard_v2.domain.entity.CourierEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.TripPointInfoEntity
import ru.yandex.market.logistics.yard_v2.facade.CourierFacade
import ru.yandex.market.logistics.yard_v2.facade.TripPointInfoFacade
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ProcessTMEventTest(
    @Autowired private val tripPointInfoFacade: TripPointInfoFacade,
    @Autowired private val courierFacade: CourierFacade,
    @Autowired private val sqsSCEventConsumer: SqsSCEventConsumer,
) : AbstractSecurityMockedContextualTest() {

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/tm_event/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/service/tm_event/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun successfulProcessing(
    ) {
        processTmEvent(
            TripInfoEvent(
                1, map(), listOf(
                    TripPointDto(tripPointId = 1, courierId = 10),
                    TripPointDto(tripPointId = 2, courierId = 11),
                    TripPointDto(tripPointId = 3)
                )
            )
        )
    }

    private fun map(): Map<Long, MovementCourierDto> =
        mapOf(
            Pair(10, MovementCourierDto(externalId = "external_id")),
            Pair(11, MovementCourierDto(externalId = "external_id2"))
        )

    private fun processTmEvent(event: TripInfoEvent) {
        val tripPointInfoEntities = event.toTripPointInfoEntities()

        if (tripPointInfoEntities.isNotEmpty()) {
            val couriers = tripPointInfoEntities.mapNotNull { it.courier }

            var courierMap = mapOf<String?, CourierEntity>()
            if (couriers.isNotEmpty()) {
                courierMap = courierFacade.saveAll(couriers)
                    .associateBy { it.externalId }
            }

            tripPointInfoEntities.forEach {
                it.courier?.id = getCourierId(courierMap, it)
                tripPointInfoFacade.save(it)
            }
        }
    }

    private fun getCourierId(
        courierMap: Map<String?, CourierEntity>,
        tripPointInfoEntity: TripPointInfoEntity
    ): Long? {
        return if (courierMap.containsKey(tripPointInfoEntity.courier?.externalId)) {
            courierMap[tripPointInfoEntity.courier?.externalId]?.id
        } else if (tripPointInfoEntity.courier?.externalId != null) {
            courierFacade.findByExternalId(tripPointInfoEntity.courier?.externalId!!).id
        } else {
            null
        }
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/sqs_event_consumer/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/service/sqs_event_consumer/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testProcessTmEvent() {
        val sqsDest = Mockito.mock(SQSQueueDestination::class.java)
        val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        sqsSCEventConsumer.processEvent(
            sqsDest, "messageId", "requestId", timestamp,
            getTestEvent()
        )
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/sqs_event_consumer/with-saved-courier/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/service/sqs_event_consumer/with-saved-courier/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testProcessTmEventWithSavedCourier() {
        val sqsDest = Mockito.mock(SQSQueueDestination::class.java)
        val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        sqsSCEventConsumer.processEvent(
            sqsDest, "messageId", "requestId", timestamp,
            getTestEvent()
        )
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/sqs_event_consumer/tm-event-with-issue-passes/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/service/sqs_event_consumer/tm-event-with-issue-passes/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testProcessTmEventWithIssuePasses() {
        val sqsDest = Mockito.mock(SQSQueueDestination::class.java)
        val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        sqsSCEventConsumer.processEvent(
            sqsDest, "messageId", "requestId", timestamp,
            getTestEvent4IssuePasses()
        )
    }

    @Test
    @DatabaseSetup(value = ["classpath:fixtures/service/sqs_event_consumer/tm-event-with-issue-passes/before.xml"])
    @ExpectedDatabase(
        value = "classpath:fixtures/service/sqs_event_consumer/tm-event-with-issue-passes/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun testProcessSameTmEventsTwiceWithIssuePasses() {
        val sqsDest = Mockito.mock(SQSQueueDestination::class.java)
        val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        sqsSCEventConsumer.processEvent(
            sqsDest, "messageId", "requestId", timestamp,
            getTestEvent4IssuePasses()
        )
        sqsSCEventConsumer.processEvent(
            sqsDest, "messageId", "requestId", timestamp,
            getTestEvent4IssuePasses()
        )
    }

    private fun getTestEvent4IssuePasses() = Event(
        source = "Source",
        eventId = "eventId",
        timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        eventType = "TRIP_INFO",
        description = "Информация о рейсе",
        payload = TripInfoEvent(
            55076, mapOf(
                0L to MovementCourierDto(
                    externalId = "40967",
                    name = "Константин",
                    surname = "Пьянкtest",
                    patronymic = "Григорьевtest",
                    carNumber = "C504TO1test",
                    phone = "+79101089599",
                    courierUid = "-26135",
                )
            ),

            listOf(
                TripPointDto(
                    tripPointId = 84414,
                    plannedIntervalStart = OffsetDateTime.of(2022, 6, 25, 8, 30, 0, 0, ZoneOffset.UTC),
                    courierId = 0,
                    unitType = TransportationUnitType.INBOUND,
                    whPartnerId = 300,
                    whName = "Яндекс.Маркет (Екатеринбург)",
                    unitYandexId = null
                ), TripPointDto(
                    tripPointId = 84415,
                    plannedIntervalStart = OffsetDateTime.of(2022, 6, 25, 8, 30, 0, 0, ZoneOffset.UTC),
                    courierId = 0,
                    unitType = TransportationUnitType.INBOUND,
                    whPartnerId = 300,
                    whName = "Яндекс.Маркет (Екатеринбург)",
                    unitYandexId = "13117254"
                ),
                TripPointDto(
                    tripPointId = 84416,
                    plannedIntervalStart = OffsetDateTime.of(2022, 6, 22, 10, 0, 0, 0, ZoneOffset.UTC),
                    courierId = 0,
                    unitType = TransportationUnitType.OUTBOUND,
                    whPartnerId = 172,
                    whName = "Яндекс.Маркет (Софьино)",
                    unitYandexId = "13117208"
                ), TripPointDto(
                    tripPointId = 84417,
                    plannedIntervalStart = OffsetDateTime.of(2022, 6, 22, 9, 0, 0, 0, ZoneOffset.UTC),
                    courierId = 0,
                    unitType = TransportationUnitType.OUTBOUND,
                    whPartnerId = 407,
                    whName = "Томилино (транзит)",
                    unitYandexId = "TMU28014210"
                )
            ), partner = TransportationPartnerExtendedInfoDto(id = 147943, name = "ГЛТ Москва")
        )

    )

    private fun getTestEvent() = Event(
        source = "Source",
        eventId = "eventId",
        timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
        eventType = "TRIP_INFO",
        description = "Информация о рейсе",
        payload = TripInfoEvent(
            50794, mapOf(
                0L to MovementCourierDto(
                    externalId = "2504",
                    name = "Серtest",
                    surname = "Марtest",
                    patronymic = "Валериеtest",
                    carNumber = "К314ВО 761",
                    phone = "+79101089599",
                    courierUid = "339901427",
                )
            ),

            listOf(
                TripPointDto(
                    tripPointId = 73892,
                    plannedIntervalStart = OffsetDateTime.of(2022, 6, 2, 6, 0, 0, 0, ZoneOffset.UTC),
                    courierId = 0,
                    unitType = TransportationUnitType.OUTBOUND,
                    whPartnerId = 408,
                    whName = "Краснодар (транзит)",
                    unitYandexId = "TMU25540696"
                ),
                TripPointDto(
                    tripPointId = 73893,
                    plannedIntervalStart = OffsetDateTime.of(2022, 6, 2, 6, 30, 0, 0, ZoneOffset.UTC),
                    courierId = 0,
                    unitType = TransportationUnitType.INBOUND,
                    whPartnerId = 147,
                    whName = "Яндекс.Маркет (Ростов-на-Дону)",
                    unitYandexId = null
                )
            ),
            partner = TransportationPartnerExtendedInfoDto(id = 138585, name = "ООО \"Иствард\"")
        )
    )
}
