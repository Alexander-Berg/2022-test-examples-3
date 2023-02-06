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
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClientEventEntity
import ru.yandex.market.logistics.yard_v2.facade.YardClientEventFacade

class EventRequiredRestrictionUnitTest: SoftAssertionSupport() {

    private var yardClientEventFacade: YardClientEventFacade? = null
    private var restriction: EventRequiredRestriction? = null

    @BeforeEach
    fun init() {
        yardClientEventFacade = mock(YardClientEventFacade::class.java)
        restriction = EventRequiredRestriction(yardClientEventFacade!!)
    }

    @Test
    fun isApplicableWhenEventExists() {
        `when`(yardClientEventFacade!!.getByYardClientId(1)).thenReturn(listOf(
            YardClientEventEntity(1, 1, "ARRIVED", null, null),
            YardClientEventEntity(2, 2, "HAS_EVENT", null, null),
        ))
        softly.assertThat(restriction!!.isApplicable(1, createRestrictionEntity("HAS_EVENT"))).isTrue
    }

    @Test
    fun isNotApplicableWhenEventNotExists() {
        `when`(yardClientEventFacade!!.getByYardClientId(1)).thenReturn(listOf(
            YardClientEventEntity(1, 1, "ARRIVED", null, null),
            YardClientEventEntity(2, 2, "HAS_EVENT", null, null),
        ))
        softly.assertThat(restriction!!.isApplicable(1, createRestrictionEntity("HAS_EVENT1"))).isFalse
    }

    private fun createRestrictionEntity(param: String): RestrictionEntity {
        return RestrictionEntity(
            1,
            null,
            RestrictionType.EVENT_REQUIRED,
            listOf(EntityParam(RestrictionParamType.EVENT_TYPE.name, param))
        )
    }
}
