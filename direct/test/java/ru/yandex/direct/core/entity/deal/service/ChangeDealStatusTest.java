package ru.yandex.direct.core.entity.deal.service;

import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.deal.container.CampaignDeal;
import ru.yandex.direct.core.entity.deal.container.UpdateDealContainer;
import ru.yandex.direct.core.entity.deal.model.CompleteReason;
import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.deal.model.StatusDirect;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestDeals;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DealInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.testing.matchers.validation.Matchers;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeCpmDealsCampaign;
import static ru.yandex.direct.core.testing.data.TestClients.defaultClient;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ChangeDealStatusTest {

    @Autowired
    private DealService dealService;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private Steps steps;

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private ClientInfo agencyClientInfo;
    private ClientInfo clientInfo;
    private Long dealId;
    private Long cid;
    private int dealShard = 1;
    private int clientShard = 2;

    @Before
    public void setUp() {
        agencyClientInfo =
                steps.clientSteps().createClient(new ClientInfo().withShard(dealShard)
                        .withClient(defaultClient().withRole(RbacRole.AGENCY)));
        clientInfo = steps.clientSteps().createClientUnderAgency(agencyClientInfo,
                new ClientInfo().withShard(clientShard));

        Deal deal = TestDeals.defaultPrivateDeal(agencyClientInfo.getClientId());
        deal.withDirectStatus(StatusDirect.ACTIVE);
        List<DealInfo> createdDeals = steps.dealSteps().addDeals(Collections.singletonList(deal), agencyClientInfo);
        List<Long> dealIds = mapList(createdDeals, DealInfo::getDealId);
        dealId = dealIds.get(0);
        cid = steps.campaignSteps().createCampaign(new CampaignInfo()
                .withClientInfo(clientInfo)
                .withCampaign(activeCpmDealsCampaign(clientInfo.getClientId(), clientInfo.getUid())
                        .withAgencyId(agencyClientInfo.getClientId().asLong())
                        .withAgencyUid(agencyClientInfo.getUid()))).getCampaignId();
        UpdateDealContainer updateDealContainer = new UpdateDealContainer();
        updateDealContainer.withAdded(
                Collections.singletonList(new CampaignDeal().withCampaignId(cid).withDealId(dealId)));
        Result<UpdateDealContainer> linkResult =
                dealService.updateDeal(agencyClientInfo.getUid(), agencyClientInfo.getClientId(),
                        updateDealContainer);
        checkState(linkResult.getErrors().isEmpty(), "линк кампаний успешен");
        Assertions.assertThat(linkResult.getValidationResult()).is(matchedBy(Matchers.hasNoErrorsAndWarnings()));
    }

    @Test
    public void completeDeals() {
        campaignRepository.updateStatusBsSynced(clientShard, singletonList(cid), StatusBsSynced.YES);
        MassResult<Long> result =
                dealService
                        .completeDeals(agencyClientInfo.getClientId(), singletonList(dealId), CompleteReason.BY_CLIENT,
                                Applicability.FULL);
        Assertions.assertThat(result.getValidationResult()).is(matchedBy(Matchers.hasNoErrorsAndWarnings()));
        CampaignSimple campaign = campaignRepository.getCampaignsSimple(clientShard, singletonList(cid)).get(cid);
        assertThat(campaign.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }

    @Test
    public void archiveDeals() {
        MassResult<Long> result =
                dealService
                        .completeDeals(agencyClientInfo.getClientId(), singletonList(dealId), CompleteReason.BY_CLIENT,
                                Applicability.FULL);
        Assertions.assertThat(result.getValidationResult()).is(matchedBy(Matchers.hasNoErrorsAndWarnings()));
        campaignRepository.updateStatusBsSynced(clientShard, singletonList(cid), StatusBsSynced.YES);
        result = dealService.archiveDeals(agencyClientInfo.getClientId(), singletonList(dealId), Applicability.FULL);
        Assertions.assertThat(result.getValidationResult()).is(matchedBy(Matchers.hasNoErrorsAndWarnings()));
        CampaignSimple campaign = campaignRepository.getCampaignsSimple(clientShard, singletonList(cid)).get(cid);
        assertThat(campaign.getStatusBsSynced(), equalTo(StatusBsSynced.NO));
    }
}
