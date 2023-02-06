package ru.yandex.market.logistics.yard_v2.domain.dbqueue.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import ru.yandex.market.logistics.dbqueue.registry.DbQueueConfigRegistry
import ru.yandex.market.logistics.dbqueue.service.DbQueueLogService
import ru.yandex.market.logistics.yard.base.SoftAssertionSupport
import ru.yandex.market.logistics.yard_v2.dbqueue.refresh_client_state.RefreshClientStateConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.refresh_client_state.RefreshClientStatePayload
import ru.yandex.market.logistics.yard_v2.facade.YardFacade
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShardId


class RefreshClientStateConsumerUnitTest() : SoftAssertionSupport() {
    @Test
    fun consume() {
        val yardFacade = Mockito.mock(YardFacade::class.java)
        val refreshClientStateConsumer = RefreshClientStateConsumer(
            Mockito.mock(DbQueueConfigRegistry::class.java),
            Mockito.mock(ObjectMapper::class.java),
            Mockito.mock(DbQueueLogService::class.java),
            yardFacade
        )
        refreshClientStateConsumer.execute(Task.builder<RefreshClientStatePayload>(Mockito.mock(QueueShardId::class.java))
            .withPayload(RefreshClientStatePayload(1))
            .build())
        Mockito.verify(yardFacade).refreshClientState(1)
    }
}

