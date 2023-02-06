package ru.yandex.direct.core.entity.campaign.service.validation.type.update;

import java.util.List;

import org.apache.commons.lang.math.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWarnPlaceInterval;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithCustomCheckPositionEvent;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.service.validation.type.container.CampaignValidationContainer;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.test.utils.RandomEnumUtils;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

@CoreTest
public class CampaignWithCheckPositionEventUpdateValidationTypeSupportTest {
    private CampaignWithCheckPositionEventUpdateValidationTypeSupport validationTypeSupport;

    private ClientId clientId;
    private Long operatorUid;
    private CampaignValidationContainer container;
    private CampaignWarnPlaceInterval validWarnPlaceInterval;
    private boolean validEnableCheckPositionEvent;

    @Before
    public void before() {
        validationTypeSupport = new CampaignWithCheckPositionEventUpdateValidationTypeSupport();
        clientId = ClientId.fromLong(RandomNumberUtils.nextPositiveLong());
        operatorUid = RandomNumberUtils.nextPositiveLong();
        container = CampaignValidationContainer.create(0, operatorUid, clientId);
        validWarnPlaceInterval = RandomEnumUtils.getRandomEnumValue(CampaignWarnPlaceInterval.class);
        validEnableCheckPositionEvent = RandomUtils.nextBoolean();
    }

    @Test
    public void validate_Successfully() {
        List<CampaignWithCustomCheckPositionEvent> validCampaigns = List.of(
                createCampaign(validEnableCheckPositionEvent, validWarnPlaceInterval));

        var vr = validationTypeSupport.validate(container, new ValidationResult<>(validCampaigns));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    private CampaignWithCustomCheckPositionEvent createCampaign(Boolean enableCheckPositionEvent,
                                                                CampaignWarnPlaceInterval warnPlaceInterval) {
        return new TextCampaign()
                .withClientId(clientId.asLong())
                .withEnableCheckPositionEvent(enableCheckPositionEvent)
                .withCheckPositionIntervalEvent(warnPlaceInterval)
                .withName("valid_campaign_name")
                .withUid(operatorUid);
    }
}
