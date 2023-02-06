package ru.yandex.market.logistics.yard.config.logbroker

import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.ff.client.dto.RequestStatusChangesDto
import ru.yandex.market.logbroker.consumer.util.LbParser
import ru.yandex.market.logistics.yard.base.AbstractSecurityMockedContextualTest
import ru.yandex.market.logistics.yard.util.FileContentUtils

class RequestStatusHandlerConsumerTest(
    @Autowired private val requestStatusHandlerConsumer: RequestStatusHandlerConsumer,
    @Autowired private val requestStatusDtoLbParser: LbParser<RequestStatusChangesDto>,
) : AbstractSecurityMockedContextualTest() {

    @Test
    @ExpectedDatabase(
        value = "classpath:fixtures/config/logbroker/after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun successfulProcessing() {
        val requests = FileContentUtils.getFileContent(
            "classpath:fixtures/config/logbroker/requests.json"
        )
        val requestStatusChangesDto =
            requestStatusDtoLbParser.parseLine(FfwfEventConsumerConfiguration.ENTITY_NAME, requests)

        requestStatusHandlerConsumer.accept(listOf(requestStatusChangesDto))
    }
}
