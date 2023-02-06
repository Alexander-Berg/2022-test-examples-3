package ru.yandex.market.jmf.module.automation.test;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.module.automation.AutomationRuleExecuteFilterStrategy;
import ru.yandex.market.jmf.module.automation.EventAutomationRule;

@ContextConfiguration(classes = AutomationRuleExecuteFilterStrategyTest.TestConfiguration.class)
public class AutomationRuleExecuteFilterStrategyTest extends AbstractAutomationRuleTest {
    @Inject
    AutomationRuleExecuteFilterStrategy filterStrategy;

    @Test
    public void filterUsed() {
        Entity entity1 = bcpService.create(FQN_1, Map.of());
        Entity entity2 = bcpService.create(FQN_2, Map.of());

        EventAutomationRule rule =
                createApprovedEventRule(entity2, "/test/jmf/module/automation/rules/event/eqAttributePredicate.json");
        startTrigger(entity1, entity2);

        Mockito.verify(filterStrategy, Mockito.atLeastOnce())
                .shouldAllowRule(Mockito.eq(rule), Mockito.any());
        Mockito.verify(filterStrategy, Mockito.atLeastOnce())
                .shouldAllowRuleCondition(Mockito.eq(rule), Mockito.any(), Mockito.any());
    }

    @Configuration
    public static class TestConfiguration {
        @Primary
        @Bean
        public AutomationRuleExecuteFilterStrategy mockFilterStrategy() {
            var mock = Mockito.mock(AutomationRuleExecuteFilterStrategy.class);
            Mockito.when(mock.isApplicable(Mockito.any())).thenReturn(true);
            Mockito.when(mock.shouldAllowRule(Mockito.any(), Mockito.any())).thenReturn(true);
            Mockito.when(mock.shouldAllowRuleCondition(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);
            return mock;
        }
    }
}
