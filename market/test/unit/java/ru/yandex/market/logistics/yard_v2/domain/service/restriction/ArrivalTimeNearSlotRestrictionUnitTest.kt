package ru.yandex.market.logistics.yard_v2.domain.service.restriction

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionParamType
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionType
import ru.yandex.market.logistics.yard_v2.domain.entity.EntityParam
import ru.yandex.market.logistics.yard_v2.domain.entity.RestrictionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClient
import ru.yandex.market.logistics.yard_v2.facade.ClientFacade
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ArrivalTimeNearSlotRestrictionUnitTest: SoftAssertionSupport() {

    private var clientFacade: ClientFacade? = null
    private var clock: Clock? = null
    private var arrivalTimeNearSlotRestriction: ArrivalTimeNearSlotRestriction? = null

    @BeforeEach
    fun init() {
        clientFacade = mock(ClientFacade::class.java)
        clock = Clock.fixed(Instant.parse("2020-01-01T12:00:00.00Z"), ZoneId.of("Europe/Moscow"))
        arrivalTimeNearSlotRestriction = ArrivalTimeNearSlotRestriction(clientFacade!!, clock!!)
    }

    @Test
    fun arrivalTimeAndNowAreLessThanNormalTime() {
        assertLogicIsCorrect(
            ZonedDateTime.of(2020, 1, 1, 12, 31, 0, 0, ZoneId.of("UTC")),
            LocalDateTime.parse("2020-01-01T14:59:59"),
            false
        )
    }

    @Test
    fun arrivalTimeIsLessThanNormalTimeButNowIsGreaterThanNormalTime() {
        assertLogicIsCorrect(
            ZonedDateTime.of(2020, 1, 1, 12, 29, 0, 0, ZoneId.of("UTC")),
            LocalDateTime.parse("2020-01-01T14:56:59"),
            true
        )
    }

    @Test
    fun arrivalTimeIsEqualToNormalTime() {
        assertLogicIsCorrect(
            ZonedDateTime.of(2020, 1, 1, 12, 29, 0, 0, ZoneId.of("UTC")),
            LocalDateTime.parse("2020-01-01T14:59:00"),
            true
        )
    }

    @Test
    fun arrivalTimeIsGreaterThanThanNormalTime() {
        assertLogicIsCorrect(
            ZonedDateTime.of(2020, 1, 1, 12, 35, 0, 0, ZoneId.of("UTC")),
            LocalDateTime.parse("2020-01-01T15:20:00"),
            true
        )
    }

    @Test
    fun arrivalTimeIsGreaterThanPlannedTime() {
        assertLogicIsCorrect(
            ZonedDateTime.of(2020, 1, 1, 12, 35, 0, 0, ZoneId.of("UTC")),
            LocalDateTime.parse("2020-01-01T16:00:00"),
            true
        )
    }

    private fun assertLogicIsCorrect(arrivalPlannedTime: ZonedDateTime,
                                     actualArrivalTime: LocalDateTime,
                                     expectedResult: Boolean) {
        `when`(clientFacade!!.getByIdOrThrow(1)).thenReturn(createClient(arrivalPlannedTime, actualArrivalTime))
        val actual = arrivalTimeNearSlotRestriction!!.isApplicable(1, createRestrictionEntity())
        softly.assertThat(actual).isEqualTo(expectedResult)
    }

    private fun createRestrictionEntity(): RestrictionEntity {
        return RestrictionEntity(
            1,
            null,
            RestrictionType.ARRIVAL_TIME_NEAR_SLOT,
            listOf(EntityParam(RestrictionParamType.MINUTES_BEFORE_SLOT_TO_ARRIVE_IN_TIME.name, "30"))
        )
    }

    private fun createClient(
        arrivalPlannedTime: ZonedDateTime,
        actualArrivalTime: LocalDateTime,
    ): YardClient {
        return YardClient(
            createdAt = actualArrivalTime,
            arrivalPlannedDate = arrivalPlannedTime
        )
    }
}
