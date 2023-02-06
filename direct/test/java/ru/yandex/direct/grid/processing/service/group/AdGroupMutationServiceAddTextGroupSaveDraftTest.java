package ru.yandex.direct.grid.processing.service.group;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddTextAdGroupItem;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.iterableWithSize;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.regions.Region.MOSCOW_REGION_ID;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupMutationServiceAddTextGroupSaveDraftTest {

    @Autowired
    Steps steps;
    @Autowired
    AdGroupRepository adGroupRepository;
    @Autowired
    AdGroupMutationService adGroupMutationService;

    private static final String AD_GROUP_NAME = RandomStringUtils.randomAlphanumeric(16);

    private GdAddTextAdGroup gdAddTextAdGroup;
    private ClientId clientId;
    private Long uid;
    private int shard;

    @Before
    public void before() {
        ClientInfo clientInfo =
                steps.clientSteps().createClient(defaultClient().withCountryRegionId(Region.RUSSIA_REGION_ID));
        User user = clientInfo.getChiefUserInfo().getUser();

        uid = user.getUid();
        clientId = clientInfo.getClientId();
        shard = clientInfo.getShard();

        Campaign campaign = TestCampaigns.activeTextCampaign(clientInfo.getClientId(), uid);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);

        gdAddTextAdGroup = new GdAddTextAdGroup()
                .withAddItems(singletonList(
                        new GdAddTextAdGroupItem()
                                .withName(AD_GROUP_NAME)
                                .withCampaignId(campaignInfo.getCampaignId())
                                .withAdGroupMinusKeywords(emptyList())
                                .withLibraryMinusKeywordsIds(emptyList())
                                .withBidModifiers(new GdUpdateBidModifiers())
                                .withRegionIds(singletonList(Long.valueOf(MOSCOW_REGION_ID).intValue()))
                ));

        TestAuthHelper.setDirectAuthentication(user);
    }

    @Test
    public void addTextAdGroups_saveDraftTrue() {
        GdAddAdGroupPayload payload = adGroupMutationService.addTextAdGroups(clientId, uid, uid,
                gdAddTextAdGroup.withSaveDraft(true));
        checkStatusModerate(payload, StatusModerate.NEW);
    }

    @Test
    public void addTextAdGroups_saveDraftFalse() {
        GdAddAdGroupPayload payload = adGroupMutationService.addTextAdGroups(clientId, uid, uid,
                gdAddTextAdGroup.withSaveDraft(false));
        checkStatusModerate(payload, StatusModerate.READY);
    }

    @Test
    public void addTextAdGroups_withoutSaveDraft() {
        GdAddAdGroupPayload payload = adGroupMutationService.addTextAdGroups(clientId, uid, uid, gdAddTextAdGroup);
        checkStatusModerate(payload, StatusModerate.READY);
    }

    private void checkStatusModerate(GdAddAdGroupPayload payload, StatusModerate expectedStatusModerate) {
        validateResponseSuccessful(payload);
        assumeThat(payload.getAddedAdGroupItems(), iterableWithSize(1));

        Long adGroupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);

        assertThat(actualAdGroup.getStatusModerate()).isEqualTo(expectedStatusModerate);
    }
}
