package ru.yandex.direct.oneshot.core.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.dbutil.testing.DbUtilTestingConfiguration;

@Configuration
@Import({OneshotCoreConfiguration.class, DbUtilTestingConfiguration.class})
public class OneshotCoreTestingConfiguration {
}
