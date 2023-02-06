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
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClientStateHistoryEntity
import ru.yandex.market.logistics.yard_v2.facade.YardClientStateHistoryFacade
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class StayingLongTimeInStateRestrictionUnitTest: SoftAssertionSupport() {

    private var yardClientStateHistoryFacade: YardClientStateHistoryFacade? = null
    private var clock: Clock? = null
    private var stayingLongTimeInStateRestriction: StayingLongTimeInStateRestriction? = null

    @BeforeEach
    fun init() {
        yardClientStateHistoryFacade = mock(YardClientStateHistoryFacade::class.java)
        clock = Clock.fixed(Instant.parse("2020-01-01T09:00:00.00Z"), ZoneId.of("Europe/Moscow"))
        stayingLongTimeInStateRestriction = StayingLongTimeInStateRestriction(yardClientStateHistoryFacade!!, clock!!)
    }

    @Test
    fun stayingTooLongInState() {
        `when`(yardClientStateHistoryFacade!!.getCurrentStateHistoryEntity(1)).thenReturn(
            YardClientStateHistoryEntity(createdAt = LocalDateTime.parse("2020-01-01T11:45:00")))
        val actual = stayingLongTimeInStateRestriction!!.isApplicable(1, createRestrictionEntity())
        softly.assertThat(actual).isTrue
    }

    @Test
    fun stayingNormalTimeInState() {
        `when`(yardClientStateHistoryFacade!!.getCurrentStateHistoryEntity(1)).thenReturn(
            YardClientStateHistoryEntity(createdAt = LocalDateTime.parse("2020-01-01T11:45:01")))
        val actual = stayingLongTimeInStateRestriction!!.isApplicable(1, createRestrictionEntity())
        softly.assertThat(actual).isFalse
    }

    private fun createRestrictionEntity(): RestrictionEntity {
        return RestrictionEntity(
            1,
            null,
            RestrictionType.STAYING_LONG_TIME_IN_STATE_RESTRICTION,
            listOf(EntityParam(RestrictionParamType.MINUTES_TO_STAY_IN_STATE.name, "15"))
        )
    }
}
