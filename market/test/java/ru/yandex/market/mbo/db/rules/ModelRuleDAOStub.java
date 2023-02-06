package ru.yandex.market.mbo.db.rules;

import ru.yandex.market.mbo.gwt.models.params.dto.ModelRuleLink;
import ru.yandex.market.mbo.gwt.models.rules.ModelRule;
import ru.yandex.market.mbo.gwt.models.rules.ModelRulePredicate;
import ru.yandex.market.mbo.gwt.models.rules.ModelRuleSet;
import ru.yandex.market.mbo.gwt.models.rules.ValueHolder;
import ru.yandex.market.mbo.gwt.models.rules.ValueSource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author ayratgdl
 * @since 25.10.18
 */
public class ModelRuleDAOStub implements ModelRuleDAO {
    private Map<Long, ModelRuleSet> ruleSets = new HashMap<>();
    private Map<Long, Long> categoryToRuleSet = new HashMap<>();
    private long nextRuleSetId = 1;
    private long nextRuleId = 1;

    @Nullable
    @Override
    public ModelRuleSet loadModelRuleSet(long ruleSetId, boolean lock) {
        ModelRuleSet ruleSet = ruleSets.get(ruleSetId);
        return ruleSet != null ? new ModelRuleSet(ruleSet) : null;
    }

    @Override
    public void saveModelRuleSet(ModelRuleSet ruleSet) {
        if (ruleSet.getId() == ModelRuleSet.NO_ID) {
            ruleSet.setId(nextRuleSetId++);
        } else if (!ruleSets.containsKey(ruleSet.getId())) {
            throw new RuntimeException("RuleSet(" + ruleSet.getId() + ") doesn't exit");
        }
        for (ModelRule rule : ruleSet.getRules()) {
            if (rule.getId() == ModelRule.NO_ID) {
                rule.setId(nextRuleId++);
            }
            if (rule.getRuleSetId() == ModelRuleSet.NO_ID) {
                rule.setRuleSetId(ruleSet.getId());
            }
        }

        ruleSets.put(ruleSet.getId(), new ModelRuleSet(ruleSet));
    }

    @Override
    public void deleteModelRule(long ruleId) {
        boolean deleted = false;
        for (ModelRuleSet ruleSet : ruleSets.values()) {
            deleted = deleted || ruleSet.getRules().removeIf(rule -> rule.getId() == ruleId);
        }
        if (!deleted) {
            throw new RuntimeException("Unable to delete Model Rule. It has been removed");
        }
    }

    @Override
    public void saveModelRule(ModelRule rule) {
        if (!ruleSets.containsKey(rule.getRuleSetId())) {
            throw new RuntimeException("ModelRule(" + rule.getId() + ") doesn't contain rule set id");
        }
        ModelRuleSet ruleSet = ruleSets.get(rule.getRuleSetId());

        if (rule.getId() == ModelRule.NO_ID) {
            rule.setId(nextRuleId++);
            ruleSet.getRules().add(rule);
        } else {
            boolean deleted = ruleSet.getRules().removeIf(r -> r.getId() == rule.getId());
            if (!deleted) {
                throw new RuntimeException(
                    "Unable modify ModelRuleSet(" + ruleSet.getId() + "). Unknown ruleId " + rule.getId());
            }
            ruleSet.getRules().add(new ModelRule(rule));
        }
    }

    @Nullable
    @Override
    public Long getCategoryRuleSetId(long categoryId) {
        return categoryToRuleSet.get(categoryId);
    }

    @Override
    public void setRuleSetIdToCategory(long categoryId, long ruleSetId) {
        categoryToRuleSet.put(categoryId, ruleSetId);
    }

