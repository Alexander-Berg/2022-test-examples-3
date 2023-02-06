package ru.yandex.market.marketpromo.core.application.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.test.ServiceTestBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class AppFeaturesConfigurationTest extends ServiceTestBase {

    @Autowired
    private ConfigHolder<AppFeatures> features;

    @Test
    void shouldGetDefaultValue() {
        assertThat(features.get().useOffersFromDatabase(), is(false));
    }

    @Test
    void shouldGetModifiedValue() {
        features.get().setUseOffersFromDatabase(true);

        assertThat(features.get().useOffersFromDatabase(), is(true));
    }
}
