package ru.yandex.chemodan.app.telemost.orchestrator;

import ru.yandex.misc.ip.HostPort;
import ru.yandex.misc.net.HostnameUtils;

public class TranslatorsOrchestratorStub implements TranslatorsOrchestrator {

    @Override
    public HostPort acquireTranslator(String sessionId) {
        return new HostPort(HostnameUtils.localHostname(), 80);
    }

    @Override
    public void releaseTranslator(String sessionId) {
    }
}
