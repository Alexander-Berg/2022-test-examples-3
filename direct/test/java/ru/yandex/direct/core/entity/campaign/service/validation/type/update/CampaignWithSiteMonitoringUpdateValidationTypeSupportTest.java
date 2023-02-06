package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithSiteMonitoring;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.testing.data.TestCampaigns;
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

@RunWith(Parameterized.class)
public class CampaignWithSiteMonitoringUpdateValidationTypeSupportTest {

    private static ClientId clientId;
    private static Long uid;

    private CampaignWithSiteMonitoringUpdateValidationTypeSupport validationTypeSupport;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MCBANNER},
        });
    }

    private CampaignWithSiteMonitoring createCampaign() {
        CommonCampaign campaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withClientId(clientId.asLong())
                .withName("campaign")
                .withUid(uid);
        return ((CampaignWithSiteMonitoring) campaign)
                .withHasSiteMonitoring(true);
    }

    @Before
    public void before() {
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        uid = RandomNumberUtils.nextPositiveLong();
        validationTypeSupport = new CampaignWithSiteMonitoringUpdateValidationTypeSupport();
    }

    @Test
    public void validate_Successfully() {
        CampaignWithSiteMonitoring campaign = createCampaign();
        var vr = validationTypeSupport.validate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(List.of(campaign)));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validate_expectedCannotBeNull() {
        CampaignWithSiteMonitoring campaign = createCampaign().withHasSiteMonitoring(null);
        var vr = validationTypeSupport.validate(CampaignValidationContainer.create(0, uid, clientId),
                new ValidationResult<>(List.of(campaign)));
        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithSiteMonitoring.HAS_SITE_MONITORING)),
                DefectIds.CANNOT_BE_NULL)));
    }
}
