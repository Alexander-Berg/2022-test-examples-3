package ru.yandex.direct.rbac.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.dbutil.testing.DbUtilTestingConfiguration;

@Configuration
@Import({DbUtilTestingConfiguration.class, RbacConfiguration.class})
public class RbacTestingConfiguration {
}
