package ru.yandex.market.logshatter.config.ddl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import ru.yandex.market.clickhouse.ddl.DDL;
import ru.yandex.market.health.configs.logshatter.config.LogShatterConfig;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 03.11.16
 */
class FakeUpdateDDLService extends UpdateDDLService {
    private final UpdateDDLTaskExecutorResult result;

    private final List<Consumer<UpdateDDLTaskExecutorResult>> resultWatchers =
        Collections.synchronizedList(new ArrayList<>());
    private final List<Consumer<Exception>> exceptionWatchers = Collections.synchronizedList(new ArrayList<>());

    FakeUpdateDDLService() {
        this(new UpdateDDLTaskExecutorResult.Success());
    }

    FakeUpdateDDLService(UpdateDDLTaskExecutorResult result) {
        super(null, null, null, 1, 600, 10, false, false, null, true);
        this.result = result;
    }

    @Override
    public List<DDL> updateDDL(List<LogShatterConfig> configs) {
        try {
            run();
        } catch (InterruptedException ignored) {
        }
        return Collections.emptyList();
    }

    @Override
    public void watchResult(Consumer<UpdateDDLTaskExecutorResult> callback) {
        resultWatchers.add(callback);
    }

    @Override
    public List<DDL> getManualDDLs() {
        if (result instanceof UpdateDDLTaskExecutorResult.ManualDDLRequired) {
            return ((UpdateDDLTaskExecutorResult.ManualDDLRequired) result).getManualDDLs();
        }

        return Collections.emptyList();
    }

    protected void run() throws InterruptedException {
        Thread.sleep(1);
        notifyWatchers(this.result);
    }

    protected void notifyWatchers(UpdateDDLTaskExecutorResult result) {
        for (Consumer<UpdateDDLTaskExecutorResult> callback : resultWatchers) {
            callback.accept(result);
        }
    }
}
