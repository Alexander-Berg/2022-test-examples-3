package ru.yandex.direct.manualtests.tasks.classdiff;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import ru.yandex.direct.manualtests.configuration.BaseConfiguration;

@Configuration
@ComponentScan(
        basePackages = "ru.yandex.direct.manualtests.tasks.classdiff",
        excludeFilters = {
                @ComponentScan.Filter(value = Configuration.class, type = FilterType.ANNOTATION),
        }
)

public class ClassDiffConfiguration extends BaseConfiguration {
}
