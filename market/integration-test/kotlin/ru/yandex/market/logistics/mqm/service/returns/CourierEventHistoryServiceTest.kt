package ru.yandex.market.logistics.mqm.service.returns

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.courier.CourierEventHistory
import ru.yandex.market.logistics.mqm.entity.courier.CourierReceivedPickupEventPayload
import ru.yandex.market.logistics.mqm.entity.enums.courier.CourierStatus

class CourierEventHistoryServiceTest : AbstractContextualTest() {

    @Autowired
    private lateinit var service: CourierEventHistoryService

    @Test
    @DatabaseSetup("/service/returns/courier_event_history_service/before/success.xml")
    fun success() {
        assertSoftly {
            service.existsByExternalBoxIdAndStatus("1", CourierStatus.RECEIVED_PICKUP) shouldBe true
            service.existsByExternalBoxIdAndStatus("2", CourierStatus.RECEIVED_PICKUP) shouldBe false
        }
    }

    @Test
    @ExpectedDatabase(
        "/service/returns/courier_event_history_service/after/save.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun save() {
        service.save(
            CourierEventHistory(
                externalBoxId = "ext",
                status = CourierStatus.RECEIVED_PICKUP,
                payload = CourierReceivedPickupEventPayload(
                    sortingCenterId = 1,
                    courier = CourierReceivedPickupEventPayload.CourierDto(
                        id = 1,
                        person = CourierReceivedPickupEventPayload.PersonDto(
                            name = "ricnorr"
                        )
                    )
                )
            )
        )
    }
}
