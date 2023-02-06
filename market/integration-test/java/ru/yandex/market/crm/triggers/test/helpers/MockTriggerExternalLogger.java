package ru.yandex.market.crm.triggers.test.helpers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import ru.yandex.market.crm.triggers.services.bpm.delegates.log.TriggerExternalLog;
import ru.yandex.market.crm.triggers.services.bpm.delegates.log.TriggerExternalLogger;

@Component
public class MockTriggerExternalLogger extends TriggerExternalLogger {
    private final List<TriggerExternalLog> logHistory = new ArrayList<>();

    public MockTriggerExternalLogger() {
        super(null);
    }

    @Override
    public void logData(TriggerExternalLog logInfo) {
        logHistory.add(logInfo);
    }

    public List<TriggerExternalLog> getLogHistory() {
        return logHistory;
    }

    public void clearHistory() {
        logHistory.clear();
    }
}
