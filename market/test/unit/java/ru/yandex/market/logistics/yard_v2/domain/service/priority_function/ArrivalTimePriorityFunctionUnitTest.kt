package ru.yandex.market.logistics.yard_v2.domain.service.priority_function

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard.client.dto.configurator.types.PriorityFunctionParamType
import ru.yandex.market.logistics.yard_v2.domain.entity.ClientQueueEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.EntityParam
import ru.yandex.market.logistics.yard_v2.domain.entity.PriorityFunctionEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.StateEntity
import ru.yandex.market.logistics.yard_v2.domain.entity.YardClient
import ru.yandex.market.logistics.yard_v2.facade.ClientFacade
import ru.yandex.market.logistics.yard_v2.facade.ClientQueueFacade
import java.time.ZonedDateTime

/**
 * Тест определения приоритета в очереди из N(прибывших вовремя) и L(опоздавших) клиентов
 */
internal class ArrivalTimePriorityFunctionUnitTest : SoftAssertionSupport() {

    private val clientQueueFacade: ClientQueueFacade = mock(ClientQueueFacade::class.java)
    private val clientFacade: ClientFacade = mock(ClientFacade::class.java)
    private val arrivalTimePriorityFunction: ArrivalTimePriorityFunction =
        ArrivalTimePriorityFunction(clientQueueFacade, clientFacade)

    private val queueSkipParam = "3"

    private val state = StateEntity(
        id = 1,
        priorityFunction =
        PriorityFunctionEntity(
            params = listOf(EntityParam(PriorityFunctionParamType.SKIP_N_CLIENTS.name, queueSkipParam))
        )
    )

    private val arrivedInTimeClient = YardClient(
        createdAt = ZonedDateTime.now().toLocalDateTime(),
        arrivalPlannedDate = ZonedDateTime.now().plusMinutes(30)
    )

    private val arrivedInSlotClient = YardClient(
        createdAt = ZonedDateTime.now().toLocalDateTime(),
        arrivalPlannedDate = ZonedDateTime.now().minusMinutes(30),
        arrivalPlannedDateTo = ZonedDateTime.now().plusMinutes(30)
    )

    private val arrivedNotInTimeClient = YardClient(
        createdAt = ZonedDateTime.now().toLocalDateTime(),
        arrivalPlannedDate = ZonedDateTime.now().minusMinutes(30)
    )

    private val arrivedNotInTimeWithLaterTimeSlotClient = YardClient(
        createdAt = ZonedDateTime.now().toLocalDateTime(),
        arrivalPlannedDate = ZonedDateTime.now().minusMinutes(15)
    )

    /**
     * Очередь до вставки пустая
     *
     * Очередь после вставки:
     * →N(0)←
     */
    @Test
    fun getPriorityOnEmptyQueueForNewNClient() {
        setupQueueForArrivedInTime()

        checkNewPriorityIsEqualTo(0)
        checkOtherClientPrioritiesWereNotChanged()
    }

    /**
     * Очередь до вставки:
     * N(0), N(100), N(200)
     *
     * Очередь после вставки:
     * N(0), N(100), N(200), →N(300)←
     */
    @Test
    fun getPriorityOnQueueWithoutLClientsForNewNClient() {
        setupQueueForArrivedInTime(n(0), n(100), n(200))

        checkNewPriorityIsEqualTo(300)
        checkOtherClientPrioritiesWereNotChanged()
    }

    /**
     * Очередь до вставки:
     * L(1)
     *
     * Очередь после вставки:
     * →N(0)←, L(1)
     */
    @Test
    fun getPriorityOnQueueWithOnlyLClientForNewNClient() {
        setupQueueForArrivedInTime(l(1))

        checkNewPriorityIsEqualTo(0)
    }

    /**
     * Очередь до вставки:
     * N(0), L(1), L(2), L(5), L(85)
     *
     * Очередь после вставки:
     * N(0), →N(100)←, L(101), L(102), L(105), L(185)
     */
    @Test
    fun getPriorityOnQueueWithShiftingLClientsForNewNClient() {
        setupQueueForArrivedInTime(n(0), l(1), l(2), l(5), l(85))

        checkNewPriorityIsEqualTo(100)

        verify(clientQueueFacade).updatePriority(1, 101)
        verify(clientQueueFacade).updatePriority(2, 102)
        verify(clientQueueFacade).updatePriority(5, 105)
        verify(clientQueueFacade).updatePriority(85, 185)
        verifyNoMoreInteractions(clientQueueFacade)
    }

    /**
     * Очередь до вставки:
     * N(0), L(1), L(2), L(5), L(85)
     *
     * Очередь после вставки:
     * N(0), →N(100)←, L(101), L(102), L(105), L(185)
     */
    @Test
    fun getPriorityOnQueueWithShiftingLClientsForNewNClientArrivedInSlot() {
        setupQueueForArrivedInSlot(n(0), l(1), l(2), l(5), l(85))

        checkNewPriorityIsEqualTo(100)

        verify(clientQueueFacade).updatePriority(1, 101)
        verify(clientQueueFacade).updatePriority(2, 102)
        verify(clientQueueFacade).updatePriority(5, 105)
        verify(clientQueueFacade).updatePriority(85, 185)
        verifyNoMoreInteractions(clientQueueFacade)
    }

