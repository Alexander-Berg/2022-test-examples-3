package ru.yandex.market.mbi.orderservice.tms.persistence.repository.yt

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.retry.support.RetryTemplate
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.tms.service.yt.YtClusterLivenessProbe
import ru.yandex.yql.YqlDataSource
import ru.yandex.yql.settings.YqlProperties
import kotlin.streams.toList

class Logistic4ShopsEventsYtDaoIntegrationTest : FunctionalTest() {

    @Autowired
    lateinit var retryTemplate: RetryTemplate

    @Test
    @Disabled
    fun `verify import from real yt`() {
        val dataSource = YqlDataSource(
            "jdbc:yql://yql.yandex.net:443",
            YqlProperties().apply {
                user = realUser()
                password = realToken()
                syntaxVersion = 1
            }
        )

        val ytJdbcTemplate = NamedParameterJdbcTemplate(dataSource)

        val logisticDao = Logistic4ShopsEventsYtDao(
            "home/cdc/prod/market/logistics_l4s/logistic_event",
            ytClusterLivenessProbe = YtClusterLivenessProbe(
                ytJdbcTemplate,
                retryTemplate, listOf(YtClusterLivenessProbe.YtCluster.HAHN, YtClusterLivenessProbe.YtCluster.ARNOLD)
            ),
            ytJdbcTemplate
        )

        val result = logisticDao.readLogisticEventsAsStream(
            Logistic4ShopsEventsImportFilter(
                fromL4SEventId = 5563,
                toL4SEventId = 5563
            )
        ).toList()[0]

        println(
            """
                $result
                ${result.returnStatusChangedPayload.returnId}
                ${result.returnStatusChangedPayload.returnEventId}
                ${result.returnStatusChangedPayload.orderId}
                ${result.returnStatusChangedPayload.returnStatus}
                ${result.returnStatusChangedPayload.clientReturnId}
                ${result.returnStatusChangedPayload.returnSource}
                ${result.returnStatusChangedPayload.additionalCase}
            """.trimIndent()
        )
    }

    private fun realUser(): String {
        return "robot-market-mbi-ts"
    }

    private fun realToken(): String {
        return "put your token"
    }
}
