package ru.yandex.direct.internaltools.configuration;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.internaltools.core.BaseInternalTool;
import ru.yandex.direct.internaltools.core.annotations.tool.HideInProduction;
import ru.yandex.direct.web.configuration.DirectWebTest;

import static org.assertj.core.api.Assertions.assertThat;

@DirectWebTest
@RunWith(SpringRunner.class)
public class InternalToolsConfigurationTest {
    @Autowired
    ApplicationContext applicationContext;

    @Test
    public void testToolsHidden() {
        InternalToolsConfiguration configuration = applicationContext.getBean(InternalToolsConfiguration.class);
        Collection<BaseInternalTool> tools = applicationContext.getBeansOfType(BaseInternalTool.class).values();

        assertThat(tools.stream()
                .anyMatch(baseInternalTool -> baseInternalTool.getClass().isAnnotationPresent(HideInProduction.class)))
                .isTrue();
        tools = configuration.hideTestTools(tools);
        assertThat(tools.stream()
                .anyMatch(baseInternalTool -> baseInternalTool.getClass().isAnnotationPresent(HideInProduction.class)))
                .isFalse();
    }
}
