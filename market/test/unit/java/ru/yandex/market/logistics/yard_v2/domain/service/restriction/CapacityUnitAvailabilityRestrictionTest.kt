package ru.yandex.market.logistics.yard_v2.domain.service.restriction

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionType
import ru.yandex.market.logistics.yard_v2.domain.entity.CapacityUnitEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.EdgeEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.RestrictionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClient
import ru.yandex.market.logistics.yard_v2.facade.CapacityUnitFacadeInterface
import ru.yandex.market.logistics.yard_v2.facade.ClientFacade
import ru.yandex.market.logistics.yard_v2.facade.EdgeFacade

class CapacityUnitAvailabilityRestrictionTest: SoftAssertionSupport() {
    private var restriction: CapacityUnitAvailabilityRestriction? = null
    private var capacityUnitFacade: CapacityUnitFacadeInterface? = null
    private var clientFacade: ClientFacade? = null
    private var edgeFacade: EdgeFacade? = null


    @BeforeEach
    fun init() {
        capacityUnitFacade = Mockito.mock(CapacityUnitFacadeInterface::class.java)
        clientFacade = Mockito.mock(ClientFacade::class.java)
        edgeFacade = Mockito.mock(EdgeFacade::class.java)
        restriction = CapacityUnitAvailabilityRestriction(capacityUnitFacade!!, clientFacade!!, edgeFacade!!)
    }

    @Test
    fun testIsApplicable() {
        val clientId = 1L
        val edgeId = 2L
        val windowId = 3L
        val capacityUnitSet = listOf(windowId)
        Mockito.`when`(capacityUnitFacade!!.getActiveCapacityUnitsByEdgeId(edgeId)).thenReturn(capacityUnitSet)
        Mockito.`when`(capacityUnitFacade!!.getByIdOrThrow(3)).thenReturn(CapacityUnitEntity(isActive = true))
        Mockito.`when`(capacityUnitFacade!!.findCapacityUnitIdByStateAndClientId(1, clientId))
            .thenReturn(null)
        Mockito.`when`(edgeFacade!!.getByIdOrThrow(edgeId)).thenReturn(EdgeEntity(stateFromId = 1))
        Mockito.`when`(clientFacade!!.getByIdOrThrow(clientId)).thenReturn(YardClient(
            id = 1,
            meta = ObjectMapper().createObjectNode().put("capacityUnitId", windowId.toString())
        ))

        val actual = restriction!!.isApplicable(
            clientId,
            RestrictionEntity(1, edgeId, RestrictionType.CAPACITY_UNIT_AVAILABILITY_RESTRICTION)
        )

        softly.assertThat(actual).isEqualTo(true)
    }

    @Test
    fun testIsApplicableFalse() {
        val clientId = 1L
        val edgeId = 2L
        Mockito.`when`(capacityUnitFacade!!.getActiveCapacityUnitsByEdgeId(edgeId)).thenReturn(emptyList())

        val actual = restriction!!.isApplicable(
            clientId,
            RestrictionEntity(1, edgeId, RestrictionType.CAPACITY_UNIT_AVAILABILITY_RESTRICTION)
        )

        softly.assertThat(actual).isEqualTo(false)
    }
}
