package ru.yandex.market.logistics.mqm.queue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.queue.dto.LomWaybillSegmentStatusDto
import java.time.Instant

@DisplayName("Тест обработки изменения даты доставки заказа")
class LomWaybillSegmentStatusAddedConsumerTest : AbstractContextualTest() {
    @Autowired
    lateinit var consumer: LomWaybillSegmentStatusAddedConsumer

    @Autowired
    private lateinit var transactionOperations: TransactionOperations

    @Test
    @DatabaseSetup("/queue/consumer/before/process_new_waybill_segment_status/setup.xml")
    @ExpectedDatabase(
        value = "/queue/consumer/after/process_new_waybill_segment_status/marked_not_actual.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешный сценарий")
    fun testConsumerLegacy() {
        clock.setFixed(Instant.parse("2021-07-01T07:00:10.00Z"), DateTimeUtils.MOSCOW_ZONE)
        transactionOperations.executeWithoutResult {
            consumer.processPayload(
                LomWaybillSegmentStatusDto(
                    1L,
                    listOf(
                        LomWaybillSegmentStatusDto.StatusDto(
                            id = 2,
                            status = SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT,
                            date = clock.instant()
                        )
                    )
                )
            )
        }
    }

    @Test
    @DatabaseSetup("/queue/consumer/before/process_new_waybill_segment_status/setup.xml")
    @ExpectedDatabase(
        value = "/queue/consumer/after/process_new_waybill_segment_status/marked_not_actual_on_demand.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Успешный сценарий OnDemand")
    fun testConsumerOnDemandLegacy() {
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
        transactionOperations.executeWithoutResult {
            consumer.processPayload(
                LomWaybillSegmentStatusDto(
                    2L,
                    listOf(
                        LomWaybillSegmentStatusDto.StatusDto(
                            id = 3,
                            status = SegmentStatus.TRANSIT_PICKUP,
                            date = clock.instant()
                        )
                    )
                )
            )
        }
    }

    @Test
    @DatabaseSetup("/queue/consumer/before/process_new_waybill_segment_status/setup_express_return.xml")
    @ExpectedDatabase(
        value = "/queue/consumer/after/process_new_waybill_segment_status/express_return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DisplayName("Обработка подозрительной отмены Express")
    fun testConsumerExpressReturn() {
        clock.setFixed(Instant.parse("2021-07-01T07:00:10.00Z"), DateTimeUtils.MOSCOW_ZONE)

        transactionOperations.executeWithoutResult {
            consumer.processPayload(
                LomWaybillSegmentStatusDto(
                    2L,
                    listOf(
                        LomWaybillSegmentStatusDto.StatusDto(
                            id = 1,
                            status = SegmentStatus.RETURN_PREPARING,
                            date = clock.instant()
                        )
                    )
                )
            )
        }
    }
}
