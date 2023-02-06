package ru.yandex.direct.intapi.entity.copyentity.controller;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.copyentity.CopyOperationAssert;
import ru.yandex.direct.core.copyentity.model.CopyCampaignFlags;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.campaign.service.CampaignWithAdGroupsService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbqueue.repository.DbQueueRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.copyentity.model.CopyCampaignRequest;
import ru.yandex.direct.intapi.entity.copyentity.model.CopyClientCampaignBulkResponse;
import ru.yandex.direct.intapi.entity.copyentity.model.CopyClientCampaignResponse;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.web.core.model.WebResponse;
import ru.yandex.qatools.allure.annotations.Description;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.copyentity.CopyOperationAssert.Mode.COPIED;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.CAMPAIGNS_COPY;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
@Description("Сценарии работы контроллера по копированию сущностей")
public class CopyEntityControllerTest {

    @Autowired
    private CopyEntityController copyEntityController;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignWithAdGroupsService campaignToAdGroupService;

    @Autowired
    private CopyOperationAssert asserts;

    @Autowired
    private DbQueueRepository dbQueueRepository;

    private Long uid;
    private ClientId clientId;

    private Long campaignId;
    private CampaignInfo campaignInfo;

    @Before
    public void setUp() {
        var superClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.SUPER);
        uid = superClientInfo.getUid();

        var clientInfo = steps.clientSteps().createDefaultClient();

        clientId = clientInfo.getClientId();
        steps.featureSteps().setCurrentClient(clientId);

        campaignInfo = steps.campaignSteps().createCampaign(
                activeTextCampaign(clientId, clientInfo.getUid()).withEmail("test@yandex-team.ru"),
                clientInfo);
        campaignId = campaignInfo.getCampaignId();

        asserts.init(clientId, clientId, uid);
    }

    @Test(expected = IntApiException.class)
    public void copyClientCampaign_noIds_errorIsThrown() {
        copyEntityController.copyClientCampaign(uid, clientId.asLong(), null);
    }

    @Test
    public void copyClientCampaign_copyOneCampaign_campaignIsCopied() {
        var adGroupInfo = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), campaignInfo);

        WebResponse result = copyEntityController.copyClientCampaign(uid, clientId.asLong(), campaignId);
        assertThat(result).isInstanceOf(CopyClientCampaignResponse.class);
        var response = (CopyClientCampaignResponse) result;
        assertThat(response.validationResult().getErrors()).isEmpty();

        var copiedCampaignIds = Set.of(response.getResultCampaignId());
        var copiedAdGroupIds = campaignToAdGroupService.getChildEntityIdsByParentIds(clientId, uid, copiedCampaignIds);

        asserts.assertCampaignIsCopied(copiedCampaignIds, campaignInfo.getCampaignId());
        asserts.assertEntitiesAreCopied(AdGroup.class, copiedAdGroupIds, List.of(adGroupInfo.getAdGroup()), COPIED);
    }

    @Test(expected = IntApiException.class)
    public void copyClientCampaigns_noIds_errorIsThrown() {
        var copyCampaignRequest = new CopyCampaignRequest(uid, clientId.asLong(), clientId.asLong(),
                null, null, false);
        copyEntityController.copyClientCampaigns(copyCampaignRequest);
    }

    @Test
    public void copyClientCampaigns_copyOneCampaign_campaignIsInDbQueue() {
        steps.dbQueueSteps().registerJobType(CAMPAIGNS_COPY);
        var flags = new CopyCampaignFlags();
        var request = new CopyCampaignRequest(uid, clientId.asLong(), clientId.asLong(),
                List.of(campaignId), flags, false);
        CopyClientCampaignBulkResponse response = copyEntityController.copyClientCampaigns(request);
        var job = dbQueueRepository.grabSingleJob(campaignInfo.getShard(), CAMPAIGNS_COPY);
        SoftAssertions.assertSoftly(soft -> {
            soft.assertThat(response.getCampaignIdsToCopy()).isEqualTo(List.of(campaignId));
            soft.assertThat(response.getTimeEstimateMinutes()).isEqualTo(1L);
            soft.assertThat(job).isNotNull();
            soft.assertThat(job.getArgs().getCampaignId()).isEqualTo(campaignId);
        });
    }
}
