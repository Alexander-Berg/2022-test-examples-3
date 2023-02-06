package ru.yandex.market.doctor.session.exporter

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import ru.yandex.market.doctor.config.DatabaseConfig
import ru.yandex.market.doctor.config.JacksonConfig
import ru.yandex.market.doctor.config.TestYtConfig
import ru.yandex.market.doctor.session.SessionConfig
import ru.yandex.market.doctor.session.request.SessionRequestConfig

@TestConfiguration
@Import(
    DatabaseConfig::class,
    JacksonConfig::class,
    SessionConfig::class,
    SessionRequestConfig::class,
    TestYtConfig::class,
)
open class TestSessionExporterConfig(
    private val databaseConfig: DatabaseConfig,
    private val jacksonConfig: JacksonConfig,
    private val sessionConfig: SessionConfig,
    private val sessionRequestConfig: SessionRequestConfig,
    private val testYtConfig: TestYtConfig,
) : AbstractSessionExporterConfig() {
    @Bean
    override fun sessionYtExporter(): SessionYtExporter {
        return SessionYtExporter(
            testYtConfig.ytClientMock(),
            databaseConfig.transactionHelper(),
            sessionConfig.sessionRepository(),
            sessionRequestConfig.sessionRequestRepository(),
            jacksonConfig.objectMapper(),
            SESSION_STATS_TABLE,
            1,
        )
    }

    companion object {
        const val SESSION_STATS_TABLE = "//session_stats_table"
    }
}
