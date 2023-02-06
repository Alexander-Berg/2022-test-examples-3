package ru.yandex.market.logistics.mqm.queue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ReturnStatus
import ru.yandex.market.logistics.mqm.queue.dto.LrmReturnStatusChangedDto
import java.time.Instant

class LrmReturnStatusChangedConsumerTest : AbstractContextualTest() {
    @Autowired
    lateinit var consumer: LrmReturnStatusChangedConsumer

    @Test
    @DatabaseSetup("/queue/consumer/before/lrm_return_status_changed/success.xml")
    @ExpectedDatabase(
        "/queue/consumer/after/lrm_return_status_changed/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun success() {
        consumer.processPayload(
            LrmReturnStatusChangedDto(
                lrmReturnId = 1,
                ReturnStatus.CANCELLED,
                DEFAULT_TIME.plusSeconds(1)
            )
        )
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-10-05T10:00:00.00Z")
    }
}
