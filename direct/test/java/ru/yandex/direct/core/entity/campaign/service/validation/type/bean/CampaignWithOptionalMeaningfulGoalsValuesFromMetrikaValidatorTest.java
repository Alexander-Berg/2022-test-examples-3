package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;


import java.util.List;

import javax.annotation.Nullable;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithOptionalMeaningfulGoalsValuesFromMetrika;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.meaningfulGoalsValuesFromMetrikaAllowOnlyAllGoals;
import static ru.yandex.direct.core.entity.campaign.service.validation.StrategyDefects.meaningfulGoalsValuesFromMetrikaInconsistentStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.dafaultAverageCpaPayForConversionStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAutobudgetRoiStrategy;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;

public class CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaValidatorTest {

    private void checkValidator(CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaValidator validator,
                                CampaignWithOptionalMeaningfulGoalsValuesFromMetrika campaign,
                                @Nullable Path expectedPath,
                                @Nullable Defect expectedDefect) {
        ValidationResult<CampaignWithOptionalMeaningfulGoalsValuesFromMetrika, Defect> result =
                validator.apply(campaign);

        if (expectedDefect == null) {
            Assertions.assertThat(result).
                    is(matchedBy(hasNoDefectsDefinitions()));
        } else {
            Assertions.assertThat(result).
                    is(matchedBy(hasDefectWithDefinition(validationError(expectedPath, expectedDefect))));
        }
    }

    @Test
    public void meaningfulGoalsValuesFromMetrika_IsNull() {
        DbStrategy dbStrategy = defaultAutobudgetRoiStrategy(1L);

        var validator = new CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaValidator();
        CampaignWithOptionalMeaningfulGoalsValuesFromMetrika campaign = new TextCampaign()
                .withType(CampaignType.TEXT)
                .withStrategy(dbStrategy)
                .withIsMeaningfulGoalsValuesFromMetrikaEnabled(null);

        checkValidator(validator, campaign, null, null);
    }

    @Test
    public void meaningfulGoalsValuesFromMetrika_IsFalse() {
        DbStrategy dbStrategy = defaultAutobudgetRoiStrategy(1L);

        var validator = new CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaValidator();
        CampaignWithOptionalMeaningfulGoalsValuesFromMetrika campaign = new TextCampaign()
                .withType(CampaignType.TEXT)
                .withStrategy(dbStrategy)
                .withIsMeaningfulGoalsValuesFromMetrikaEnabled(false);

        checkValidator(validator, campaign, null, null);
    }

    @Test
    public void meaningfulGoalsValuesFromMetrika_IsTrue() {
        DbStrategy dbStrategy = defaultAutobudgetRoiStrategy(MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID);

        var validator = new CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaValidator();
        CampaignWithOptionalMeaningfulGoalsValuesFromMetrika campaign = new TextCampaign()
                .withType(CampaignType.TEXT)
                .withStrategy(dbStrategy)
                .withIsMeaningfulGoalsValuesFromMetrikaEnabled(true);

        checkValidator(validator, campaign, null, null);
    }

    @Test
    public void meaningfulGoalsValuesFromMetrika_UnsupportedFeatureButIsCopyMode_Success() {
        DbStrategy dbStrategy = defaultAutobudgetRoiStrategy(MEANINGFUL_GOALS_OPTIMIZATION_GOAL_ID);

        var validator = new CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaValidator();
        CampaignWithOptionalMeaningfulGoalsValuesFromMetrika campaign = new TextCampaign()
                .withType(CampaignType.TEXT)
                .withStrategy(dbStrategy)
                .withIsMeaningfulGoalsValuesFromMetrikaEnabled(true);

        checkValidator(validator, campaign, null, null);
    }

    @Test
    public void meaningfulGoalsValuesFromMetrika_UnsupportedStrategy() {
        DbStrategy dbStrategy = dafaultAverageCpaPayForConversionStrategy();

        var validator = new CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaValidator();
        CampaignWithOptionalMeaningfulGoalsValuesFromMetrika campaign = new TextCampaign()
                .withType(CampaignType.TEXT)
                .withStrategy(dbStrategy)
                .withIsMeaningfulGoalsValuesFromMetrikaEnabled(true);

        checkValidator(validator, campaign, new Path(List.of(field("isMeaningfulGoalsValuesFromMetrikaEnabled"))),
                meaningfulGoalsValuesFromMetrikaInconsistentStrategy());
    }

    @Test
    public void meaningfulGoalsValuesFromMetrika_UnsupportedGoalId() {
        DbStrategy dbStrategy = defaultAutobudgetRoiStrategy(1L);

        var validator = new CampaignWithOptionalMeaningfulGoalsValuesFromMetrikaValidator();
        CampaignWithOptionalMeaningfulGoalsValuesFromMetrika campaign = new TextCampaign()
                .withType(CampaignType.TEXT)
                .withStrategy(dbStrategy)
                .withIsMeaningfulGoalsValuesFromMetrikaEnabled(true);

        checkValidator(validator, campaign, new Path(List.of(field("isMeaningfulGoalsValuesFromMetrikaEnabled"))),
                meaningfulGoalsValuesFromMetrikaAllowOnlyAllGoals());
    }
}
