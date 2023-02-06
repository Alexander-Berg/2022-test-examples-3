package ru.yandex.market.mbo.conflictrule;

import ru.yandex.utils.Pair;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ayratgdl
 * @date 02.11.17
 */
public class ConflictRuleDAOMock implements ConflictRuleDAO {
    private HashMap<Long, ConflictRule> rules = new HashMap<>();
    private long nextId = 1;

    @Override
    public void createConflictRule(ConflictRule rule) {
        rule.setId(nextId++);
        rules.put(rule.getId(), rule.copy());
    }

    @Override
    public void deleteConflictRule(Long id) {
        rules.remove(id);
    }

    @Override
    public void addPair(Long id, Long firstOptionId, Long secondOptionId) {
        ConflictRule rule = rules.get(id);
        rule.addPair(firstOptionId, secondOptionId);
    }

    @Override
    public void removePair(Long id, Long firstOptionId, Long secondOptionId) {
        rules.get(id).getPairs().remove(new Pair<>(firstOptionId, secondOptionId));
    }

    @Override
    public ConflictRule getConflictRule(Long id) {
        return rules.get(id).copy();
    }

    @Override
    public List<ConflictRule> getAllConflictRules() {
        return rules.values().stream().map(ConflictRule::copy).collect(Collectors.toList());
    }

    @Override
    public List<ConflictRule> findConflictRules(Collection<Long> categoryIds,
                                                Collection<Long> firstParamIds,
                                                Collection<Long> secondParamIds) {
        return rules.values().stream()
            .filter(rule -> categoryIds.isEmpty() || categoryIds.contains(rule.getCategoryId()))
            .filter(rule -> firstParamIds.isEmpty() || firstParamIds.contains(rule.getFirstParamId()))
            .filter(rule -> secondParamIds.isEmpty() || secondParamIds.contains(rule.getSecondParamId()))
            .map(ConflictRule::copy)
            .collect(Collectors.toList());
    }
}
