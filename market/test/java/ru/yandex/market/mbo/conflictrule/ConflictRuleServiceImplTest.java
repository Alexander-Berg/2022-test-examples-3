package ru.yandex.market.mbo.conflictrule;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;

/**
 * @author ayratgdl
 * @date 07.11.17
 */
public class ConflictRuleServiceImplTest {
    private static final long CATEGORY_X = 101;
    private static final long CATEGORY_Y = 102;
    private static final long PARAMETER_A = 201;
    private static final long PARAMETER_B = 202;
    private static final long PARAMETER_C = 203;

    private ConflictRuleServiceImpl conflictRuleService;

    @Before
    public void setUp() {
        conflictRuleService = new ConflictRuleServiceImpl();
        conflictRuleService.setConflictRuleDAO(new ConflictRuleDAOMock());
    }

    @Test
    public void getConflictRulesForCategory() throws Exception {
        ConflictRule ruleGAB = buildConflictRule(KnownIds.GLOBAL_CATEGORY_ID, PARAMETER_A, PARAMETER_B);
        ConflictRule ruleXAB = buildConflictRule(CATEGORY_X, PARAMETER_A, PARAMETER_B);
        ConflictRule ruleYBC = buildConflictRule(CATEGORY_Y, PARAMETER_B, PARAMETER_C);

        conflictRuleService.createConflictRule(ruleGAB);
        conflictRuleService.createConflictRule(ruleXAB);
        conflictRuleService.createConflictRule(ruleYBC);

        MatcherAssert.assertThat(conflictRuleService.getConflictRulesForCategory(CATEGORY_X),
                                 Matchers.containsInAnyOrder(ruleXAB));
        MatcherAssert.assertThat(conflictRuleService.getConflictRulesForCategory(CATEGORY_Y),
                                 Matchers.containsInAnyOrder(ruleGAB, ruleYBC));
    }

    private static ConflictRule buildConflictRule(long categoryId, long firstParamId, long secondParamId) {
        return new ConflictRule()
            .setCategoryId(categoryId)
            .setFirstParamId(firstParamId)
            .setSecondParamId(secondParamId)
            .setScope(ConflictRule.OfferScope.ALL_OFFER)
            .setEnumerationType(ConflictRule.EnumerationType.ALL_EXCEPT_LISTED);
    }
}
