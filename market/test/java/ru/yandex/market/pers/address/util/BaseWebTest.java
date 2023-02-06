package ru.yandex.market.pers.address.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import ru.yandex.market.pers.address.config.Mocks;
import ru.yandex.market.pers.address.config.TestConfig;
import ru.yandex.market.pers.address.config.TestConfig.TestTvmRequestAuthHandler;
import ru.yandex.market.pers.address.services.PresetService;

import java.util.Map;

import static org.mockito.Mockito.reset;
import static ru.yandex.market.pers.address.config.TestConfig.TestTvmRequestAuthHandler.TestMode.DEFAULT;

@ExtendWith(ComplicatedMonitorCleaner.class)
@SpringJUnitWebConfig(TestConfig.class)
public class BaseWebTest {
    @Autowired
    private MarketDataSyncClientTestImpl dataSyncClientTest;
    @Autowired
    private DbCleaner dbCleaner;

    @Mocks
    @Autowired
    private Map<Object, Runnable> mocks;

    @Autowired
    protected TestTvmRequestAuthHandler testTvmRequestAuthHandler;

    @Autowired
    private PresetService presetService;

    @BeforeEach
    @AfterEach
    public void clearDbAndCheckConsistency() {
        if (shouldClearDb()) {
            dbCleaner.clearDb();
        }
    }

    protected boolean shouldClearDb() {
        return true;
    }

    @BeforeEach
    public void init() {
        resetMocks();
        dataSyncClientTest.clear();
        testTvmRequestAuthHandler.setTestMode(DEFAULT);
        presetService.invalidateCache();
    }

    private void resetMocks() {
        mocks.forEach((o, runnable) -> {
            reset(o);
            runnable.run();
        });
    }
}
