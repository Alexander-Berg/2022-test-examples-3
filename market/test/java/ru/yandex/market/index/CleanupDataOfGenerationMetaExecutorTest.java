package ru.yandex.market.index;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.mbi.tms.jobs.CleanupOutdatedDataExecutor;
import ru.yandex.market.shop.FunctionalTest;

class CleanupDataOfGenerationMetaExecutorTest extends FunctionalTest {
    @Autowired
    @Qualifier("cleanupDataOfGenerationMetaExecutor")
    CleanupOutdatedDataExecutor executor;

    @Test
    void doJob() {
        executor.doJob(null);
    }
}
