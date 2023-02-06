package ru.yandex.market.logistics.yard_v2.domain.service.priority_function

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard.client.dto.configurator.types.PriorityFunctionParamType
import ru.yandex.market.logistics.yard_v2.domain.entity.ClientQueueEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.EntityParam
import ru.yandex.market.logistics.yard_v2.domain.entity.PriorityFunctionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.StateEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClient
import ru.yandex.market.logistics.yard_v2.extension.putMetaValue
import ru.yandex.market.logistics.yard_v2.facade.ClientFacade
import ru.yandex.market.logistics.yard_v2.facade.ClientQueueFacade
import java.time.ZonedDateTime

class RequestTypePriorityFunctionUnitTest : SoftAssertionSupport() {

    private val clientQueueFacade: ClientQueueFacade = Mockito.mock(ClientQueueFacade::class.java)
    private val clientFacade: ClientFacade = Mockito.mock(ClientFacade::class.java)
    private val requestTypePriorityFunction: RequestTypePriorityFunction =
        RequestTypePriorityFunction(clientQueueFacade, clientFacade)

    private val state = StateEntity(
        id = 1,
        priorityFunction =
        PriorityFunctionEntity(
            params = listOf(
                EntityParam(
                    PriorityFunctionParamType.HIGH_PRIORITY_REQUEST_TYPES.name,
                    "highPriorityRequestType"
                )
            )
        )
    )

    private val arrivedInTimeClientWithNeedToRecalc = YardClient(
        createdAt = ZonedDateTime.now().toLocalDateTime(),
        arrivalPlannedDate = ZonedDateTime.now().plusMinutes(30),
        meta = ObjectMapper().createObjectNode().put("needRecalc", true)
    )

    @Test
    fun getPriorityOnEmptyQueueForNewHighPriorityClient() {
        setupQueueForHighPriorityClient()
        checkNewPriorityIsEqualTo(1)
    }

    @Test
    fun getPriorityOnEmptyQueueForNewDefaultClient() {
        setupQueueForDefaultPriorityClient()
        checkNewPriorityIsEqualTo(100)
    }

    @Test
    fun getPriorityOnQueueWithOnlyHighPriorityClientForNewHighPriorityClient() {
        setupQueueForHighPriorityClient(h(1))
        checkNewPriorityIsEqualTo(2)
    }

    @Test
    fun getPriorityOnQueueWithHighPriorityClientForNewHighPriorityClient() {
        setupQueueForHighPriorityClient(h(1), h(2))
        checkNewPriorityIsEqualTo(3)
    }

    @Test
    fun getPriorityOnQueueWithOnlyDefaultPriorityClientForNewHighPriorityClient() {
        setupQueueForHighPriorityClient(d(100))
        checkNewPriorityIsEqualTo(1)
    }

    @Test
    fun getPriorityOnQueueWithMixClientsForNewHighPriorityClient() {
        setupQueueForHighPriorityClient(h(1), h(2), d(100), h(101), d(200))
        checkNewPriorityIsEqualTo(102)
    }

    @Test
    fun getPriorityOnQueueWithMixClientsForNewUnprocessedHighPriorityClient() {
        setupQueueForUnprocessedHighPriorityClient(h(1), h(2), d(100), h(101), d(200))
        checkNewPriorityIsEqualTo(300)
    }

    @Test
    fun getPriorityOnQueueWithOnlyHighPriorityClientForNewDefaultPriorityClient() {
        setupQueueForDefaultPriorityClient(h(1))
        checkNewPriorityIsEqualTo(100)
    }

    @Test
    fun getPriorityOnQueueWithHighPriorityClientForNewDefaultPriorityClient() {
        setupQueueForDefaultPriorityClient(h(1), h(2))
        checkNewPriorityIsEqualTo(100)
    }

    @Test
    fun getPriorityOnQueueWithOnlyDefaultPriorityClientForNewDefaultPriorityClient() {
        setupQueueForDefaultPriorityClient(d(100))
        checkNewPriorityIsEqualTo(200)
    }

    @Test
    fun getPriorityOnQueueWithMixClientsForNewDefaultPriorityClient() {
        setupQueueForDefaultPriorityClient(h(1), h(2), d(100), h(101), d(200))
        checkNewPriorityIsEqualTo(300)
    }

    @Test
    fun getPriorityWithShiftLaterClientWithHighPriority() {
        setupQueueForHighPriorityClient(h(1), id = 0)

        checkNewPriorityIsEqualTo(1)

        Mockito.verify(clientQueueFacade).updatePriority(1, 101)
        verifyNoMoreInteractions(clientQueueFacade)
    }

