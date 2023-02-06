package ru.yandex.market.mbo.export.helper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.mbo.db.params.GLRulesService;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleImpl;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRulePredicate;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleSearchFilter;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleType;
import ru.yandex.market.mbo.utils.SearchableCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author york
 * @since 29.01.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class SizeMeasureExportHelperTest {
    private Long idsSeq = 1L;

    private GLRulesService rulesService;
    private List<GLRule> rules = new ArrayList<>();

    @Before
    public void setUp() {
        rulesService = new GLRulesService() {
            public List<GLRule> searchRules(GLRuleSearchFilter filter) {
                SearchableCache<Long, GLRule> cache = buildInMemoryCache(rules);
                return searchRulesInCache(filter, cache);
            }
        };
    }

    @Test
    public void testLoadConversionRules() {
        Set<Long> paramIds = Sets.newSet(1L, 2L);
        Set<Long> optionIds = LongStream.range(1, 1000L).boxed().collect(Collectors.toSet());
        GLRule diffType = createRule(GLRuleType.MANUAL,
            pr(1L, 1L), pr(2L, 2L));

        GLRule withIf = createRule(GLRuleType.CONVERSION,
            pr(1L, 1L), pr(10L, 1L));

        GLRule withThen = createRule(GLRuleType.CONVERSION,
            pr(10L, 1L), pr(1L, 1L));

        GLRule withDiffValue = createRule(GLRuleType.CONVERSION,
            pr(10L, 1L), pr(2L, 1001L));

        GLRule without = createRule(GLRuleType.CONVERSION,
            pr(10L, 1L), pr(5L, 1L));

        GLRule withIf2 = createRule(GLRuleType.CONVERSION,
            Arrays.asList(pr(10L, 5L), pr(2L, 10L)),
            Arrays.asList(pr(10L, 115L), pr(52L, 101L)));

        rules = Arrays.asList(diffType, withIf, withThen, without, withIf2, withDiffValue);

        MultiMap<Long, GLRule> result = SizeMeasureExportHelper.loadConversionRules(rulesService,
            paramIds, optionIds);

        assertThat(result.get(1L)).containsExactlyInAnyOrder(withIf, withThen);
        assertThat(result.get(10L)).containsExactlyInAnyOrder(withIf2);
        assertThat(result.flatEntryList().size()).isEqualTo(3);
    }

    private GLRule createRule(GLRuleType type, GLRulePredicate if0, GLRulePredicate then0) {
        return createRule(type, Collections.singletonList(if0), Collections.singletonList(then0));
    }

    private GLRule createRule(GLRuleType type, List<GLRulePredicate> ifs, List<GLRulePredicate> thens) {
        GLRule result = new GLRuleImpl();
        result.setType(type);
        result.setId(idsSeq++);
        result.setIfs(ifs);
        result.setThens(thens);
        return result;
    }

    private GLRulePredicate pr(Long paramId, Long valueId) {
        GLRulePredicate result = new GLRulePredicate();
        result.setId(idsSeq++);
        result.setParamId(paramId);
        result.setValueId(valueId);
        return result;
    }
}