    /**
     * Очередь до вставки:
     * N(0), N(100), L(101), L(102), L(105), L(185)
     *
     * Очередь после вставки (пропускаем опоздавших каждым 3м):
     * N(0), N(100), L(101), →N(200)←, L(202), L(203), L(285)
     */
    @Test
    fun getPriorityOnQueueWithShiftingLClientsExceptOneForNewNClient() {
        setupQueueForArrivedInTime(n(0), n(100), l(101), l(102), l(105), l(185))

        checkNewPriorityIsEqualTo(200)

        verify(clientQueueFacade).updatePriority(102, 202)
        verify(clientQueueFacade).updatePriority(105, 205)
        verify(clientQueueFacade).updatePriority(185, 285)
        verifyNoMoreInteractions(clientQueueFacade)
    }

    /**
     * Очередь до вставки:
     * N(0), N(100), L(101), N(200), L(202), L(203), L(285)
     *
     * Очередь после вставки (пропускаем опоздавших каждым 3м):
     * N(0), N(100), L(101), N(200), →N(300)←, L(302), L(303), L(385)
     */
    @Test
    fun getPriorityOnQueueWithShiftingLClientsForNewNClient2() {
        setupQueueForArrivedInTime(n(0), n(100), l(101), n(200), l(202), l(203), l(285))

        checkNewPriorityIsEqualTo(300)

        verify(clientQueueFacade).updatePriority(202, 302)
        verify(clientQueueFacade).updatePriority(203, 303)
        verify(clientQueueFacade).updatePriority(285, 385)
        verifyNoMoreInteractions(clientQueueFacade)
    }

    /**
     * Очередь до вставки:
     * N(0), N(100), L(101), N(200), N(300), L(302), L(303), L(385)
     *
     * Очередь после вставки (пропускаем опоздавших каждым 3м):
     * N(0), N(100), L(101), N(200), N(300), L(302), →N(400)←, L(403), L(485)
     */
    @Test
    fun getPriorityOnQueueWithShiftingLClientsExceptOneForNewNClient2() {
        setupQueueForArrivedInTime(n(0), n(100), l(101), n(200), n(300), l(302), l(303), l(385))

        checkNewPriorityIsEqualTo(400)

        verify(clientQueueFacade).updatePriority(303, 403)
        verify(clientQueueFacade).updatePriority(385, 485)
        verifyNoMoreInteractions(clientQueueFacade)
    }

    /**
     * Очередь до вставки пустая
     *
     * Очередь после вставки:
     * →L(1)←
     */
    @Test
    fun getPriorityOnEmptyQueueForNewLClient() {
        setupQueueForArrivedNotInTime()

        checkNewPriorityIsEqualTo(1)
        checkOtherClientPrioritiesWereNotChanged()
    }

    /**
     * Очередь до вставки:
     * N(0), N(100), N(200)
     *
     * Очередь после вставки (пропускаем опоздавших каждым 3м):
     * N(0), N(100), →L(101)←, N(200)
     */
    @Test
    fun getPriorityOnQueueWithoutLClientsForNewLClient() {
        setupQueueForArrivedNotInTime(n(0), n(100), n(200))

        checkNewPriorityIsEqualTo(101)
        checkOtherClientPrioritiesWereNotChanged()
    }

    /**
     * Очередь до вставки:
     * L(1)
     *
     * Очередь после вставки:
     * L(1), →L(2)←
     */
    @Test
    fun getPriorityOnQueueWithOnlyLClientForNewLClient() {
        setupQueueForArrivedNotInTime(l(1))

        checkNewPriorityIsEqualTo(2)
        checkOtherClientPrioritiesWereNotChanged()
    }

    /**
     * Очередь до вставки:
     * L(2)
     *
     * Очередь после вставки:
     * →L(2)←, L(3)
     */
    @Test
    fun getPriorityOnQueueWithOnlyLClientWithLaterTimeSlotForNewLClient() {
        setupQueueForArrivedNotInTime(l(2))
        Mockito.`when`(clientFacade.getByIdOrThrow(2)).thenReturn(arrivedNotInTimeWithLaterTimeSlotClient)

        checkNewPriorityIsEqualTo(2)
        verify(clientQueueFacade).updatePriority(2, 3)
        verifyNoMoreInteractions(clientQueueFacade)
    }

