package ru.yandex.market.loyalty.core.rule;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.lightweight.EnumUtils;
import ru.yandex.market.loyalty.lightweight.GenericType;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;

public class RulesValidationTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private ApplicationContext applicationContext;
    private static final Collection<RuleType<?>> allRuleTypes = EnumUtils.createCache(
            new GenericType<RuleType<?>>() {
            },
            RuleType::getBeanName
    )
            .values();

    @Test
    public void validateRulesWithParams() {
        List<RuleType<?>> notPrototypesButWithState = allRuleTypes
                .stream()
                .filter(ruleType -> RuleWithParams.class.isAssignableFrom(ruleType.getRuleClass()))
                .filter(ruleType -> !applicationContext.isPrototype(ruleType.getBeanName()))
                .collect(Collectors.toList());

        assertThat(notPrototypesButWithState, is(empty()));
    }

    @Test
    public void invalidRuleTypeReference() {
        List<RuleType<?>> notValidBackwardRuleTypeReference = allRuleTypes
                .stream()
                .filter(ruleType -> applicationContext.getBean(ruleType.getBeanName(), Rule.class).getType() != ruleType)
                .collect(Collectors.toList());

        assertThat(notValidBackwardRuleTypeReference, is(empty()));
    }

    @Test
    public void invalidClassReference() {
        List<RuleType<?>> notValidBackwardClassReference = allRuleTypes
                .stream()
                .filter(ruleType -> applicationContext.getBean(ruleType.getBeanName(), Rule.class).getClass() != ruleType.getRuleClass())
                .collect(Collectors.toList());

        assertThat(notValidBackwardClassReference, is(empty()));
    }
}
