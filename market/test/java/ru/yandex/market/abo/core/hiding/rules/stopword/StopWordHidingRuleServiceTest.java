package ru.yandex.market.abo.core.hiding.rules.stopword;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.core.export.hidden.StopWordsHidingRulesProcessorTest;
import ru.yandex.market.abo.core.hiding.rules.stopword.model.StopWordHidingRule;
import ru.yandex.market.abo.util.entity.DeletableEntityService;
import ru.yandex.market.abo.util.entity.DeletableEntityServiceTest;

/**
 * @author artemmz
 * @date 15.03.18.
 */
public class StopWordHidingRuleServiceTest extends DeletableEntityServiceTest<StopWordHidingRule, String> {
    @Autowired
    private StopWordHidingRuleService stopWordHidingRuleService;

    @Override
    protected DeletableEntityService<StopWordHidingRule, String> service() {
        return stopWordHidingRuleService;
    }

    @Override
    protected String extractId(StopWordHidingRule entity) {
        return entity.getStopWord();
    }

    @Override
    protected StopWordHidingRule newEntity() {
        return StopWordsHidingRulesProcessorTest.initStopWordRule();
    }

    @Override
    protected StopWordHidingRule example() {
        return new StopWordHidingRule();
    }
}