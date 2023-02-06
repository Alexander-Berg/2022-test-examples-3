package ru.yandex.market.mcrm.lock;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mcrm.db.test.TestMasterReadOnlyDataSourceConfiguration;

@Configuration
@Import({
        TestMasterReadOnlyDataSourceConfiguration.class,
        LockServiceConfiguration.class
})
public class DbLockServiceTestConfig {
}
