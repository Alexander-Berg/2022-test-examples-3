package ru.yandex.market.logistics.yard_v2.domain.service.restriction

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard.client.dto.configurator.types.RestrictionType
import ru.yandex.market.logistics.yard_v2.domain.entity.CapacityUnitEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.EntityParam
import ru.yandex.market.logistics.yard_v2.domain.entity.RestrictionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.StateEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClient
import ru.yandex.market.logistics.yard_v2.facade.CapacityUnitFacadeInterface
import ru.yandex.market.logistics.yard_v2.facade.ClientFacade
import ru.yandex.market.logistics.yard_v2.facade.StateFacade
import ru.yandex.market.logistics.yard_v2.facade.YardFacade

class PreemptiveCapacityUnitAvailabilityRestrictionTest : SoftAssertionSupport() {
    private var capacityUnitFacade: CapacityUnitFacadeInterface = Mockito.mock(CapacityUnitFacadeInterface::class.java)
    private var clientFacade: ClientFacade = Mockito.mock(ClientFacade::class.java)
    private val stateFacade: StateFacade = Mockito.mock(StateFacade::class.java)
    private val yardFacade: YardFacade = Mockito.mock(YardFacade::class.java)
    private var restriction: Restriction =
        PreemptiveCapacityUnitAvailabilityRestriction(capacityUnitFacade, clientFacade, stateFacade, yardFacade)

    @AfterEach
    fun init() {
        Mockito.reset(capacityUnitFacade, clientFacade, stateFacade, yardFacade)
    }

    @Test
    fun testIsApplicableInEmptyCapacityUnit() {
        val clientId = 1L
        val windowId = 3L

        Mockito.`when`(capacityUnitFacade.getByIdOrThrow(3)).thenReturn(CapacityUnitEntity(isActive = true))
        Mockito.`when`(clientFacade.getByIdOrThrow(clientId)).thenReturn(
            YardClient(
                id = 1,
                meta = ObjectMapper().createObjectNode()
                    .put("capacityUnitId", windowId.toString())
                    .put("lastCapacityUnitId", windowId.toString())
            )
        )

        val actual = restriction.isApplicable(
            clientId,
            RestrictionEntity(1, 1, RestrictionType.PREEMPTIVE_CAPACITY_UNIT_RESTRICTION)
        )

        softly.assertThat(actual).isTrue
    }

    @Test
    fun testIsApplicableOccupiedCapacityUnit() {
        val clientId = 1L
        val occupiedClientId = 2L
        val windowId = 3L
        val stateId = 1L

        val allowedState = "ASSIGNED"
        Mockito.`when`(stateFacade.getById(stateId))
            .thenReturn(StateEntity(id = stateId, name = allowedState))
        Mockito.`when`(capacityUnitFacade.getByIdOrThrow(3))
            .thenReturn(CapacityUnitEntity(isActive = true, occupiedByClientId = occupiedClientId))
        Mockito.`when`(clientFacade.getByIdOrThrow(clientId)).thenReturn(
            YardClient(
                id = 1,
                meta = ObjectMapper().createObjectNode()
                    .put("capacityUnitId", windowId.toString())
                    .put("lastCapacityUnitId", windowId.toString())
            )
        )
        Mockito.`when`(clientFacade.getByIdOrThrow(occupiedClientId)).thenReturn(
            YardClient(
                id = 1,
                stateId = stateId,
                meta = ObjectMapper().createObjectNode()
                    .put("capacityUnitId", windowId.toString())
                    .put("lastCapacityUnitId", windowId.toString())
            )
        )

        val actual = restriction.isApplicable(
            clientId,
            RestrictionEntity(
                1,
                1,
                RestrictionType.PREEMPTIVE_CAPACITY_UNIT_RESTRICTION,
                listOf(EntityParam("ALLOWED_STATES", allowedState))
            )
        )

        softly.assertThat(actual).isTrue
    }
}
