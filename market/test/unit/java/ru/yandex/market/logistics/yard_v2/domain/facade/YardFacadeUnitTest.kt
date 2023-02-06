package ru.yandex.market.logistics.yard_v2.domain.facade

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito
import org.mockito.Mockito.never
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard.client.dto.event.read.YardClientEventDto
import ru.yandex.market.logistics.yard.service.env.SystemPropertyService
import ru.yandex.market.logistics.yard_v2.converter.YardClientEventDtoConverter
import ru.yandex.market.logistics.yard_v2.dbqueue.move_client_to_next_state.MoveClientToNextStateQueueProducer
import ru.yandex.market.logistics.yard_v2.dbqueue.process_client_queue_by_state_to_id.ProcessClientQueueByStateToIdProducer
import ru.yandex.market.logistics.yard_v2.dbqueue.publish_client_state_change_event.PublishClientStateChangeEventProducer
import ru.yandex.market.logistics.yard_v2.dbqueue.refresh_client_state.RefreshClientStateProducer
import ru.yandex.market.logistics.yard_v2.domain.dto.action.ActionProcessingError
import ru.yandex.market.logistics.yard_v2.domain.dto.action.ActionProcessingErrorType
import ru.yandex.market.logistics.yard_v2.domain.dto.action.ActionProcessingResult
import ru.yandex.market.logistics.yard_v2.domain.dto.restriction.RestrictionProcessingError
import ru.yandex.market.logistics.yard_v2.domain.dto.restriction.RestrictionProcessingErrorType
import ru.yandex.market.logistics.yard_v2.domain.dto.restriction.RestrictionProcessingResult
import ru.yandex.market.logistics.yard_v2.domain.dto.yard_client.ClientProcessingResult
import ru.yandex.market.logistics.yard_v2.domain.entity.ActionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.CapacityEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.EdgeEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.RestrictionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.StateEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClient
import ru.yandex.market.logistics.yard_v2.facade.ActionFacade
import ru.yandex.market.logistics.yard_v2.facade.CapacityFacade
import ru.yandex.market.logistics.yard_v2.facade.CapacityFacadeInterface
import ru.yandex.market.logistics.yard_v2.facade.CapacityUnitFacadeInterface
import ru.yandex.market.logistics.yard_v2.facade.ClientFacade
import ru.yandex.market.logistics.yard_v2.facade.ClientOnChangeForYtFacade
import ru.yandex.market.logistics.yard_v2.facade.ClientQueueFacade
import ru.yandex.market.logistics.yard_v2.facade.ClientTimeProcessingForYtFacade
import ru.yandex.market.logistics.yard_v2.facade.EdgeFacade
import ru.yandex.market.logistics.yard_v2.facade.PriorityFunctionFacade
import ru.yandex.market.logistics.yard_v2.facade.RestrictionFacade
import ru.yandex.market.logistics.yard_v2.facade.StateFacade
import ru.yandex.market.logistics.yard_v2.facade.YardClientEventFacade
import ru.yandex.market.logistics.yard_v2.facade.YardClientStateHistoryFacade
import ru.yandex.market.logistics.yard_v2.facade.YardFacade
import ru.yandex.market.logistics.yard_v2.repository.mapper.TicketMapper
import ru.yandex.market.logistics.yard_v2.validator.YardClientEventValidator
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class YardFacadeUnitTest : SoftAssertionSupport() {
    private var yardFacade: YardFacade? = null
    private var edgeFacade: EdgeFacade? = null
    private var actionFacade: ActionFacade? = null
    private var clientFacade: ClientFacade? = null
    private var restrictionFacade: RestrictionFacade? = null
    private var stateFacade: StateFacade? = null
    private var priorityFunctionFacade: PriorityFunctionFacade? = null
    private var clientQueueFacade: ClientQueueFacade? = null
    private var clientStateHistoryFacade: YardClientStateHistoryFacade? = null
    private var yardClientEventValidator: YardClientEventValidator? = null
    private var yardClientEventDtoConverter: YardClientEventDtoConverter? = null
    private var clock: Clock? = null
    private var yardClientEventFacade: YardClientEventFacade? = null
    private var refreshClientStateProducer: RefreshClientStateProducer? = null
    private var moveClientToNextStateQueueProducer: MoveClientToNextStateQueueProducer? = null
    private var capacityUnitFacade: CapacityUnitFacadeInterface? = null
    private var capacityFacade: CapacityFacadeInterface? = null
    private var clientTimeProcessingForYtFacade: ClientTimeProcessingForYtFacade? = null
    private var clientOnChangeForYtFacade: ClientOnChangeForYtFacade? = null
    private var systemPropertyService: SystemPropertyService? = null
    private var ticketMapper: TicketMapper? = null

    @BeforeEach
    fun init() {
        edgeFacade = Mockito.mock(EdgeFacade::class.java)
        actionFacade = Mockito.mock(ActionFacade::class.java)
        clientFacade = Mockito.mock(ClientFacade::class.java)
        restrictionFacade = Mockito.mock(RestrictionFacade::class.java)
        stateFacade = Mockito.mock(StateFacade::class.java)
        priorityFunctionFacade = Mockito.mock(PriorityFunctionFacade::class.java)
        clientQueueFacade = Mockito.mock(ClientQueueFacade::class.java)
        yardClientEventValidator = Mockito.mock(YardClientEventValidator::class.java)
        yardClientEventDtoConverter = YardClientEventDtoConverter()
        yardClientEventFacade = Mockito.mock(YardClientEventFacade::class.java)
        clientStateHistoryFacade = Mockito.mock(YardClientStateHistoryFacade::class.java)
        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault())
        val publishClientStateChangeEventProducer = Mockito.mock(PublishClientStateChangeEventProducer::class.java)
        refreshClientStateProducer = Mockito.mock(RefreshClientStateProducer::class.java)
        moveClientToNextStateQueueProducer = Mockito.mock(MoveClientToNextStateQueueProducer::class.java)
        capacityUnitFacade = Mockito.mock(CapacityUnitFacadeInterface::class.java)
        capacityFacade = Mockito.mock(CapacityFacade::class.java)
        clientTimeProcessingForYtFacade = Mockito.mock(ClientTimeProcessingForYtFacade::class.java)
        clientOnChangeForYtFacade = Mockito.mock(ClientOnChangeForYtFacade::class.java)
        systemPropertyService = Mockito.mock(SystemPropertyService::class.java)
        ticketMapper = Mockito.mock(TicketMapper::class.java)

        val processClientQueueByStateToIdProducer = Mockito.mock(ProcessClientQueueByStateToIdProducer::class.java)

        yardFacade = YardFacade(
            edgeFacade!!,
            actionFacade!!,
            clientFacade!!,
            restrictionFacade!!,
            stateFacade!!,
            yardClientEventFacade!!,
            priorityFunctionFacade!!,
            clientQueueFacade!!,
            clientStateHistoryFacade!!,
            yardClientEventDtoConverter!!,
            clock!!,
            publishClientStateChangeEventProducer,
            refreshClientStateProducer!!,
            moveClientToNextStateQueueProducer!!,
            capacityFacade!!,
            capacityUnitFacade!!,
            processClientQueueByStateToIdProducer,
            systemPropertyService!!,
            ticketMapper!!
        )
    }

    private val actions = listOf(ActionEntity(id = 1))
    private val restrictions = listOf(RestrictionEntity(id = 1))
    private val edgeEntity = EdgeEntity(1, 1, 2, 1, actions, restrictions)

    private val restrictionProcessingErrorResult = RestrictionProcessingResult(
        listOf(
            RestrictionProcessingError(
                id = 1,
                message = "some error",
                errorType = RestrictionProcessingErrorType.NOT_APPLICABLE
            )
        )
    )

    private val actionProcessingErrorResult = ActionProcessingResult(
        listOf(
            ActionProcessingError(
                id = 1,
                message = "some error",
                errorType = ActionProcessingErrorType.DEFAULT
            )
        )
    )

    @Test
    fun processLinkedComponentsAndMoveToNextStateSuccessful() {
        val clientId = 1L
        val operatorWindowId = 123L
        val toState = 2L

        Mockito.`when`(clientFacade!!.getByIdForUpdateSkipLocked(1)).thenReturn(client)
        Mockito.`when`(edgeFacade!!.applyRestrictions(clientId, edgeEntity))
            .thenReturn(RestrictionProcessingResult.success())
        Mockito.`when`(actionFacade!!.runActions(clientId, actions))
            .thenReturn(ActionProcessingResult.success())

        Mockito.`when`(clientFacade!!.getByIdOrThrow(clientId)).thenReturn(
            YardClient(
                id = 1,
                meta = ObjectMapper().createObjectNode().put("capacityUnitId", operatorWindowId.toString())
            )
        )
        Mockito.`when`(stateFacade!!.getByIdOrThrow(toState)).thenReturn(
            StateEntity(
                id = toState,
                capacity = null
            )
        )

        Mockito.`when`(capacityFacade!!.getByCapacityUnitId(123)).thenReturn(CapacityEntity(id = 5))

        Mockito.`when`(capacityUnitFacade!!.findCapacityUnitIdByStateAndClientId(1, 1)).thenReturn(123)

        val result = yardFacade!!.processLinkedComponentsAndMoveToNextState(clientId, edgeEntity)

        softly.assertThat(result).isEqualTo(ClientProcessingResult.success())
        Mockito.verify(clientFacade)?.changeClientState(clientId, 2)
        Mockito.verify(clientQueueFacade)?.delete(clientId)
        Mockito.verify(capacityUnitFacade)?.unfreezeCapacityUnit(operatorWindowId, clientId)
    }

    @Test
    fun processWithRestrictionError() {
        Mockito.`when`(clientFacade!!.getByIdForUpdateSkipLocked(1)).thenReturn(client)
        Mockito.`when`(restrictionFacade!!.removeFromQueueIsRequired(restrictions, 1))
            .thenReturn(true)
        Mockito.`when`(edgeFacade!!.applyRestrictions(1, edgeEntity))
            .thenReturn(restrictionProcessingErrorResult)
        Mockito.`when`(edgeFacade!!.unapplyRestrictions(1, restrictionProcessingErrorResult, edgeEntity))
            .thenReturn(RestrictionProcessingResult.success())
        val expectedResult = ClientProcessingResult(
            listOf(
                RestrictionProcessingError(
                    1,
                    "some error",
                    RestrictionProcessingErrorType.NOT_APPLICABLE
                )
            )
        )

        val result = yardFacade!!.processLinkedComponentsAndMoveToNextState(1, edgeEntity)

        softly.assertThat(result).isEqualTo(expectedResult)

        Mockito.verify(edgeFacade)?.unapplyRestrictions(1, restrictionProcessingErrorResult, edgeEntity)
        Mockito.verify(clientQueueFacade)?.delete(1)
        Mockito.verify(clientFacade, never())?.changeClientState(1, 2)
    }

    @Test
    fun processWithRestrictionErrorWithoutRemoveFromQueue() {
        Mockito.`when`(clientFacade!!.getByIdForUpdateSkipLocked(1)).thenReturn(client)
        Mockito.`when`(restrictionFacade!!.removeFromQueueIsRequired(restrictions, 1))
            .thenReturn(false)
        Mockito.`when`(edgeFacade!!.applyRestrictions(1, edgeEntity))
            .thenReturn(restrictionProcessingErrorResult)
        Mockito.`when`(edgeFacade!!.unapplyRestrictions(1, restrictionProcessingErrorResult, edgeEntity))
            .thenReturn(RestrictionProcessingResult.success())
        val expectedResult = ClientProcessingResult(
            listOf(
                RestrictionProcessingError(
                    1,
                    "some error",
                    RestrictionProcessingErrorType.NOT_APPLICABLE
                )
            )
        )

        val result = yardFacade!!.processLinkedComponentsAndMoveToNextState(1, edgeEntity)

        softly.assertThat(result).isEqualTo(expectedResult)

        Mockito.verify(edgeFacade)?.unapplyRestrictions(1, restrictionProcessingErrorResult, edgeEntity)
        Mockito.verify(clientQueueFacade, never())?.delete(1)
        Mockito.verify(clientFacade, never())?.changeClientState(1, 2)
    }

    @Test
    fun processWithActionError() {
        Mockito.`when`(clientFacade!!.getByIdForUpdateSkipLocked(1)).thenReturn(client)
        Mockito.`when`(edgeFacade!!.applyRestrictions(1, edgeEntity))
            .thenReturn(RestrictionProcessingResult.success())
        Mockito.`when`(actionFacade!!.runActions(1, actions))
            .thenReturn(actionProcessingErrorResult)
        Mockito.`when`(edgeFacade!!.unapplyRestrictions(1, RestrictionProcessingResult.success(), edgeEntity))
            .thenReturn(RestrictionProcessingResult.success())

        val expectedResult = ClientProcessingResult(
            listOf(
                ActionProcessingError(
                    1,
                    "some error",
                    ActionProcessingErrorType.DEFAULT
                )
            )
        )

        val result = yardFacade!!.processLinkedComponentsAndMoveToNextState(1, edgeEntity)

        softly.assertThat(result).isEqualTo(expectedResult)

        Mockito.verify(edgeFacade)?.unapplyRestrictions(1, RestrictionProcessingResult.success(), edgeEntity)
        Mockito.verify(clientFacade, never())?.changeClientState(1, 2)
        Mockito.verify(clientQueueFacade, never())?.delete(1)
    }

    @Test
    fun saveEvents() {
        Mockito.`when`(clientFacade!!.getByExternalIdsAndServiceId(anyList(), any()))
            .thenReturn(listOf(YardClient(id = 1, externalId = "extId")))
        yardFacade!!.saveEvents(1, listOf(YardClientEventDto("extId", "type", null)))
        Mockito.verify(yardClientEventFacade)!!.persistAll(anyList())
    }

    companion object {
        val client = YardClient(id = 1, stateId = 1)
    }
}
