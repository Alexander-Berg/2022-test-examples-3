package ru.yandex.market.logistics.yard.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.config.ConsumerConfig
import ru.yandex.market.logistics.yard_v2.dbqueue.publish_client_state_change_event.PublishClientStateChangeEventConsumer
import ru.yandex.market.logistics.yard_v2.dbqueue.publish_client_state_change_event.PublishClientStateChangeEventPayload
import ru.yandex.market.logistics.yard.client.dto.event.write.YardClientStateChangeEvent
import ru.yandex.market.logistics.yard_v2.logbroker.producer.LogbrokerPublishingService
import ru.yandex.market.logistics.yard_v2.logbroker.producer.LogbrokerTopic
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShardId


@Import(ConsumerConfig::class)
class PublishClientStateChangeEventConsumerTest(
    @Autowired private val consumer: PublishClientStateChangeEventConsumer,
    @Autowired private val logbrokerPublishingService: LogbrokerPublishingService,
) : AbstractSecurityMockedContextualTest() {


    @Test
    @DatabaseSetup(value = ["classpath:fixtures/dbqueue/consumer/publish-client-state-change-event/before.xml"])
    fun executeSuccessfullyTest() {

        val taskPayload = Task.builder<PublishClientStateChangeEventPayload>(QueueShardId("")).withPayload(
            PublishClientStateChangeEventPayload(1L, 1000L, 2000L)
        ).build()

        consumer.execute(taskPayload)

        val captor = argumentCaptor<YardClientStateChangeEvent>()

        Mockito.verify(logbrokerPublishingService)
            .publish(eq(LogbrokerTopic.YARD_CLIENT_STATE_CHANGE_EVENTS), captor.capture())

        assertions().assertThat(captor.firstValue.clientId).isEqualTo(1)
        assertions().assertThat(captor.firstValue.externalClientId).isEqualTo("extClient")
        assertions().assertThat(captor.firstValue.serviceId).isEqualTo(1)
        assertions().assertThat(captor.firstValue.stateFromId).isEqualTo(1000L)
        assertions().assertThat(captor.firstValue.stateFromName).isEqualTo("state")
        assertions().assertThat(captor.firstValue.stateToId).isEqualTo(2000L)
        assertions().assertThat(captor.firstValue.stateToName).isEqualTo("state2")

    }

}
