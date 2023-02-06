package ru.yandex.chemodan.app.orchestrator;

import ru.yandex.chemodan.boot.ChemodanMainSupport;
import ru.yandex.chemodan.util.test.ContextTestSupport;

/**
 * @author yashunsky
 */
public class OrchestratorWorkerContextTest extends ContextTestSupport {
    @Override
    public ChemodanMainSupport createMain() {
        return new OrchestratorWorkerMain();
    }
}
