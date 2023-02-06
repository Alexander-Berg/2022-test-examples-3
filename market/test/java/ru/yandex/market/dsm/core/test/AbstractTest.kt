package ru.yandex.market.dsm.core.test

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.market.dsm.config.DbConfiguration
import ru.yandex.market.dsm.config.DbQueueConfiguration
import ru.yandex.market.dsm.config.DsmConstants
import ru.yandex.market.dsm.config.MockConfiguration
import ru.yandex.market.dsm.config.TestConfiguration
import ru.yandex.market.dsm.test.CleanupAfterEachExtension
import ru.yandex.market.javaframework.main.config.SpringApplicationConfig
import ru.yandex.market.starter.postgres.config.EmbeddedPostgresConfiguration
import java.time.Clock

@ActiveProfiles(DsmConstants.ENV.FUNCTIONAL_TEST_PROFILE)
@ExtendWith(value = [
    SpringExtension::class,
    CleanupAfterEachExtension::class
])
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    classes = [
        SpringApplicationConfig::class,
        EmbeddedPostgresConfiguration::class,
        DbQueueConfiguration::class,
        DbConfiguration::class,
        TestConfiguration::class,
        MockConfiguration::class
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = [
        "spring.autoconfigure.exclude=ru.yandex.market.starter.quartz.config.MjQuartzAutoConfiguration",
        "quartzStubsEnabled=true",
        "tpl.dbQueue.runQueueLoop=false",
        "spring.jpa.properties.org.hibernate.envers.audit_table_suffix=_history",
        "spring.jpa.properties.org.hibernate.envers.revision_field_name=revision_id",
        "spring.jpa.properties.org.hibernate.envers.revision_type_field_name=revision_type",
        "spring.jpa.properties.org.hibernate.envers.store_data_at_delete=true",
        "spring.jpa.properties.org.hibernate.envers.audit_strategy=org.hibernate.envers.strategy.DefaultAuditStrategy",
        "courier.lobroker.producer.test-email-patterns=tpl-auto-user-.*@yandex.ru",
        "external.balance.url=http://test",
        "external.balance.tvm.applicationId=1"
    ]
)
abstract class AbstractTest {
    @Autowired
    private lateinit var clock: Clock

    @AfterEach
    fun resetClock() {
        ClockUtil.reset(clock)
    }
}