    /**
     * Очередь до вставки:
     * L(2), N(100), L(101), L(102), L(103), L(104)
     *
     * Очередь после вставки:
     * L(2), N(100), L(101), →L(102)←, L(103), L(104), L(105)
     */
    @Test
    fun getPriorityOnQueueWithMixClientsForNewLClient() {
        setupQueueForArrivedNotInTime(l(1), n(100), l(101), l(102), l(103), l(104))
        Mockito.`when`(clientFacade.getByIdOrThrow(104)).thenReturn(arrivedNotInTimeWithLaterTimeSlotClient)
        Mockito.`when`(clientFacade.getByIdOrThrow(103)).thenReturn(arrivedNotInTimeWithLaterTimeSlotClient)
        Mockito.`when`(clientFacade.getByIdOrThrow(102)).thenReturn(arrivedNotInTimeWithLaterTimeSlotClient)
        Mockito.`when`(clientFacade.getByIdOrThrow(101)).thenReturn(arrivedNotInTimeClient)

        checkNewPriorityIsEqualTo(102)
        verify(clientQueueFacade).updatePriority(104, 105)
        verify(clientQueueFacade).updatePriority(103, 104)
        verify(clientQueueFacade).updatePriority(102, 103)
        verifyNoMoreInteractions(clientQueueFacade)
    }

    /**
     * Очередь до вставки:
     * N(0), N(100), L(101), N(200)
     *
     * Очередь после вставки (пропускаем опоздавших каждым 3м):
     * N(0), N(100), L(101), N(200), →L(201)←
     */
    @Test
    fun getPriorityOnQueueForNewLClient() {
        setupQueueForArrivedNotInTime(n(0), n(100), l(101), n(200))

        checkNewPriorityIsEqualTo(201)
        checkOtherClientPrioritiesWereNotChanged()
    }

    /**
     * Очередь до вставки:
     * N(0), L(1)
     *
     * Очередь после вставки (пропускаем опоздавших каждым 3м):
     * N(0), L(1), →N(100)←
     */
    @Test
    fun getPriorityOnQueueForNewNClientWithoutShiftWhenPassedManyClientsBefore() {
        setupQueueForArrivedInTime(n(0, 2), l(1, 2))

        checkNewPriorityIsEqualTo(100)
        checkOtherClientPrioritiesWereNotChanged()
    }

    /**
     * Очередь до вставки:
     * N(0), L(1), L(2)
     *
     * Очередь после вставки (пропускаем опоздавших каждым 3м):
     * N(0), L(1), →N(100)←, L(102)
     */
    @Test
    fun getPriorityOnQueueForNewNClientWithShiftBecauseMoreThanOneLClientBefore() {
        setupQueueForArrivedInTime(n(0, 2), l(1, 2), l(2, 2))

        checkNewPriorityIsEqualTo(100)
        verify(clientQueueFacade).findAllByStateToId(1)
        verify(clientQueueFacade).updatePriority(2, 102)
        verifyNoMoreInteractions(clientQueueFacade)
    }

    private fun checkOtherClientPrioritiesWereNotChanged() {
        verify(clientQueueFacade, never()).updatePriority(any(), any())
    }

    private fun checkNewPriorityIsEqualTo(expectedNewPriority: Int) {
        val newPriority = arrivalTimePriorityFunction.getPriority(state, 1)
        softly.assertThat(newPriority).isEqualTo(expectedNewPriority)
        verify(clientQueueFacade).findAllByStateToId(1)
    }

    private fun setupQueueForArrivedInTime(vararg clients: ClientQueueEntity) {
        Mockito.`when`(clientFacade.getByIdOrThrow(1)).thenReturn(arrivedInTimeClient)
        Mockito.`when`(clientQueueFacade.findAllByStateToId(state.id!!)).thenReturn(clients.toList())
    }

    private fun setupQueueForArrivedInSlot(vararg clients: ClientQueueEntity) {
        Mockito.`when`(clientFacade.getByIdOrThrow(1)).thenReturn(arrivedInSlotClient)
        Mockito.`when`(clientQueueFacade.findAllByStateToId(state.id!!)).thenReturn(clients.toList())
    }

    private fun setupQueueForArrivedNotInTime(vararg clients: ClientQueueEntity) {
        Mockito.`when`(clientFacade.getByIdOrThrow(1)).thenReturn(arrivedNotInTimeClient)
        Mockito.`when`(clientQueueFacade.findAllByStateToId(state.id!!)).thenReturn(clients.toList())
    }

    private fun n(p: Int) = ClientQueueEntity(id = p.toLong(), priority = p, currentEdgeId = 0, clientId = p.toLong())
    private fun l(p: Int) = ClientQueueEntity(id = p.toLong(), priority = p, currentEdgeId = 0, clientId = p.toLong())
    private fun n(p: Int, clientsPassedBefore: Int) = ClientQueueEntity(
        id = p.toLong(), priority = p,
        currentEdgeId = 0, clientsPassedBefore = clientsPassedBefore, clientId = p.toLong()
    )

    private fun l(p: Int, clientsPassedBefore: Int) = ClientQueueEntity(
        id = p.toLong(), priority = p,
        currentEdgeId = 0, clientsPassedBefore = clientsPassedBefore, clientId = p.toLong()
    )
}
