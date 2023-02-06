package ru.yandex.market.mbo.db.modelstorage.stubs;

import ru.yandex.market.mbo.db.modelstorage.health.SaveStats;
import ru.yandex.market.mbo.db.modelstorage.transitions.ClusterTransitionsService;
import ru.yandex.market.mbo.export.modelstorage.ThrowingConsumer;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * @author s-ermakov
 */
public class ClusterTransitionsServiceStub implements ClusterTransitionsService {

    private List<ModelStorage.ModelTransitions> transitions = new ArrayList<>();

    @Override
    public void save(List<ModelStorage.ModelTransitions> transitions) {
        this.transitions.addAll(transitions);
    }

    @Override
    public void save(List<ModelStorage.ModelTransitions> transitions, SaveStats saveStats) {
        save(transitions);
    }

    @Override
    public void processTransitionsHistory(long startTime, long endTime,
                                          ThrowingConsumer<ModelStorage.ModelTransitions> processor) {
        throw new RuntimeException("Not implemented");
    }

    public long getTransitionsCount() {
        return transitions.size();
    }
}
