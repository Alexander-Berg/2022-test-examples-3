package ru.yandex.direct.core.entity.campaign.service.validation.type.add;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomCheckPositionEvent;
import ru.yandex.direct.core.entity.campaign.model.CommonCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomEnumUtils;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithCheckPositionEventAddValidationTypeSupportTest {
    private CampaignWithCheckPositionEventAddValidationTypeSupport validationTypeSupport;

    private ClientId clientId;
    private Long operatorUid;
    private CampaignWarnPlaceInterval validWarnPlaceInterval;
    private boolean validEnableCheckPositionEvent;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.MOBILE_CONTENT},
                {CampaignType.DYNAMIC}
        });
    }

    @Before
    public void before() {
        validationTypeSupport = new CampaignWithCheckPositionEventAddValidationTypeSupport();
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        operatorUid = RandomNumberUtils.nextPositiveLong();
        validWarnPlaceInterval = RandomEnumUtils.getRandomEnumValue(CampaignWarnPlaceInterval.class);
        validEnableCheckPositionEvent = RandomUtils.nextBoolean();
    }

    @Test
    public void validate_Successfully() {
        List<CampaignWithCustomCheckPositionEvent> validCampaigns = List.of(
                createCampaign(validEnableCheckPositionEvent, validWarnPlaceInterval));

        var vr = validationTypeSupport.validate(CampaignValidationContainer.create(0, operatorUid, clientId),
                new ValidationResult<>(validCampaigns));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    private CampaignWithCustomCheckPositionEvent createCampaign(Boolean enableCheckPositionEvent,
                                                                CampaignWarnPlaceInterval warnPlaceInterval) {
        CommonCampaign campaign = TestCampaigns.newCampaignByCampaignType(campaignType)
                .withClientId(clientId.asLong())
                .withName("valid_campaign_name")
                .withUid(operatorUid);
        return ((CampaignWithCustomCheckPositionEvent) campaign)
                .withEnableCheckPositionEvent(enableCheckPositionEvent)
                .withCheckPositionIntervalEvent(warnPlaceInterval);
    }
}
