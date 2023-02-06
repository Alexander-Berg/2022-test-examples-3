package ru.yandex.direct.core.entity.feature.repository;

import java.util.Collections;
import java.util.LinkedHashSet;

import org.junit.Test;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.utils.JsonUtils;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class FeatureSettingsSerializationTest {
    private static final String DEFAULT_FEATURE_SETTINGS_JSON =
            "{\"is_accessible_after_disabling\":true,\"roles\":[\"SUPER\"],\"original_owner\":\"user1\"," +
                    "\"original_watchers\":[\"user1\",\"user2\"],\"percent\":1,\"is_public\":true}";
    private static final FeatureSettings DEFAULT_FEATURE_SETTINGS = getDefaultFeatureSettings();

    @Test
    public void settingsFromJson() {
        FeatureSettings actualFeatureSettings =
                JsonUtils.fromJson(DEFAULT_FEATURE_SETTINGS_JSON, FeatureSettings.class);
        assertThat("rule соответствует ожиданиям", DEFAULT_FEATURE_SETTINGS,
                beanDiffer(actualFeatureSettings).useCompareStrategy(
                        DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    public void settingsToJson() {
        String actualFeatureSettingsJson = JsonUtils.toJson(DEFAULT_FEATURE_SETTINGS);
        assertThat("rule соответствует ожиданиям", DEFAULT_FEATURE_SETTINGS_JSON, equalTo(actualFeatureSettingsJson));
    }

    @Test
    public void settingsToJson_isPublicNull() {
        String expectedSettings = "{\"is_accessible_after_disabling\":true,\"roles\":[\"SUPER\"]," +
                "\"original_owner\":\"user1\",\"original_watchers\":[\"user1\",\"user2\"],\"percent\":1}";
        FeatureSettings featureSettings = getDefaultFeatureSettings().withIsPublic(null);
        String actualFeatureSettingsJson = JsonUtils.toJson(featureSettings);
        assertThat("rule соответствует ожиданиям", actualFeatureSettingsJson, equalTo(expectedSettings));
    }

    @Test
    public void settingsFromDb_isPublicNull() {
        String featureSettings = "{\"is_accessible_after_disabling\":true,\"roles\":[\"SUPER\"]," +
                "\"original_owner\":\"user1\",\"original_watchers\":[\"user1\",\"user2\"],\"percent\":1," +
                "\"is_public\":false}";
        FeatureSettings actualFeatureSettings = FeatureMappings.fromDb(featureSettings);
        FeatureSettings expectedSettings = getDefaultFeatureSettings().withIsPublic(false);
        assertThat("rule соответствует ожиданиям", actualFeatureSettings, beanDiffer(expectedSettings));
    }

    private static FeatureSettings getDefaultFeatureSettings() {
        return new FeatureSettings()
                .withIsAccessibleAfterDisabling(true)
                .withIsPublic(true)
                .withPercent(1)
                .withOriginalOwner("user1")
                .withOriginalWatchers(new LinkedHashSet<>(asList("user1", "user2")))
                .withRoles(Collections.singleton("SUPER"));
    }
}
