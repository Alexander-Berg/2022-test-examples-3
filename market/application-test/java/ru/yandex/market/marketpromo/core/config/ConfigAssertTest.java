package ru.yandex.market.marketpromo.core.config;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.core.test.misc.TestingComponent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

public class ConfigAssertTest extends ServiceTestBase {

    @Autowired
    private Environment environment;

    @Autowired
    private TestingComponent testingComponent;

    @Test
    void shouldBeDevelopmentProfile() {
        assertThat(List.of(environment.getDefaultProfiles()), hasItem(ApplicationProfile.DEFAULT));
        assertThat(List.of(environment.getActiveProfiles()), empty());
    }

    @Test
    void shouldBeDefaultComponent() {
        assertThat(testingComponent.getName(), is("default"));
    }
}
