package ru.yandex.direct.internaltools.tools.feature.tool;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.exception.InternalToolValidationException;
import ru.yandex.direct.internaltools.tools.feature.container.InternalToolsAddFeaturesParams;
import ru.yandex.direct.staff.client.StaffClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeaturesAddToolTest {

    private static final String NOT_EXISTING_FEATURE_TEXT_ID = "not_existing_feature_text_id";
    private static final String OWNER = "user1";
    private static final String WATCHER_1 = "user2";
    private static final String WATCHER_2 = "user3";

    @Autowired
    private FeaturesAddTool tool;

    @Autowired
    private FeatureSteps steps;

    @Autowired
    private StaffClient staffClient;

    @Before
    public void before() {
        Set<String> allUsers = Set.of(OWNER, WATCHER_1, WATCHER_2);
        when(staffClient.getExistingUsers(allUsers)).thenReturn(allUsers);
    }

    @Test(expected = InternalToolValidationException.class)
    public void process_AddNotExistingFeature() {
        InternalToolsAddFeaturesParams params = getParams(NOT_EXISTING_FEATURE_TEXT_ID);
        tool.process(params);
    }


    @Test
    public void process_AddExistingFeature() {
        FeatureName defaultFeature = FeatureName.values()[0];

        InternalToolsAddFeaturesParams params = getParams(defaultFeature.getName());

        tool.process(params);
        var featureFromDb = steps.getFeature(defaultFeature.getName());
        var settingsFromDb = featureFromDb.getSettings();
        assertThat(settingsFromDb.getOriginalOwner()).isEqualTo(OWNER);
        assertThat(settingsFromDb.getOriginalWatchers()).isEqualTo(Set.of(WATCHER_1, WATCHER_2));
        assertThat(settingsFromDb.getIsAgencyFeature()).isTrue();

        steps.deleteFeature(featureFromDb.getId());
    }

    private InternalToolsAddFeaturesParams getParams(String featureTextId) {
        var params = new InternalToolsAddFeaturesParams();
        params.setFeatureTextId(featureTextId);
        params.setOwner(OWNER);
        params.setWatchers(WATCHER_1 + "," + WATCHER_2);
        params.setIsAgencyFeature(true);
        return params;
    }

}
