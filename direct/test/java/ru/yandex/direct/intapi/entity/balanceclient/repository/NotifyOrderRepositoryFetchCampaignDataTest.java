package ru.yandex.direct.intapi.entity.balanceclient.repository;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.balanceclient.container.CampaignDataForNotifyOrder;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestUsers.generateNewUser;

@RunWith(Parameterized.class)
@IntApiTest
public class NotifyOrderRepositoryFetchCampaignDataTest {
    @Autowired
    private NotifyOrderRepository notifyOrderRepository;

    @Autowired
    public Steps steps;

    private CampaignInfo campaignInfo;
    private CampaignDataForNotifyOrder campData;

    private final Campaign campaign;
    private final String expectedEmail;
    private final Boolean copyConverted;
    private final boolean expectedAfterCopyConvertFlag;

    public NotifyOrderRepositoryFetchCampaignDataTest(
            Campaign campaign, Boolean copyConverted, String expectedEmail, boolean expectedAfterCopyConvertFlag
    ) {
        this.campaign = campaign;
        this.expectedEmail = expectedEmail;
        this.copyConverted = copyConverted;
        this.expectedAfterCopyConvertFlag = expectedAfterCopyConvertFlag;
    }

    @Before
    public void prepareData() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        ClientInfo clInfo = steps.clientSteps().createClient(new ClientInfo()
                .withChiefUserInfo(new UserInfo()
                        .withUser(generateNewUser().withEmail(TestUsers.DEFAULT_USER_EMAIL))));
        campaignInfo = steps.campaignSteps().createCampaign(campaign, clInfo);
        if (copyConverted != null) {
            long oldCid = campaignInfo.getCampaignId();
            if (copyConverted) {
                campaignInfo = steps.campaignSteps().createCampaign(campaign.withId(null), clInfo);
            }
            long newCid = campaignInfo.getCampaignId();
            steps.currencySteps().createCurrencyConvertMoneyCorrespondenceEntry(clInfo, oldCid, newCid);
        }

        campData = notifyOrderRepository.fetchCampaignData(campaignInfo.getShard(), campaignInfo.getCampaignId());
    }

    @Parameterized.Parameters()
    public static Collection campaigns() {
        return Arrays.asList(new Object[][]{
                {TestCampaigns.newTextCampaign(null, null).withEmail(""), null, TestUsers.DEFAULT_USER_EMAIL, false},
                {TestCampaigns.newTextCampaign(null, null).withEmail("hello@there.com"),
                        null, "hello@there.com", false},

                {TestCampaigns.newTextCampaign(null, null).withEmail(""), false, TestUsers.DEFAULT_USER_EMAIL, false},
                {TestCampaigns.newTextCampaign(null, null).withEmail(""), true, TestUsers.DEFAULT_USER_EMAIL, true}
        });
    }


    @Test
    public void fetchCampaignDataReturnsNullForNonexistentId() {
        assertNull(notifyOrderRepository.fetchCampaignData(1, Long.MAX_VALUE));
    }

    @Test
    public void campaignDataHasCorrectEmail() {
        assertThat(campData.getEmail(), is(expectedEmail));
    }

    @Test
    public void campaignDataHasCorrectPostConvertFlag() {
        assertThat(campData.getFirstAfterCopyConvert(), is(expectedAfterCopyConvertFlag));
    }

    @Test
    public void testFutureTestReturnsFalseForNonexistent() {
        assertFalse(notifyOrderRepository.isThereAnyCampStartingInFutureUnderWallet(1, Long.MAX_VALUE, Long.MAX_VALUE));
    }
}
