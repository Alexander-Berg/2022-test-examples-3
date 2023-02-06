package ru.yandex.market.mbi.orderservice.tms.persistence.repository.yt

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.retry.support.RetryTemplate
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.InvalidLogisticEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.InvalidLogisticEventKey
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.DLQRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.service.pg.EnvironmentService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.TableBindingHolder
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.tms.service.monitoring.LRMReturnsFlowMonitoring
import ru.yandex.market.mbi.orderservice.tms.service.yt.YtClusterLivenessProbe
import ru.yandex.yql.YqlDataSource
import ru.yandex.yql.settings.YqlProperties

class LrmReturnsFlowMonitoringTest : FunctionalTest() {

    @Autowired
    lateinit var retryTemplate: RetryTemplate

    @Autowired
    lateinit var environmentService: EnvironmentService

    @Autowired
    lateinit var logisticDlqRepository: DLQRepository<InvalidLogisticEventKey, InvalidLogisticEventEntity>

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var tableBindingHolder: TableBindingHolder

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

        val logisticDao = LRMReturnFlowMonitoringDao(
            "//home/cdc/test/market/logistics_lrm/return",
            "//home/cdc/test/market/logistics_lrm/return_event",
            ytClusterLivenessProbe = YtClusterLivenessProbe(
                ytJdbcTemplate,
                retryTemplate, listOf(YtClusterLivenessProbe.YtCluster.HAHN, YtClusterLivenessProbe.YtCluster.ARNOLD)
            ),
            ytJdbcTemplate
        )

        val monitoring = LRMReturnsFlowMonitoring(
            logisticDlqRepository,
            orderRepository,
            logisticDao,
            environmentService,
            tableBindingHolder
        )

        monitoring.doJob(null)
    }

    private fun realUser(): String {
        return "robot-market-mbi-ts"
    }

    private fun realToken(): String {
        return "your token"
    }
}
