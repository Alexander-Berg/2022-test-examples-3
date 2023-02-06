package ru.yandex.market.tpl.core.task.defaults;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.task.flow.Context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author sekulebyakin
 */
@ExtendWith(MockitoExtension.class)
public class TaskDefaultsUnitTest {

    @Mock
    private ConfigurationProviderAdapter configurationProviderAdapter;

    @InjectMocks
    private TaskDefaults taskDefaults;

    @Test
    void configurationFlagPreconditionUnitTest() {
        var preconditionTrue = taskDefaults.configurationFlag(ConfigurationProperties.CORE_TASK_V2_ENABLED, true);
        var preconditionFalse = taskDefaults.configurationFlag(ConfigurationProperties.CORE_TASK_V2_ENABLED, false);
        var context = mock(Context.class);

        doReturn(false)
                .when(configurationProviderAdapter).isBooleanEnabled(ConfigurationProperties.CORE_TASK_V2_ENABLED);
        assertThat(preconditionTrue.test(context)).isFalse();
        assertThat(preconditionFalse.test(context)).isTrue();

        doReturn(true)
                .when(configurationProviderAdapter).isBooleanEnabled(ConfigurationProperties.CORE_TASK_V2_ENABLED);
        assertThat(preconditionTrue.test(context)).isTrue();
        assertThat(preconditionFalse.test(context)).isFalse();
    }

}
