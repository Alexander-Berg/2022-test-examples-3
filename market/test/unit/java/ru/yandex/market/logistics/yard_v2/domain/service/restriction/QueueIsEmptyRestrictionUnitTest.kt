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
import ru.yandex.market.logistics.yard_v2.facade.ClientQueueFacade
import ru.yandex.market.logistics.yard_v2.facade.StateFacade

class QueueIsEmptyRestrictionUnitTest: SoftAssertionSupport() {

    private var clientFacade: ClientFacade? = null
    private var stateFacade: StateFacade? = null
    private var clientQueueFacade: ClientQueueFacade? = null
    private var queueIsEmptyRestriction: QueueIsEmptyRestriction? = null

    @BeforeEach
    fun init() {
        clientFacade = mock(ClientFacade::class.java)
        stateFacade = mock(StateFacade::class.java)
        clientQueueFacade = mock(ClientQueueFacade::class.java)
        queueIsEmptyRestriction = QueueIsEmptyRestriction(clientFacade!!, stateFacade!!, clientQueueFacade!!)
    }

    @Test
    fun queueIsEmptyWhenElementsInAlmostEmptyQueueIsNotPresent() {
        `when`(clientFacade!!.getByIdOrThrow(1)).thenReturn(YardClient(serviceId = 123))
        `when`(stateFacade!!.getNumberOfClientsInStates(listOf("state1", "state2"), 123)).thenReturn(0)
        val actual = queueIsEmptyRestriction!!.isApplicable(1, createRestrictionEntity(null))
        softly.assertThat(actual).isTrue
    }

    @Test
    fun queueIsNotEmptyWhenElementsInAlmostEmptyQueueIsNotPresent() {
        `when`(clientFacade!!.getByIdOrThrow(1)).thenReturn(YardClient(serviceId = 123))
        `when`(stateFacade!!.getNumberOfClientsInStates(listOf("state1", "state2"), 123)).thenReturn(1)
        val actual = queueIsEmptyRestriction!!.isApplicable(1, createRestrictionEntity(null))
        softly.assertThat(actual).isFalse
    }

    @Test
    fun queueIsAlmostEmptyWhenElementsInAlmostEmptyQueueIsPresent() {
        `when`(clientFacade!!.getByIdOrThrow(1)).thenReturn(YardClient(serviceId = 123))
        `when`(stateFacade!!.getNumberOfClientsInStates(listOf("state1", "state2"), 123)).thenReturn(10)
        `when`(clientQueueFacade!!.getNumberOfClientsInQueueToStates(listOf("state1", "state2"), 123, 1)).thenReturn(1)
        val actual = queueIsEmptyRestriction!!.isApplicable(1, createRestrictionEntity("11"))
        softly.assertThat(actual).isTrue
    }

    @Test
    fun queueIsAlmostEmptyOnlyWithSumWhenElementsInAlmostEmptyQueueIsPresent() {
        `when`(clientFacade!!.getByIdOrThrow(1)).thenReturn(YardClient(serviceId = 123))
        `when`(stateFacade!!.getNumberOfClientsInStates(listOf("state1", "state2"), 123)).thenReturn(10)
        `when`(clientQueueFacade!!.getNumberOfClientsInQueueToStates(listOf("state1", "state2"), 123, 1)).thenReturn(2)
        val actual = queueIsEmptyRestriction!!.isApplicable(1, createRestrictionEntity("11"))
        softly.assertThat(actual).isFalse
    }

    @Test
    fun queueIsNotAlmostEmptyOnlyByClientsInStatesWhenElementsInAlmostEmptyQueueIsPresent() {
        `when`(clientFacade!!.getByIdOrThrow(1)).thenReturn(YardClient(serviceId = 123))
        `when`(stateFacade!!.getNumberOfClientsInStates(listOf("state1", "state2"), 123)).thenReturn(10)
        val actual = queueIsEmptyRestriction!!.isApplicable(1, createRestrictionEntity("9"))
        softly.assertThat(actual).isFalse
    }

    private fun createRestrictionEntity(elementsInAlmostEmptyQueue: String?): RestrictionEntity {
        val params: MutableList<EntityParam> = ArrayList()
        params.add(EntityParam(RestrictionParamType.STATES_TO_CHECK_QUEUE_IS_EMPTY.name, "state1, state2"))
        if (elementsInAlmostEmptyQueue != null) {
            params.add(EntityParam(
                RestrictionParamType.ELEMENTS_IN_ALMOST_EMPTY_QUEUE.name,
                elementsInAlmostEmptyQueue))
        }
        return RestrictionEntity(
            1,
            null,
            RestrictionType.QUEUE_IS_EMPTY,
            params
        )
    }
}
