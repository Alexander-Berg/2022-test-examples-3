package ru.yandex.market.marketpromo.core.config;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.config.TestApplicationProfiles;
import ru.yandex.market.marketpromo.core.test.misc.TestingComponent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

@ActiveProfiles(TestApplicationProfiles.UNIT_TEST)
public class DifferentProfileAssertTest extends ServiceTestBase {
    @Autowired
    private Environment environment;

    @Autowired
    private TestingComponent testingComponent;

    @Test
    void shouldBeUnitProfile() {
        assertThat(List.of(environment.getDefaultProfiles()), hasItem(TestApplicationProfiles.DEFAULT));
        assertThat(List.of(environment.getActiveProfiles()), hasItem(TestApplicationProfiles.UNIT_TEST));
    }

    @Test
    void shouldBeUnitComponent() {
        assertThat(testingComponent.getName(), is(TestApplicationProfiles.UNIT_TEST));
    }
}
