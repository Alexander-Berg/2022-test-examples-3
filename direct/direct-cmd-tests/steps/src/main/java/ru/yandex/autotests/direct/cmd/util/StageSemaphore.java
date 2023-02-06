package ru.yandex.autotests.direct.cmd.util;

import ru.yandex.autotests.irt.testutils.allure.LogSteps;
import ru.yandex.qatools.hazelcast.SemaphoreRule;

public class StageSemaphore {
    private static LogSteps log = LogSteps.getLogger(StageSemaphore.class);

    public static SemaphoreRule getSemaphore(String host, Integer permits) {
        String ruleKey = "direct-cmd-" + host + "-" + permits;
        return getSemaphoreRule(ruleKey, permits);
    }

    public static SemaphoreRule getSemaphoreRule(String ruleKey, Integer permits) {
        log.info("Semaphore rule key: " + ruleKey);
        return (permits > 0) ? new SemaphoreRule(ruleKey, permits) : null;
    }
}
