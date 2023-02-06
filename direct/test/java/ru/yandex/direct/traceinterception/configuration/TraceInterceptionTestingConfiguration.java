package ru.yandex.direct.traceinterception.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.dbutil.testing.DbUtilTestingConfiguration;

@Configuration
@Import({TraceInterceptionConfiguration.class, DbUtilTestingConfiguration.class})
public class TraceInterceptionTestingConfiguration {
}