    @Test
    fun getPriorityWithShiftLaterClientWithHighPriorityInTheMiddle() {
        setupQueueForHighPriorityClient(h(1), h(3), id = 2)

        checkNewPriorityIsEqualTo(3)

        Mockito.verify(clientQueueFacade).updatePriority(3, 103)
        verifyNoMoreInteractions(clientQueueFacade)
    }

    @Test
    fun getPriorityWithoutShiftClientWithHighPriorityAndNeedRecalc() {
        setupQueueForHighPriorityClientWithRecalc(h(1), h(3), id = 2)

        checkNewPriorityIsEqualTo(4)

        verifyNoMoreInteractions(clientQueueFacade)
    }

    @Test
    fun getPriorityWithShiftOnQueueWithMixClientsForNewDefaultPriorityClient() {
        setupQueueForDefaultPriorityClient(h(1), h(2), d(100), h(101), d(200), id = 90)

        checkNewPriorityIsEqualTo(100)

        Mockito.verify(clientQueueFacade).updatePriority(100, 200)
        Mockito.verify(clientQueueFacade).updatePriority(101, 201)
        Mockito.verify(clientQueueFacade).updatePriority(200, 300)

        verifyNoMoreInteractions(clientQueueFacade)
    }

    @Test
    fun getPriorityWithShiftOnQueueWithOnlyDefaultClientForNewDefaultPriorityClient() {
        setupQueueForDefaultPriorityClient(d(100), id = 90)

        checkNewPriorityIsEqualTo(100)

        Mockito.verify(clientQueueFacade).updatePriority(100, 200)

        verifyNoMoreInteractions(clientQueueFacade)
    }

    private fun checkNewPriorityIsEqualTo(expectedNewPriority: Int) {
        val newPriority = requestTypePriorityFunction.getPriority(state, 1)
        softly.assertThat(newPriority).isEqualTo(expectedNewPriority)
        Mockito.verify(clientQueueFacade).findAllByStateToId(1)
    }

    private fun setupQueueForUnprocessedHighPriorityClient(vararg clients: ClientQueueEntity, id: Long = 500) {
        Mockito.`when`(clientFacade.getByIdOrThrow(1)).thenReturn(unprocessedHighPriorityRequestTypeClient(id))
        Mockito.`when`(clientQueueFacade.findAllByStateToId(state.id!!)).thenReturn(clients.toList())
    }

    private fun setupQueueForHighPriorityClient(vararg clients: ClientQueueEntity, id: Long = 500) {
        Mockito.`when`(clientFacade.getByIdOrThrow(1)).thenReturn(highPriorityRequestTypeClient(id))
        Mockito.`when`(clientQueueFacade.findAllByStateToId(state.id!!)).thenReturn(clients.toList())
    }

    private fun setupQueueForHighPriorityClientWithRecalc(vararg clients: ClientQueueEntity, id: Long = 500) {
        Mockito.`when`(clientFacade.getByIdOrThrow(1)).thenReturn(highPriorityRequestTypeClientWithRecalc(id))
        Mockito.`when`(clientQueueFacade.findAllByStateToId(state.id!!)).thenReturn(clients.toList())
    }

    private fun setupQueueForDefaultPriorityClient(vararg clients: ClientQueueEntity, id: Long = 500) {
        Mockito.`when`(clientFacade.getByIdOrThrow(1)).thenReturn(defaultPriorityRequestTypeClient(id))
        Mockito.`when`(clientQueueFacade.findAllByStateToId(state.id!!)).thenReturn(clients.toList())
    }

    private fun defaultPriorityRequestTypeClient(id: Long): YardClient {
        val yardClient = YardClient(id = id)
        yardClient.putMetaValue("requestType", "defaultPriorityRequestType")

        return yardClient
    }

    private fun highPriorityRequestTypeClient(id: Long): YardClient {
        val yardClient = YardClient(id = id)
        yardClient.putMetaValue("requestType", "highPriorityRequestType")
        yardClient.putMetaValue("capacityUnitId", "3")

        return yardClient
    }

    private fun highPriorityRequestTypeClientWithRecalc(id: Long): YardClient {
        val yardClient = YardClient(id = id)
        yardClient.putMetaValue("requestType", "highPriorityRequestType")
        yardClient.putMetaValue("capacityUnitId", "3")
        yardClient.putMetaValue("needRecalc", "true")

        return yardClient
    }

    private fun unprocessedHighPriorityRequestTypeClient(id: Long): YardClient {
        val yardClient = YardClient(id = id)
        yardClient.putMetaValue("requestType", "highPriorityRequestType")

        return yardClient
    }

    /**
     * Default priority client.
     */
    private fun d(p: Int) = ClientQueueEntity(id = p.toLong(), priority = p, currentEdgeId = 0, clientId = p.toLong())

    /**
     * High priority client.
     */
    private fun h(p: Int) = ClientQueueEntity(id = p.toLong(), priority = p, currentEdgeId = 0, clientId = p.toLong())
}
