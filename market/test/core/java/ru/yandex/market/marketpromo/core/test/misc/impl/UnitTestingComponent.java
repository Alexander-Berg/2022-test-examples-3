package ru.yandex.market.marketpromo.core.test.misc.impl;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles;
import ru.yandex.market.marketpromo.core.test.misc.TestingComponent;

@Component
@Profile(TestApplicationProfiles.UNIT_TEST)
public class UnitTestingComponent implements TestingComponent {
    @Override
    public String getName() {
        return TestApplicationProfiles.UNIT_TEST;
    }
}
