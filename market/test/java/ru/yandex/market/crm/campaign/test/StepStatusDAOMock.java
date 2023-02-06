package ru.yandex.market.crm.campaign.test;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.services.actions.StepsStatusDAO;

/**
 * @author apershukov
 */
public class StepStatusDAOMock implements StepsStatusDAO {

    private final Map<Pair<String, String>, StepStatus<?>> statuses = new ConcurrentHashMap<>();

    @Override
    public void upsert(String actionId, StepStatus status) {
        statuses.put(Pair.of(actionId, status.getStepId()), status);
    }

    @Override
    public <T extends StepStatus<T>> T update(String actionId, String stepId, Consumer<T> callback) {
        var key = Pair.of(actionId, stepId);
        var status = (T) statuses.get(key);

        callback.accept(status);
        statuses.put(key, status);

        return status;
    }

    @Override
    public List<StepStatus<?>> getOfAction(String actionId) {
        return statuses.entrySet().stream()
                .filter(e -> e.getKey().getLeft().equals(actionId))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public StepStatus<?> get(String actionId, String stepId) {
        return statuses.get(Pair.of(actionId, stepId));
    }

    @Override
    public void removeForAction(String actionId) {
        statuses.keySet().stream()
            .filter(key -> Objects.equals(key.getLeft(), actionId))
            .forEach(statuses::remove);
    }

    @Override
    public void remove(String actionId, Set<String> stepIds) {
        stepIds.stream()
            .map(stepId -> Pair.of(actionId, stepId))
            .forEach(statuses::remove);
    }
}
