package ru.yandex.market.logistics.yard_v2.domain.dbqueue.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.logistics.dbqueue.registry.DbQueueConfigRegistry
import ru.yandex.market.logistics.dbqueue.service.DbQueueLogService
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard_v2.dbqueue.move_client_to_next_state.MoveClientToNextStateConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.move_client_to_next_state.MoveClientToNextStatePayload
import ru.yandex.market.logistics.yard_v2.domain.dto.Result
import ru.yandex.market.logistics.yard_v2.domain.entity.EdgeEntity
import ru.yandex.market.logistics.yard_v2.facade.EdgeFacade
import ru.yandex.market.logistics.yard_v2.facade.YardFacade
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShardId

class MoveClientToNextStateConsumerUnitTest() : SoftAssertionSupport() {
    @Test
    fun consume() {
        val yardFacade = Mockito.mock(YardFacade::class.java)
        val edgeFacade = Mockito.mock(EdgeFacade::class.java)
        val processClientQueueConsumer = MoveClientToNextStateConsumer(
            Mockito.mock(DbQueueConfigRegistry::class.java),
            Mockito.mock(ObjectMapper::class.java),
            Mockito.mock(DbQueueLogService::class.java),
            yardFacade,
            edgeFacade
        )
        Mockito.`when`(edgeFacade!!.getFullById(1)).thenReturn(EdgeEntity())
        Mockito.`when`(yardFacade!!.processLinkedComponentsAndMoveToNextState(1, EdgeEntity()))
            .thenReturn(Mockito.mock(Result::class.java))
        processClientQueueConsumer.execute(
            Task.builder<MoveClientToNextStatePayload>(Mockito.mock(QueueShardId::class.java))
                .withPayload(MoveClientToNextStatePayload(1, 1)).build()
        )
        Mockito.verify(yardFacade).processLinkedComponentsAndMoveToNextState(1, EdgeEntity())
    }
}

