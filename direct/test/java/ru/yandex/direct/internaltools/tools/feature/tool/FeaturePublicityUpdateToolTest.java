package ru.yandex.direct.internaltools.tools.feature.tool;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.testing.steps.FeatureSteps;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.core.exception.InternalToolValidationException;
import ru.yandex.direct.internaltools.tools.feature.container.InternalToolsFeaturePublicityState;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeaturePublicityUpdateToolTest {

    private static final String NOT_EXISTING_FEATURE_TEXT_ID = "not_existing_feature_text_id";

    @Autowired
    private FeatureSteps steps;

    @Autowired
    private FeaturePublicityUpdateTool featurePublicityUpdateTool;

    @Test
    public void validate_whenFeatureTextIdIsNull() {
        var params = new InternalToolsFeaturePublicityState()
                .withTextId(null)
                .withAction(InternalToolsFeaturePublicityState.Action.DISABLE_PUBLICITY);

        ValidationResult<InternalToolsFeaturePublicityState, Defect> vr = featurePublicityUpdateTool.validate(params);

        var expected = validationError(path(field(InternalToolsFeaturePublicityState.FEATURE_TEXT_ID)), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_whenActionIsNull() {
        var params = new InternalToolsFeaturePublicityState()
                .withTextId("1")
                .withAction(null);

        ValidationResult<InternalToolsFeaturePublicityState, Defect> vr = featurePublicityUpdateTool.validate(params);

        var expected = validationError(path(field(InternalToolsFeaturePublicityState.PUBLICITY_ACTION)), notNull());
        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(expected)));
    }

    @Test
    public void validate_whenAllParamsIsValid() {
        var params = new InternalToolsFeaturePublicityState()
                .withTextId("1")
                .withAction(InternalToolsFeaturePublicityState.Action.DISABLE_PUBLICITY);

        ValidationResult<InternalToolsFeaturePublicityState, Defect> vr = featurePublicityUpdateTool.validate(params);

        assertThat(vr.hasAnyErrors()).isFalse();
    }

    @Test(expected = InternalToolValidationException.class)
    public void process_whenUpdateNotExistingFeature() {
        var params = new InternalToolsFeaturePublicityState()
                .withTextId(NOT_EXISTING_FEATURE_TEXT_ID)
                .withAction(InternalToolsFeaturePublicityState.Action.DISABLE_PUBLICITY);
        featurePublicityUpdateTool.process(params);
    }

    @Test
    public void process_whenUpdateExistingFeature() {
        var defaultFeature = steps.addDefaultFeature();
        var featureBeforeUpdate = new Feature()
                .withId(defaultFeature.getId())
                .withFeatureTextId(defaultFeature.getFeatureTextId())
                .withFeaturePublicName(defaultFeature.getFeaturePublicName())
                .withSettings(defaultFeature.getSettings());

        var params = new InternalToolsFeaturePublicityState()
                .withTextId(defaultFeature.getFeatureTextId())
                .withAction(InternalToolsFeaturePublicityState.Action.ENABLE_PUBLICITY);
        featurePublicityUpdateTool.process(params);
        var featureAfterUpdate = steps.getFeature(defaultFeature.getId());
        var settingsBefore = featureBeforeUpdate.getSettings();
        var settingsAfter = featureAfterUpdate.getSettings();
        assertThat(settingsBefore.getIsAccessibleAfterDisabling()).isEqualTo(settingsAfter.getIsAccessibleAfterDisabling());
        assertThat(settingsBefore.getRoles()).isEqualTo(settingsAfter.getRoles());
        assertThat(settingsBefore.getPercent()).isEqualTo(settingsAfter.getPercent());
        assertThat(settingsBefore.getCanEnable()).isEqualTo(settingsAfter.getCanEnable());
        assertThat(settingsBefore.getIsAgencyFeature()).isEqualTo(settingsAfter.getIsAgencyFeature());
        checkState(defaultFeature.getSettings().getIsPublic().equals(false));
        assertThat(settingsAfter.getIsPublic()).isTrue();
    }
}
