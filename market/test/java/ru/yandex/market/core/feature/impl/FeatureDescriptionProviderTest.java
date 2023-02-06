package ru.yandex.market.core.feature.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.feature.model.FeatureDescription;
import ru.yandex.market.core.feature.model.FeatureType;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Тесты для {@link FeatureDescriptionProvider}.
 */
class FeatureDescriptionProviderTest extends FunctionalTest {

    @Autowired
    private FeatureDescriptionProvider featureDescriptionProvider;

    @Test
    void testMapping() {
        for (final FeatureType type : FeatureType.values()) {
            final FeatureDescription description = featureDescriptionProvider.getDescription(type);

            assertThat(description, notNullValue());
            assertThat(description.getFeatureType(), equalTo(type));
        }
    }

}
