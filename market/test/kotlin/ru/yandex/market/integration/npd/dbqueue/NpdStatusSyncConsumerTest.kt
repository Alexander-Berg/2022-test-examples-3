package ru.yandex.market.integration.npd.dbqueue

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.integration.npd.AbstractFunctionalTest
import ru.yandex.market.mbi.open.api.client.model.ApiError
import ru.yandex.market.mbi.open.api.client.model.PartnerAppNpdStatus
import ru.yandex.market.mbi.open.api.client.model.PartnerNpdRequest
import ru.yandex.market.mbi.open.api.exception.MbiOpenApiClientResponseException
import ru.yoomoney.tech.dbqueue.config.QueueService
import java.time.Duration

class NpdStatusSyncConsumerTest: AbstractFunctionalTest() {

    @Autowired
    private lateinit var queueService: QueueService

    @BeforeEach
    fun before() {
        Mockito.reset(client)
    }

    @Test
    @DbUnitDataSet(
        before = ["NpdStatusSyncConsumerTest.before.csv"],
        after = ["NpdStatusSyncConsumerTest.testOk.after.csv"]
    )
    fun testConsumerOk() {
        queueService.start(DbQueueTaskType.NPD_STATUS_SYNC.queueId)
        queueService.awaitTermination(DbQueueTaskType.NPD_STATUS_SYNC.queueId, Duration.ofSeconds(3))
        Mockito.verify(mbiOpenApiClient).saveNpdSelfEmployed(
            Mockito.eq(222L),
            Mockito.anyLong(),
            Mockito.eq(PartnerNpdRequest().inn("123456789").checkResult(PartnerAppNpdStatus.DONE))
        )
    }

    @Test
    @DbUnitDataSet(
        before = ["NpdStatusSyncConsumerTest.before.csv"],
        after = ["NpdStatusSyncConsumerTest.testFail.after.csv"]
    )
    fun testConsumerFail() {
        Mockito.`when`(
            mbiOpenApiClient.saveNpdSelfEmployed(
                Mockito.anyLong(),
                Mockito.anyLong(),
                Mockito.any(PartnerNpdRequest::class.java)
            )
        ).thenThrow(MbiOpenApiClientResponseException(
            "Panic",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ApiError().code(HttpStatus.INTERNAL_SERVER_ERROR.value()).message("Panic"))
        )

        queueService.start(DbQueueTaskType.NPD_STATUS_SYNC.queueId)
        queueService.awaitTermination(DbQueueTaskType.NPD_STATUS_SYNC.queueId, Duration.ofSeconds(1))
    }
}
