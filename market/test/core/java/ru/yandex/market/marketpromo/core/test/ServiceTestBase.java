package ru.yandex.market.marketpromo.core.test;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.marketpromo.core.test.config.ApplicationCoreTaskBasicSupportConfig;
import ru.yandex.market.marketpromo.core.test.config.ApplicationCoreTestConfig;
import ru.yandex.market.marketpromo.core.test.context.initializer.YdbContainerContextInitializer;
import ru.yandex.market.marketpromo.core.test.utils.OfferStorageTestHelper;
import ru.yandex.market.marketpromo.core.utils.RequestContextUtils;
import ru.yandex.market.marketpromo.misc.ExtendedClock;
import ru.yandex.market.marketpromo.security.SystemUserUtils;
import ru.yandex.market.ydb.integration.DataCleaner;

@ContextConfiguration(
        classes = {
                ApplicationCoreTestConfig.class,
                ApplicationCoreTaskBasicSupportConfig.class
        },
        initializers = YdbContainerContextInitializer.class
)
@ExtendWith(SpringExtension.class)
public abstract class ServiceTestBase {

    @Autowired
    protected ExtendedClock clock;
    @Autowired
    private DataCleaner dataCleaner;
    @Autowired
    private OfferStorageTestHelper storageTestHelper;

    public ServiceTestBase() {
        SystemUserUtils.loginAsSystem();
        RequestContextUtils.setupContext(Map.of());
    }

    @AfterEach
    private void cleanData() {
        dataCleaner.cleanData();
        storageTestHelper.reset();
        clock.clearFixed();
    }
}
