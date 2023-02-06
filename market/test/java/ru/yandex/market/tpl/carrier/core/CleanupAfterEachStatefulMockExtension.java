package ru.yandex.market.tpl.carrier.core;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.tpl.mock.DriverApiEmulator;
import ru.yandex.market.tpl.mock.EmployerApiEmulator;

public class CleanupAfterEachStatefulMockExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        SpringExtension.getApplicationContext(context).getBean(DriverApiEmulator.class).clear();
        SpringExtension.getApplicationContext(context).getBean(EmployerApiEmulator.class).clear();
    }
}
