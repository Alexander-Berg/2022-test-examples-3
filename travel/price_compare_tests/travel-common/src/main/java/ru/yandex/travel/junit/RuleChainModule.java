package ru.yandex.travel.junit;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import java.util.Set;

/**
 * @author Artem Eroshenko <erosenkoam@me.com>
 */
public class RuleChainModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    public RuleChain provideRuleChain(Set<TestRule> rules) {
        RuleChain ruleChain = RuleChain.emptyRuleChain();
        for (TestRule rule : rules) {
            ruleChain = ruleChain.around(rule);
        }
        return ruleChain;
    }
}
