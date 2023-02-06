package ru.yandex.market.wms.core.service.impl

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.jms.core.JmsTemplate
import ru.yandex.market.wms.common.model.enums.UserActivityStatus
import ru.yandex.market.wms.common.spring.IntegrationTest
import ru.yandex.market.wms.common.spring.dto.IndirectActivityDto

internal class IndirectActivityQueueProducerTest(
    @Autowired @SpyBean private val jmsTemplate: JmsTemplate,
    @Autowired private val producer: IndirectActivityQueueProducer
) : IntegrationTest() {

    @BeforeEach
    fun beforeEach() {
        Mockito.reset(jmsTemplate)
    }

    @Test
    fun produce() {
        Mockito.doNothing().`when`(jmsTemplate).convertAndSend(
            Mockito.any(String::class.java),
            Mockito.any(IndirectActivityDto::class.java)
        )

        producer.produce("assigner", listOf("user", "user2"), "activity", UserActivityStatus.IN_PROCESS, null)

        Mockito.verify(jmsTemplate, Mockito.times(2)).convertAndSend(
            Mockito.any(String::class.java),
            Mockito.any(IndirectActivityDto::class.java)
        )
    }

    @Test
    fun `produce when validation error then do not send message`() {
        Mockito.doNothing().`when`(jmsTemplate).convertAndSend(
            Mockito.any(String::class.java),
            Mockito.any(IndirectActivityDto::class.java)
        )

        producer.produce("assigner", listOf("user", ""), "activity", UserActivityStatus.IN_PROCESS, null)

        Mockito.verify(jmsTemplate, Mockito.times(1)).convertAndSend(
            Mockito.any(String::class.java),
            Mockito.any(IndirectActivityDto::class.java)
        )
    }
}
