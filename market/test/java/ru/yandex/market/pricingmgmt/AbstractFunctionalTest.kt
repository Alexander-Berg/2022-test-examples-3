package ru.yandex.market.pricingmgmt

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestExecutionListeners
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer
import ru.yandex.market.pricingmgmt.config.ApplicationConfig
import ru.yandex.market.pricingmgmt.config.ApplicationTestConfig
import ru.yandex.market.pricingmgmt.service.TimeService
import ru.yandex.market.yql_test.YqlTestConfiguration
import ru.yandex.market.yql_test.test_listener.YqlPrefilledDataTestListener
import ru.yandex.market.yql_test.test_listener.YqlTestListener
import ru.yandex.mj.generated.OpenAPI2SpringBoot
import ru.yandex.mj.generated.client.self.api.CategoryApiClient

@ExtendWith(SpringExtension::class)
@TestExecutionListeners(
    value = [
        DependencyInjectionTestExecutionListener::class,
        DbUnitTestExecutionListener::class,
        MockitoTestExecutionListener::class,
        YqlTestListener::class,
        YqlPrefilledDataTestListener::class,
        WithSecurityContextTestExecutionListener::class
    ]
)
@ContextConfiguration(initializers = [PGaaSZonkyInitializer::class])
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [
        SpringApplicationConfig::class,
        OpenAPI2SpringBoot::class,
        ApplicationConfig::class,
        YqlTestConfiguration::class
    ],
    properties = ["spring.liquibase.enabled=true"]
)
@TestPropertySource(locations = ["classpath:functional-test.properties"])
@ActiveProfiles("unittest")
@DbUnitDataSet(
    before = ["/test_data.csv"],
    nonTruncatedTables = [
        "pricing_management.databasechangelog",
        "pricing_management.databasechangeloglock"
    ],
    nonRestartedSequences = ["pricing_management.promo_id_seq"]
)
@Import(ApplicationTestConfig::class)
@MockBean(classes = [TimeService::class, CategoryApiClient::class])
abstract class AbstractFunctionalTest {
    init {
        System.setProperty("environment", "local_test")
    }

    @Autowired
    private var jdbcTemplate: JdbcTemplate? = null

    private val sequenceName: String = "promo_id_seq"

    protected fun <T> notNull(): T {
        return Mockito.notNull()
    }

    @BeforeEach
    fun restartSequences() {
        // Корректно рестартуем последовательность
        val sequenceStart = jdbcTemplate!!.queryForObject(
            "select seqstart " +
                "from pg_sequence as s " +
                "inner join pg_class pc on pc.oid = s.seqrelid " +
                "where pc.relname = '$sequenceName'",
            Long::class.java
        )

        jdbcTemplate!!.execute("select setval('$sequenceName', $sequenceStart, false)")
    }
}
