package ru.yandex.market.logistics.yard.service.event

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import ru.yandex.market.logistics.yard.model.logbroker.TestPayload
import java.util.function.Consumer
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
@Component
class TestConsumer : Consumer<List<TestPayload>> {
    override fun accept(testPayloads: List<TestPayload>) {
        log.info("Message received: $testPayloads")
    }

    companion object {
        private val log = LoggerFactory.getLogger(TestConsumer::class.java)
    }
}
