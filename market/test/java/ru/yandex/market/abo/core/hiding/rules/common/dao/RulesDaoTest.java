package ru.yandex.market.abo.core.hiding.rules.common.dao;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.core.hiding.rules.common.CompositeHidingRule;
import ru.yandex.market.abo.core.hiding.rules.common.OfferHidingRuleType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RulesDaoTest {
    @Test
    public void testOfRuleType() {
        Stream.of(OfferHidingRuleType.values()).forEach(type ->
                assertNotNull(RulesDao.ofRuleType(type), "No DAO for " + type));
    }

    @Test
    public void testAggregateWithNoSecondaryValues() {
        RulesDao<Long> dao = ExclusiveRulesDao.MODEL_EXCEPT_SHOP;
        RuleRow<Long> row = new RuleRow<>(1, null);
        List<CompositeHidingRule<Long>> aggregated = dao.aggregate(Collections.singletonList(row));
        assertEquals(1, aggregated.size());
        assertEquals(1L, aggregated.get(0).getPrimaryValue().longValue());
        assertEquals(Collections.emptySet(), aggregated.get(0).getSecondaryValues());
    }

}
