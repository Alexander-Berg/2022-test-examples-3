package ru.yandex.market.logistics.cte.dbqueue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.github.springtestdbunit.annotation.ExpectedDatabase
import com.github.springtestdbunit.assertion.DatabaseAssertionMode
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import ru.yandex.market.logistics.cte.base.IntegrationTest
import ru.yandex.market.logistics.cte.dbqueue.payload.AscEnrichmentPayload
import ru.yandex.market.logistics.cte.entity.asc.GuaranteePeriodDTO
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.config.QueueShard
import ru.yandex.startrek.client.Session
import java.time.Clock
import java.time.ZonedDateTime

class AscEnrichGuaranteePeriodConsumerTest(
        @Autowired private val ascEnrichGuaranteePeriodConsumer: AscEnrichGuaranteePeriodConsumer,
        @Autowired private val queueShard: QueueShard,
        @Autowired private val clock: Clock,
        @Autowired private val session: Session
) : IntegrationTest() {

    @BeforeEach
    fun init() {
        Mockito.reset(session)
    }

    @Autowired
    @Qualifier("yqlJdbcTemplate")
    private lateinit var yqlJdbcTemplate: JdbcTemplate


    @Test
    @DatabaseSetup(
            value = ["classpath:/dbqueue/consumer/asc-enrich-guarantee-period-task/before.xml"]
    )
    @ExpectedDatabase(
            value = "classpath:dbqueue/consumer/asc-enrich-guarantee-period-task/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    fun shouldSuccessfullyEnrichGuaranteePeriod() {

        whenever(yqlJdbcTemplate.query(any<String>(), any<RowMapper<*>>()))
                .thenReturn(
                        listOf(GuaranteePeriodDTO(101, "shopSku1", 2, "YEAR"),
                               GuaranteePeriodDTO(101, "shopSku2", 3, "MONTH"),
                               GuaranteePeriodDTO(101, "shopSku3", 4, "WEEK")),
                )

        val task = Task.builder<AscEnrichmentPayload>(queueShard.shardId)
                .withCreatedAt(ZonedDateTime.now(clock))
                .withPayload(AscEnrichmentPayload(listOf(1L, 2L, 3L), 1L))
                .build()
        ascEnrichGuaranteePeriodConsumer.execute(task)
    }
}
