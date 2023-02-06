package ru.yandex.market.transferact.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.market.logistics.les.DiscrepancyActGeneratedEvent
import ru.yandex.market.transferact.sqs.SqsSender

@Configuration
class MockLesClientConfiguration {

    @Bean
    fun discrepancyActSender(): SqsSender<DiscrepancyActGeneratedEvent> {
        return object : SqsSender<DiscrepancyActGeneratedEvent> {
            override fun sendSilently(payload: DiscrepancyActGeneratedEvent, eventType: String) {
                val log: Logger = LoggerFactory.getLogger(this.javaClass)

                fun sendSilently(payload: DiscrepancyActGeneratedEvent, eventType: String) {
                    log.info("Event {} was sent!", payload)
                }
            }
        }
    }
}
