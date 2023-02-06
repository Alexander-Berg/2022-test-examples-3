package ru.yandex.direct.core.copyentity;

import java.time.LocalDate;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetNotificationStatus;
import ru.yandex.direct.core.entity.campaign.model.CampaignStatusModerate;
import ru.yandex.direct.core.entity.campaign.model.CpmBannerCampaign;
import ru.yandex.direct.core.entity.campaign.model.EshowsVideoType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.campaign.TestCpmBannerCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CopyOperationCpmBannerCampaignTest {

    @Autowired
    private Steps steps;

    @Autowired
    private CopyOperationFactory factory;

    @Autowired
    private CopyOperationAssert asserts;

    private Long uid;
    private ClientId clientId;
    private ClientInfo clientInfo;

    private Long campaignId;
    private CampaignInfo campaignInfo;
    private CopyOperation xerox;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        asserts.init(clientId, clientId, uid);

        campaignInfo = steps.cpmBannerCampaignSteps().createCampaign(
                clientInfo, TestCpmBannerCampaigns.fullCpmBannerCampaign()
                        .withStartDate(LocalDate.now().plusDays(1L))
                        .withEndDate(LocalDate.now().plusDays(2L))
                        .withStatusModerate(CampaignStatusModerate.YES));
        campaignId = campaignInfo.getCampaignId();

        CopyConfig copyConfig = CopyEntityTestUtils.campaignCopyConfig(clientInfo, campaignId, uid);
        xerox = factory.build(copyConfig);
    }

    @Test
    public void campaignIsCopied() {
        var copyResult = xerox.copy();
        asserts.checkErrors(copyResult);

        Set<Long> copiedCampaignIds = Set.copyOf(copyResult.getEntityMapping(BaseCampaign.class).values());
        asserts.assertCampaignIsCopied(copiedCampaignIds, campaignInfo.getCampaignId(), this::prepare);
    }

    private void prepare(BaseCampaign baseCampaign) {
        CpmBannerCampaign campaign = (CpmBannerCampaign) baseCampaign;
        campaign.setDayBudgetNotificationStatus(CampaignDayBudgetNotificationStatus.READY);
        campaign.getEshowsSettings().setVideoType(EshowsVideoType.COMPLETES);
    }

}
