package ru.yandex.market.mbo.mdm.common.service;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;

public class FeatureSwitchingAssistantImplTest {
    private FeatureSwitchingAssistant featureSwitchingAssistant;


    @Before
    public void setUp() {
        featureSwitchingAssistant = new FeatureSwitchingAssistantImpl(new StorageKeyValueServiceMock());
    }

    @Test
    public void testFeatureEnabledGlobal() {
        String key = "KEY";

        // storage_key_value not contain key -> feature not enabled
        Assertions.assertThat(featureSwitchingAssistant.isFeatureEnabled(key)).isFalse();

        featureSwitchingAssistant.enableFeature(key);
        Assertions.assertThat(featureSwitchingAssistant.isFeatureEnabled(key)).isTrue();

        featureSwitchingAssistant.disableFeature(key);
        Assertions.assertThat(featureSwitchingAssistant.isFeatureEnabled(key)).isFalse();
    }

    @Test
    public void testFeatureEnabledInCategory() {
        String key = "KEY";

        // storage_key_value not contain key -> feature not enabled
        Assertions.assertThat(featureSwitchingAssistant.isFeatureEnabledInCategory(key, 1)).isFalse();

        featureSwitchingAssistant.updateFeatureEnabledCategories(key, List.of(1L));
        Assertions.assertThat(featureSwitchingAssistant.isFeatureEnabledInCategory(key, 1)).isTrue();
        Assertions.assertThat(featureSwitchingAssistant.isFeatureEnabledInCategory(key, 2)).isFalse();
        Assertions.assertThat(featureSwitchingAssistant.isFeatureEnabledInCategory(key, 3)).isFalse();

        // updateFeatureEnabledCategories fully replaces categories
        featureSwitchingAssistant.updateFeatureEnabledCategories(key, List.of(2L, 3L));
        Assertions.assertThat(featureSwitchingAssistant.isFeatureEnabledInCategory(key, 1)).isFalse();
        Assertions.assertThat(featureSwitchingAssistant.isFeatureEnabledInCategory(key, 2)).isTrue();
        Assertions.assertThat(featureSwitchingAssistant.isFeatureEnabledInCategory(key, 3)).isTrue();
    }
}
