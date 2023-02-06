package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithNetworkSettings;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.AUTO_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MAX_CONTEXT_LIMIT;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstants.MIN_CONTEXT_LIMIT;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.NumberDefects.inInterval;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithNetworkSettingsUpdateValidationTypeSupportTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    private CampaignWithNetworkSettingsUpdateValidationTypeSupport typeSupport;

    private static final ClientId CLIENT_ID = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
    private static final Long UID = RandomNumberUtils.nextPositiveLong();

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MCBANNER},
        });
    }

    @Test
    public void preValidate_Successfully() {
        CampaignWithNetworkSettings campaign = createCampaign();
        ModelChanges<CampaignWithNetworkSettings> campaignModelChanges =
                ModelChanges.build(campaign, CampaignWithNetworkSettings.CONTEXT_LIMIT, MIN_CONTEXT_LIMIT);
        var vr = typeSupport.preValidate(
                CampaignValidationContainer.create(0, UID, CLIENT_ID),
                new ValidationResult<>(List.of(campaignModelChanges)));

        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void preValidate_WithValidationError() {
        CampaignWithNetworkSettings campaign = createCampaign();
        ModelChanges<CampaignWithNetworkSettings> campaignModelChanges =
                ModelChanges.build(campaign, CampaignWithNetworkSettings.CONTEXT_LIMIT, AUTO_CONTEXT_LIMIT);
        var vr = typeSupport.preValidate(
                CampaignValidationContainer.create(0, UID, CLIENT_ID),
                new ValidationResult<>(List.of(campaignModelChanges)));

        assertThat(vr, hasDefectWithDefinition(validationError(
                path(index(0), field(CampaignWithNetworkSettings.CONTEXT_LIMIT)),
                inInterval(MIN_CONTEXT_LIMIT, MAX_CONTEXT_LIMIT))));
    }

    private CampaignWithNetworkSettings createCampaign() {
        CommonCampaign campaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withClientId(CLIENT_ID.asLong())
                .withName("campaign")
                .withUid(UID);
        return ((CampaignWithNetworkSettings) campaign)
                .withContextLimit(100);
    }

}
