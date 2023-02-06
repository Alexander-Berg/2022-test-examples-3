package ru.yandex.market.mbo.db.params;

import ru.yandex.market.mbo.gwt.models.gurulight.GLPredicateSearchFilter;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRule;
import ru.yandex.market.mbo.gwt.models.gurulight.GLRuleSearchFilter;
import ru.yandex.market.mbo.gwt.models.params.CategoryRules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author ayratgdl
 * @date 06.11.17
 */
public class GLRulesServiceMock implements GLRulesServiceInterface {
    private List<GLRule> rules = new ArrayList<>();
    private long nextId = 1;

    @Override
    public GLRule addRule(GLRule rule, Long uid, boolean generateId) {
        return addRule(rule, uid, generateId, true);
    }

    @Override
    public GLRule addRule(GLRule rule, Long uid, boolean generateId, boolean touchCategory) {
        if (generateId) {
            rule.setId(nextId++);
        }
        rules.add(rule);
        return rule;
    }

    @Override
    public GLRule addRule(GLRule rule, Long uid) {
        return addRule(rule, uid, true, true);
    }

    @Override
    public void saveRules(Collection<GLRule> rules, Long uid) {
        for (GLRule rule : rules) {
            saveRule(rule, uid);
        }
    }

    @Override
    public void saveRule(GLRule rule, Long uid) {
        if (rule.getId() == 0) {
            addRule(rule, uid);
        } else {
            rules.removeIf(o -> o.getId() == rule.getId());
            addRule(rule, uid, false, true);
        }
    }

    @Override
    public Optional<GLRule> getRule(Long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<GLRule> searchRules(GLRuleSearchFilter filter) {
        return rules.stream().filter(filter::test).collect(Collectors.toList());
    }

    @Override
    public Set<Long> searchRulesByPredicates(Set<GLPredicateSearchFilter> predicates) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> searchRulesByPredicate(GLPredicateSearchFilter filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<GLRule> loadRules(long hid) {
        return rules.stream().filter(rule -> rule.getHid() == hid).collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<GLRule>> loadRules(Collection<Long> hids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRule(GLRule removedRule, Long uid) {
        rules.removeIf(nextRule -> nextRule.getId() == removedRule.getId());
    }

    @Override
    public void removeRules(Collection<GLRule> rules, Long uid) {
        List<Long> ids = rules.stream().map(GLRule::getId).collect(Collectors.toList());
        this.rules.removeIf(nextRule -> ids.contains(nextRule.getId()));
    }

    @Override
    public void disableRuleInheritance(Long uid, GLRule rule, long hid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableRuleInheritance(Long uid, GLRule rule, long hid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CategoryRules loadCategoryRulesByHid(long hid) {
        throw new UnsupportedOperationException();
    }

    public List<GLRule> getRules() {
        return rules;
    }

    public void setRules(List<GLRule> rules) {
        this.rules = rules;
    }
}
