package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import freemarker.template.Configuration
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.queue.base.QueueRegister
import ru.yandex.market.logistics.mqm.repository.MonitoringEventProcessorRepository
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class LomConsumerTest : AbstractContextualTest() {
    @Autowired
    protected lateinit var queueRegister: QueueRegister

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var processorRepository: MonitoringEventProcessorRepository

    @Autowired
    protected lateinit var freemarkerConfiguration: Configuration

    @BeforeEach
    fun prepare() {
        clock.setFixed(Instant.parse("2021-09-09T10:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    fun description(description: String) = description.format(DATE_TIME)

    fun tags(tags: List<String>): Map<String, Any> {
        val items = mutableMapOf<String, Any>()
        items["tags"] = tags
        return items
    }

    companion object {
        val TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        const val QUEUE = "MQMU"
        const val LOM_COMPONENT = 49000L
        const val EXPRESS_TAG = "EXPRESS"
        const val BARCODE = "barcode-1"
        const val PARTNER_ID = 1001L
        const val PARTNER_NAME = "PN"
        const val ERROR_CODE = 10
        const val ERROR_MESSAGE = "EM"
        const val PARTNER_TYPE = "DS"
        const val SEGMENT_ID = 301L
        const val EXPRESS = true
        const val LOM_ORDER_ID = 101L
        const val CAUSE = "cause"
        val INSTANT: Instant = LocalDateTime
            .of(2021, 9, 9, 17, 0, 0)
            .atZone(DateTimeUtils.MOSCOW_ZONE)
            .toInstant()
        const val BUSINESS_PROCESS_ID = 3023L
        val DATE_TIME: String = LocalDateTime.ofInstant(INSTANT, DateTimeUtils.MOSCOW_ZONE).format(TIME_FORMATTER)
    }
}
