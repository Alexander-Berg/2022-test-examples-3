package ru.yandex.market.pers.shopinfo.test.context;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.core.database.EmbeddedPostgresConfig;

/**
 * @author stani on 16.02.18.
 */
@Configuration
@Import({SolomonTestJvmConfig.class,
        EatsAndLavkaYtDaoTestConfig.class,
        EmbeddedPostgresConfig.class})
public class FunctionalTestConfig {

}
