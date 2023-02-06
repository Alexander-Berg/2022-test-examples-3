package ru.yandex.direct.internaltools.tools.feature.tool;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.exception.InternalToolValidationException;
import ru.yandex.direct.internaltools.tools.feature.container.InternalToolsAddFeaturesParams;
import ru.yandex.direct.internaltools.tools.feature.container.InternalToolsFeaturePercent;

import static org.assertj.core.api.Assertions.assertThat;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeaturesPercentUpdateToolTest {

    private static final String NOT_EXISTING_FEATURE_TEXT_ID = "someone_not_existing_feature";
    public static final Integer TARGET_PERCENT = 50;
    public static final String OWNER = "user1";

    @Autowired
    private FeaturesAddTool addTool;

    @Autowired
    private FeaturePercentUpdateTool tool;

    @Autowired
    private FeatureSteps steps;

    @Test(expected = InternalToolValidationException.class)
    public void process_UpdatePercebtForNotExistingFeature() {
        InternalToolsFeaturePercent params = getPercentUpdateParams(NOT_EXISTING_FEATURE_TEXT_ID);
        tool.process(params);
    }


    @Test
    public void process_UpdatePercentOnExistingFeature() {
        FeatureName defaultFeature = FeatureName.values()[0];

        String featureTextId = defaultFeature.getName();
        InternalToolsFeaturePercent params = getPercentUpdateParams(featureTextId);
        addFeature(featureTextId);
        steps.updatePercentForFeature(featureTextId, 0);

        tool.process(params);
        var featureFromDb = steps.getFeature(featureTextId);
        var settingsFromDb = featureFromDb.getSettings();
        assertThat(settingsFromDb.getPercent()).isEqualTo(TARGET_PERCENT);

        steps.deleteFeature(featureFromDb.getId());
    }

    private void addFeature(String featureTextId) {
        var addParams = new InternalToolsAddFeaturesParams();
        addParams.setFeatureTextId(featureTextId);
        addParams.setOwner(OWNER);
        addTool.process(addParams);
    }


    private InternalToolsFeaturePercent getPercentUpdateParams(String featureTextId) {
        var params = new InternalToolsFeaturePercent()
                .withPercent(Long.valueOf(TARGET_PERCENT))
                .withTextId(featureTextId);
        return params;
    }

}
