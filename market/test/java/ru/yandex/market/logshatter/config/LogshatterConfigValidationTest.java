package ru.yandex.market.logshatter.config;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;
import ru.yandex.market.logshatter.config.ddl.UpdateDDLService;
import ru.yandex.market.logshatter.config.validation.ConfigurationValidationState;

import static org.mockito.Mockito.when;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 06.12.16
 */
@RunWith(MockitoJUnitRunner.class)
public class LogshatterConfigValidationTest {
    private ConfigurationService configurationService;
    private List<LogShatterConfig> configs;

    @Mock
    private UpdateDDLService updateDDLServiceMock;

    @Before
    public void setUp() {
        configurationService = TestServiceBuilder.buildConfigurationService();
        configurationService.setUpdateDDLService(updateDDLServiceMock);
        when(updateDDLServiceMock.getClickhouseDdlService()).thenReturn(TestServiceBuilder.buildDdlService());
        configs = Collections.unmodifiableList(configurationService.readConfigurationFromFiles());
    }

    @Test
    public void validateConfigs() {
        configurationService.setConfigs(configs);
        ConfigurationValidationState validationResult = configurationService.validateConfigs(configs);
        Preconditions.checkState(validationResult.allConfigsAreValid());
        Preconditions.checkState(validationResult.getValidConfigs().size() == configs.size());
    }

}
