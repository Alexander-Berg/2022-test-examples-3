package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.time.LocalDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithEshowsSettingsAndStrategy;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.model.EshowsSettings;
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefectIds.Gen.VIDEO_TYPE_NOT_SUPPORTED_WITH_STRATEGY;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultAvgCpvStrategy;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newCampaignByCampaignType;
import static ru.yandex.direct.core.testing.data.TestCampaigns.simpleStrategy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class CampaignWithEshowsSettingsAndStrategyValidatorTest {

    @Test
    public void testCreateCampaignWithValidEshowsVideoType() {
        CampaignWithEshowsSettingsAndStrategy campaign = createCampaign(
                defaultAvgCpvStrategy(LocalDateTime.now()),
                null
        );
        ValidationResult<CampaignWithEshowsSettingsAndStrategy, Defect> vr =
                CampaignWithEshowsSettingsAndStrategyValidator.build().apply(campaign);
        assertThat(vr, hasNoErrorsAndWarnings());
    }

    @Test
    public void testCreateCampaignWithInvalidEshowsVideoType() {
        CampaignWithEshowsSettingsAndStrategy campaign = createCampaign(
                defaultAvgCpvStrategy(LocalDateTime.now()),
                EshowsVideoType.LONG_CLICKS
        );
        ValidationResult<CampaignWithEshowsSettingsAndStrategy, Defect> vr =
                CampaignWithEshowsSettingsAndStrategyValidator.build().apply(campaign);
        assertThat(vr, hasWarningWithDefinition(
                validationError(path(field(CampaignWithEshowsSettingsAndStrategy.ESHOWS_SETTINGS)),
                        VIDEO_TYPE_NOT_SUPPORTED_WITH_STRATEGY)));
    }

    @Test
    public void testUpdateCampaignWithValidEshowsVideoType() {
        CampaignWithEshowsSettingsAndStrategy campaign = createCampaign(
                simpleStrategy(),
                EshowsVideoType.LONG_CLICKS
        );
        var modelChanges = ModelChanges.build(campaign, CampaignWithEshowsSettingsAndStrategy.STRATEGY,
                defaultAvgCpvStrategy(LocalDateTime.now()));
        var appliedChanges = modelChanges.applyTo(campaign);
        ValidationResult<CampaignWithEshowsSettingsAndStrategy, Defect> vr =
                CampaignWithEshowsSettingsAndStrategyValidator.build(appliedChanges).apply(campaign);
        assertThat(vr, hasNoErrorsAndWarnings());
    }

    @Test
    public void testUpdateCampaignWithInvalidEshowsVideoType() {
        CampaignWithEshowsSettingsAndStrategy campaign = createCampaign(
                defaultAvgCpvStrategy(LocalDateTime.now()), null);
        var modelChanges = ModelChanges.build(campaign, CampaignWithEshowsSettingsAndStrategy.ESHOWS_SETTINGS,
                new EshowsSettings().withVideoType(EshowsVideoType.LONG_CLICKS));
        var appliedChanges = modelChanges.applyTo(campaign);
        ValidationResult<CampaignWithEshowsSettingsAndStrategy, Defect> vr =
                CampaignWithEshowsSettingsAndStrategyValidator.build(appliedChanges).apply(campaign);
        assertThat(vr, hasWarningWithDefinition(
                validationError(path(field(CampaignWithEshowsSettingsAndStrategy.ESHOWS_SETTINGS)),
                        VIDEO_TYPE_NOT_SUPPORTED_WITH_STRATEGY)));
    }

    private static CampaignWithEshowsSettingsAndStrategy createCampaign(
            DbStrategy strategy,
            EshowsVideoType eshowsVideoType
    ) {
        return ((CampaignWithEshowsSettingsAndStrategy) newCampaignByCampaignType(CampaignType.CPM_BANNER))
                .withId(RandomNumberUtils.nextPositiveLong())
                .withStrategy(strategy)
                .withEshowsSettings(new EshowsSettings().withVideoType(eshowsVideoType));
    }
}
