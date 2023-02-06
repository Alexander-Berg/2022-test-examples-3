package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithExcludePausedCompetingAds;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CampaignWithExcludePausedCompetingAdsAddValidationTypeSupportTest {

    private static ClientId clientId;
    private static Long uid;

    private CampaignWithExcludePausedCompetingAdsAddValidationTypeSupport validationTypeSupport;

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        validationTypeSupport = new CampaignWithExcludePausedCompetingAdsAddValidationTypeSupport();
    }

    @Test
    public void validate_Successfully() {
        CampaignWithExcludePausedCompetingAds campaign = createCampaign();
        var vr = validationTypeSupport.validate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(List.of(campaign)));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_expectedCannotBeNull() {
        CampaignWithExcludePausedCompetingAds campaign = createCampaign().withExcludePausedCompetingAds(null);
        var vr = validationTypeSupport.validate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(List.of(campaign)));
        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithExcludePausedCompetingAds.EXCLUDE_PAUSED_COMPETING_ADS)),
                DefectIds.CANNOT_BE_NULL)));
    }

    private static CampaignWithExcludePausedCompetingAds createCampaign() {
        return new TextCampaign()
                .withClientId(clientId.asLong())
                .withExcludePausedCompetingAds(true)
                .withName("campaign")
                .withUid(uid);
    }

}
