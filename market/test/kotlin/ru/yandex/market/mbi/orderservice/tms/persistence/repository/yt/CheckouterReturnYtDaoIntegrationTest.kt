package ru.yandex.market.mbi.orderservice.tms.persistence.repository.yt

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.retry.support.RetryTemplate
import ru.yandex.market.mbi.orderservice.common.util.toInstantAtMoscowTime
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.tms.service.yt.YtClusterLivenessProbe
import ru.yandex.yql.YqlDataSource
import ru.yandex.yql.settings.YqlProperties
import java.time.LocalDateTime
import kotlin.streams.toList

class CheckouterReturnYtDaoIntegrationTest : FunctionalTest() {

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

        val checkouterReturnYtDao = CheckouterReturnYtDao(
            "home/market/production/checkouter/testing/cdc/checkouter_main/return",
            "home/market/production/checkouter/testing/cdc/checkouter_main/return_item",
            ytClusterLivenessProbe = YtClusterLivenessProbe(
                ytJdbcTemplate,
                retryTemplate, listOf(YtClusterLivenessProbe.YtCluster.HAHN, YtClusterLivenessProbe.YtCluster.ARNOLD)
            ),
            ytJdbcTemplate
        )

        val result = checkouterReturnYtDao.readCheckouterReturnsAsStream(
            CheckouterReturnImportFilter(
                fromUpdatedAt = LocalDateTime.of(2021, 12, 12, 12, 12, 12).toInstantAtMoscowTime(),
                toUpdatedAt = LocalDateTime.of(2022, 1, 1, 1, 1, 1).toInstantAtMoscowTime()
            )
        ).toList()

        println(result)
    }

    private fun realUser(): String {
        return "robot-market-mbi-ts"
    }

    private fun realToken(): String {
        return "put your token"
    }
}
