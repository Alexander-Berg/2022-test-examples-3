package ru.yandex.direct.intapi.entity.balanceclient.service.validation;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BANANA_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.BAYAN_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.BalanceClientServiceConstants.DIRECT_SERVICE_ID;
import static ru.yandex.direct.intapi.entity.balanceclient.container.BalanceClientResponse.error;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.CAMPAIGN_TYPE_DOES_NOT_CORRESPOND_TO_SERVICE_MESSAGE;
import static ru.yandex.direct.intapi.entity.balanceclient.service.validation.NotifyOrderValidationService.INCOMPATIBLE_SERVICE_ID_AND_CAMPAIGN_TYPE_ERROR_CODE;

@RunWith(Parameterized.class)
public class NotifyOrderValidationServiceCampaignTypeTest {

    private static final int INVALID_SERVICE_ID = 123;

    private NotifyOrderValidationService notifyOrderValidationService;
    private Long campaignId;

    @Parameterized.Parameter()
    public Integer serviceId;

    @Parameterized.Parameter(1)
    public CampaignType campaignType;

    @Parameterized.Parameter(2)
    public boolean expectError;

    @Parameterized.Parameters(name = "serviceId={0}, campaignType={1}, expectError={2}")
    public static Collection<Object[]> data() {
        return asList(new Object[][]{
                {INVALID_SERVICE_ID, CampaignType.TEXT, true},
                {DIRECT_SERVICE_ID, CampaignType.MCB, true},
                {BAYAN_SERVICE_ID, CampaignType.TEXT, true},
                {BAYAN_SERVICE_ID, CampaignType.DYNAMIC, true},
                {BAYAN_SERVICE_ID, CampaignType.MOBILE_CONTENT, true},

                {DIRECT_SERVICE_ID, CampaignType.GEO, false},
                {DIRECT_SERVICE_ID, CampaignType.TEXT, false},
                {DIRECT_SERVICE_ID, CampaignType.PERFORMANCE, false},
                {DIRECT_SERVICE_ID, CampaignType.BILLING_AGGREGATE, false},
                {BAYAN_SERVICE_ID, CampaignType.MCB, false},
        });
    }

    @Before
    public void before() {
        notifyOrderValidationService = new NotifyOrderValidationService(null,
                DIRECT_SERVICE_ID, BAYAN_SERVICE_ID, BANANA_SERVICE_ID);
        campaignId = RandomNumberUtils.nextPositiveLong();
    }


    @Test
    public void checkValidateCampaignType() {
        BalanceClientResponse response =
                notifyOrderValidationService.validateCampaignType(serviceId, campaignType, campaignId);

        if (expectError) {
            String expectedMessage = String.format(CAMPAIGN_TYPE_DOES_NOT_CORRESPOND_TO_SERVICE_MESSAGE,
                    campaignId, campaignType.name().toLowerCase(), serviceId);
            assertThat("получили ожидаемый ответ", response,
                    beanDiffer(error(INCOMPATIBLE_SERVICE_ID_AND_CAMPAIGN_TYPE_ERROR_CODE, expectedMessage)));
        } else {
            assertThat("получили null в ответе", response, nullValue());
        }
    }
}
