package ru.yandex.market.logistics.config.quartz.hostcontextaware;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.quartz.QuartzAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;

import ru.yandex.market.logistics.test.integration.db.DbUnitTestConfiguration;
import ru.yandex.market.logistics.test.integration.db.zonky.EnableZonkyEmbeddedPostgres;

@SpringBootConfiguration
@Import({
    HostContextAwareConfiguration.class,
    DbUnitTestConfiguration.class,
    QuartzAutoConfiguration.class,
})
@EnableZonkyEmbeddedPostgres
@EnableCaching
@MockBean(DatabaseMasterEnvironmentPredicate.class)
public class ContextualTestConfiguration {
}
