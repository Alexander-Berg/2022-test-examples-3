package ru.yandex.market.mbi.orderservice.tms.service.monitoring

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.retry.support.RetryTemplate
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.service.pg.EnvironmentService
import ru.yandex.market.mbi.orderservice.tms.service.yt.YtClusterLivenessProbe
import ru.yandex.yql.YqlDataSource
import ru.yandex.yql.settings.YqlProperties

@Disabled
class OrderAndReturnConsistencyValidatorIntegrationTest : FunctionalTest() {

    @Autowired
    lateinit var environmentService: EnvironmentService

    @Autowired
    lateinit var retryTemplate: RetryTemplate

    @Test
    @Disabled
    fun `verify validate from real yt`() {
        val dataSource = YqlDataSource(
            "jdbc:yql://yql.yandex.net:443",
            YqlProperties().apply {
                user = realUser()
                password = realToken()
                syntaxVersion = 1
            }
        )

        val ytJdbcTemplate = NamedParameterJdbcTemplate(dataSource)

        val validator = OrderAndReturnConsistencyValidator(
            environmentService,
            checkouterReturnPath = "//home/market/testing/mbi/order-service/returns/checkouter_return",
            checkouterReturnLinePath = "//home/market/testing/mbi/order-service/returns/checkouter_return_line",
            logisticReturnLinePath = "//home/market/testing/mbi/order-service/returns/logistic_return_line",
            returnLinePath = "//home/market/testing/mbi/order-service/returns/return_line",
            orderLinesPath = "//home/market/testing/mbi/order-service/order_lines",
            ordersPath = "//home/market/testing/mbi/order-service/orders",
            ytJdbcTemplate,
            YtClusterLivenessProbe(
                ytJdbcTemplate,
                retryTemplate, listOf(
                    YtClusterLivenessProbe.YtCluster.SENECA_VLA,
                    YtClusterLivenessProbe.YtCluster.SENECA_SAS
                )
            ),
            realUser(),
            realToken()
        )

        validator.doJob(null)
    }

    private fun realUser(): String {
        return "yout login"
    }

    private fun realToken(): String {
        return "your token"
    }
}