    @Override
    public Map<Long, List<ModelRuleLink>> getRulesByOptionIds(Collection<Long> optionIds) {
        Map<Long, List<ModelRuleLink>> result = new HashMap<>();
        for (Long optionId : optionIds) {
            for (ModelRuleSet ruleSet : ruleSets.values()) {
                for (ModelRule rule : ruleSet.getRules()) {
                    if (containsOptionId(optionId, rule)) {
                        ModelRuleLink ruleLink = new ModelRuleLink();
                        ruleLink.setCategoryHid(ruleSet.getCategoryId());
                        ruleLink.setRuleId(rule.getId());
                        ruleLink.setRuleName(rule.getName());
                        ruleLink.setModelEditorRule(ruleSet.isEditable());
                        ruleLink.setActive(rule.isActive());
                        ruleLink.setRuleSetId(ruleSet.getId());
                        result.computeIfAbsent(optionId, __ -> new ArrayList<>()).add(ruleLink);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Map<Long, List<ModelRuleLink>> getRulesByParamIds(Collection<Long> paramIds) {
        Map<Long, List<ModelRuleLink>> result = new HashMap<>();
        for (Long paramId : paramIds) {
            for (ModelRuleSet ruleSet : ruleSets.values()) {
                for (ModelRule rule : ruleSet.getRules()) {
                    if (containsParamId(paramId, rule)) {
                        ModelRuleLink ruleLink = new ModelRuleLink();
                        ruleLink.setCategoryHid(ruleSet.getCategoryId());
                        ruleLink.setRuleId(rule.getId());
                        ruleLink.setRuleName(rule.getName());
                        ruleLink.setModelEditorRule(ruleSet.isEditable());
                        ruleLink.setActive(rule.isActive());
                        ruleLink.setRuleSetId(ruleSet.getId());
                        result.computeIfAbsent(paramId, __ -> new ArrayList<>()).add(ruleLink);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Map<Long, Integer> getModelRulesCount(Long globalId) {
        Map<Long, Integer> result = new HashMap<>();
        for (ModelRuleSet value : ruleSets.values()) {
            for (ModelRule rule : value.getRules()) {
                if (rule.getThens().stream()
                        .anyMatch(x ->
                                x.getValueHolder().getId().equals(globalId))
                        || rule.getIfs().stream()
                        .anyMatch(x ->
                                x.getValueHolder().getId().equals(globalId))) {
                    result.put(value.getCategoryId(),
                            result.computeIfAbsent(value.getCategoryId(),
                                    x -> 0)
                                    + 1);
                }
            }
        }
        return result;
    }

    private boolean containsOptionId(Long optionId, ModelRule rule) {
        for (ModelRulePredicate anIf : rule.getIfs()) {
            if (anIf.getValueIds().contains(optionId)) {
                return true;
            }
        }
        for (ModelRulePredicate anThen : rule.getThens()) {
            if (anThen.getValueIds().contains(optionId)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsParamId(Long paramId, ModelRule rule) {
        for (ModelRulePredicate anIf : rule.getIfs()) {
            ValueHolder valueHolder = anIf.getValueHolder();
            if (valueHolder.getSource() == ValueSource.MODEL_PARAMETER) {
                Long id = valueHolder.getId();
                if (paramId.equals(id)) {
                    return true;
                }
            }
        }
        for (ModelRulePredicate anThen : rule.getThens()) {
            ValueHolder valueHolder = anThen.getValueHolder();
            if (valueHolder.getSource() == ValueSource.MODEL_PARAMETER) {
                Long id = valueHolder.getId();
                if (paramId.equals(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public <T> T doInTransaction(Supplier<T> callback) {
        return callback.get();
    }

    @Nullable
    @Override
    public ModelRule loadModelRule(long ruleId, boolean lock) {
        return ruleSets.values().stream()
            .map(ModelRuleSet::getRules)
            .flatMap(Collection::stream)
            .filter(rule -> rule.getId() == ruleId)
            .findAny()
            .map(rule -> {
                ModelRule copy = new ModelRule(rule);
                copy.setIfs(new ArrayList<>());
                copy.setThens(new ArrayList<>());
                return copy;
            })
            .orElse(null);
    }
}
