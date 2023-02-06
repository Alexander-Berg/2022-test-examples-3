package ru.yandex.market.mbi.mbidwh.dataaggregator;

import org.mockito.Mock;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.mbi.mbidwh.common.config.yt.YtConfig;

@Configuration
public class CommonUnitTestConfig {
    @Mock
    public YtConfig.YqlNamedParameterJdbcTemplateWrapper yqlNamedParameterJdbcTemplateWrapper;
}
