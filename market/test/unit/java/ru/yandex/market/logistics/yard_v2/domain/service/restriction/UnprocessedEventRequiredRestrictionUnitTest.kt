package ru.yandex.market.logistics.yard_v2.domain.service.restriction

import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionParamType
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionType
import ru.yandex.market.logistics.yard_v2.domain.entity.EntityParam
import ru.yandex.market.logistics.yard_v2.domain.entity.RestrictionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClientEventEntity
import ru.yandex.market.logistics.yard_v2.facade.YardClientEventFacade

class UnprocessedEventRequiredRestrictionUnitTest : SoftAssertionSupport() {

    private var yardClientEventFacade: YardClientEventFacade? = null
    private var restriction: UnprocessedEventRequiredRestriction? = null

    @BeforeEach
    fun init() {
        yardClientEventFacade = mock(YardClientEventFacade::class.java)
        restriction = UnprocessedEventRequiredRestriction(yardClientEventFacade!!)
    }

    @Test
    fun isApplicableWhenEventExists() {
        `when`(yardClientEventFacade!!.getByYardClientId(1)).thenReturn(
            listOf(
                YardClientEventEntity(1, 1, "ARRIVED", null, null),
                YardClientEventEntity(2, 2, "HAS_EVENT", null, null),
            )
        )
        softly.assertThat(restriction!!.isApplicable(1, createRestrictionEntity("HAS_EVENT"))).isTrue
    }

    @Test
    fun isNotApplicableWhenEventNotExists() {
        `when`(yardClientEventFacade!!.getByYardClientId(1)).thenReturn(
            listOf(
                YardClientEventEntity(1, 1, "ARRIVED", null, null),
                YardClientEventEntity(2, 2, "HAS_EVENT", null, null),
            )
        )
        softly.assertThat(restriction!!.isApplicable(1, createRestrictionEntity("HAS_EVENT1"))).isFalse
    }

    @Test
    fun isNotApplicableWhenProcessedEventExists() {
        `when`(yardClientEventFacade!!.getByYardClientId(1)).thenReturn(
            listOf(
                YardClientEventEntity(1, 1, "ARRIVED", null, null),
                YardClientEventEntity(2, 2, "HAS_EVENT", null, null, true),
            )
        )
        softly.assertThat(restriction!!.isApplicable(1, createRestrictionEntity("HAS_EVENT"))).isFalse
    }

    @Test
    fun apply() {
        `when`(yardClientEventFacade!!.getByYardClientId(1)).thenReturn(
            listOf(
                YardClientEventEntity(1, 1, "ARRIVED", null, null),
                YardClientEventEntity(2, 2, "HAS_EVENT", null, null),
            )
        )

        softly.assertThat(restriction!!.apply(1, createRestrictionEntity("HAS_EVENT")).isSuccess()).isTrue
        verify(yardClientEventFacade)?.setProcessed(1, "HAS_EVENT", true)
    }

    @Test
    fun unapply() {
        `when`(yardClientEventFacade!!.getByYardClientId(1)).thenReturn(
            listOf(
                YardClientEventEntity(1, 1, "ARRIVED", null, null),
                YardClientEventEntity(2, 2, "HAS_EVENT", null, null),
            )
        )

        softly.assertThat(restriction!!.unapply(1, createRestrictionEntity("HAS_EVENT")).isSuccess()).isTrue
        verify(yardClientEventFacade)?.setProcessed(1, "HAS_EVENT", false)
    }

    private fun createRestrictionEntity(param: String): RestrictionEntity {
        return RestrictionEntity(
            1,
            null,
            RestrictionType.UNPROCESSED_EVENT_REQUIRED,
            listOf(EntityParam(RestrictionParamType.EVENT_TYPE.name, param))
        )
    }
}
