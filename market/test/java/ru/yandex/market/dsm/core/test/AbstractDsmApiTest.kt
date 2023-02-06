package ru.yandex.market.dsm.core.test

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import ru.yandex.market.dsm.config.DbConfiguration
import ru.yandex.market.dsm.config.DbQueueConfiguration
import ru.yandex.market.dsm.config.MockConfiguration
import ru.yandex.market.dsm.config.TestConfiguration
import ru.yandex.market.dsm.test.CleanupAfterEachExtension
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig
import ru.yandex.market.starter.postgres.config.EmbeddedPostgresConfiguration
import ru.yandex.market.starter.tvm.factory.TvmClientSettings
import ru.yandex.passport.tvmauth.CheckedServiceTicket
import ru.yandex.passport.tvmauth.TicketStatus
import ru.yandex.passport.tvmauth.TvmClient

@ExtendWith(CleanupAfterEachExtension::class)
@SpringBootTest(
    classes = [
        SpringApplicationConfig::class,
        EmbeddedPostgresConfiguration::class,
        DbQueueConfiguration::class,
        DbConfiguration::class,
        TestConfiguration::class,
        MockConfiguration::class
    ]
)
@AutoConfigureMockMvc(printOnlyOnFailure = false)
@TestPropertySource(
    properties =
    [
        "mj.tvm.clientsTvmDisabled=true",
        "mj.tvm.serverTvmDisabled=true",
    ]
)
abstract class AbstractDsmApiTest : AbstractTest() {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var tvmClient: TvmClient

    // дебаг jdbcTemplate.getDataSource().getConnection().getMetaData().getURL()
    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    val TEST_USER_UID = 1L

    @BeforeEach
    fun setUp() {
        Mockito.`when`(tvmClient.checkServiceTicket(any()))
            .thenReturn(CheckedServiceTicket(
                TicketStatus.OK,
                "test CheckedServiceTicket",
                TvmClientSettings.LOCAL_TVM_ID,
                TEST_USER_UID
            ))
    }
}
