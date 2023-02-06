package ru.yandex.market.abo.core.hiding.rules.white;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.util.entity.DeletableEntityService;
import ru.yandex.market.abo.util.entity.DeletableEntityServiceTest;

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 30.01.2020
 */
class WhiteOfferHidingRuleServiceTest extends DeletableEntityServiceTest<WhiteOfferHidingRule, Long> {

    private static final String TEST_OGRN = "1234567890123";
    private static final String TEST_COMMENT = "comment";

    @Autowired
    private WhiteOfferHidingRuleService whiteOfferHidingRuleService;

    @Override
    protected DeletableEntityService<WhiteOfferHidingRule, Long> service() {
        return whiteOfferHidingRuleService;
    }

    @Override
    protected Long extractId(WhiteOfferHidingRule entity) {
        return entity.getId();
    }

    @Override
    protected WhiteOfferHidingRule newEntity() {
        return new WhiteOfferHidingRule(TEST_OGRN, TEST_COMMENT);
    }

    @Override
    protected WhiteOfferHidingRule example() {
        return new WhiteOfferHidingRule();
    }
}
