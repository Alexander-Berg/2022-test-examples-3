package ru.yandex.market.mbi.orderservice.tms.service.yt.jobs

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.orderservice.common.enum.OrderSourcePlatform.OZON
import ru.yandex.market.mbi.orderservice.common.model.yt.LongWrapper
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderSparseIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.ProcessedCheckouterEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.StringWrapper
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderSparseIndexRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.ProcessedCheckouterEventRepository
import ru.yandex.market.mbi.orderservice.common.util.UTC_ZONE_ID
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.temporal.ChronoUnit

@CleanupTables(
    [OrderEntity::class, ProcessedCheckouterEventEntity::class]
)
class SparseIndexBuildJobTest : FunctionalTest() {

    @Autowired
    lateinit var sparseIndexBuildJob: SparseIndexBuildJob

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var sparseIndexRepository: OrderSparseIndexRepository

    @Autowired
    lateinit var processedEventsRepository: ProcessedCheckouterEventRepository

    @Test
    fun test() {
        // given
        val lastEventTimestamp = Instant.now().minusSeconds(300)
        processedEventsRepository.insertRow(
            ProcessedCheckouterEventEntity(
                eventId = LongWrapper(1),
                eventTimestamp = lastEventTimestamp.toEpochMilli(),
                processingTimestamp = lastEventTimestamp
            )
        )
        orderEntityRepository.insertRows(
            listOf(
                OrderEntity(
                    key = OrderKey(
                        partnerId = 777,
                        orderId = 25
                    ),
                    createdAt = Instant.now().truncatedTo(ChronoUnit.DAYS).minusSeconds(5)
                ),
                // не попадет в выборку по времени
                OrderEntity(
                    key = OrderKey(
                        partnerId = 777,
                        orderId = 40
                    ),
                    createdAt = Instant.now()
                ),
                // не попадет в выборку, т.к. FaaS-заказ
                OrderEntity(
                    key = OrderKey(
                        partnerId = 888,
                        orderId = 1040
                    ),
                    createdAt = Instant.now().truncatedTo(ChronoUnit.DAYS).minusSeconds(5),
                    sourcePlatform = OZON
                )
            )
        )

        // when
        sparseIndexBuildJob.doJob(null)

        // then
        // проверяем, что создалась запись за предыдущий день
        val expectedDate = LocalDate.now(UTC_ZONE_ID).atStartOfDay().format(ISO_LOCAL_DATE)
        val indexEntry = sparseIndexRepository.lookupRow(
            StringWrapper(expectedDate)
        )
        assertThat(indexEntry).isEqualTo(
            OrderSparseIndex(
                date = StringWrapper(expectedDate),
                minCreatedOrderId = 25,
                minDeliveredOrderId = -1
            )
        )
    }
}
