package ru.yandex.direct.core.entity.relevancematch.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupSimple;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.relevancematch.container.RelevanceMatchAddContainer;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.showcondition.container.ShowConditionAutoPriceParams;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.dbutil.model.ClientId;

import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;

public abstract class RelevanceMatchOperationBaseTest {
    @Autowired
    protected Steps steps;
    @Autowired
    protected UserSteps userSteps;
    @Autowired
    protected CampaignSteps campaignSteps;
    @Autowired
    protected AdGroupSteps adGroupSteps;
    @Autowired
    protected RelevanceMatchService relevanceMatchService;
    @Autowired
    protected RelevanceMatchRepository relevanceMatchRepository;
    @Autowired
    protected CampaignRepository campaignRepository;

    protected ClientInfo clientInfo;
    protected UserInfo defaultUser;
    protected CampaignInfo activeCampaign;
    protected CampaignInfo activeManualCampaign;
    protected AdGroupInfo defaultAdGroup;
    protected Map<Long, Campaign> campaignsByIds;
    protected Map<Long, AdGroupSimple> adGroupByIds;
    protected Map<Long, Long> campaignIdsByAdGroupIds;


    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        defaultUser = clientInfo.getChiefUserInfo();
        activeCampaign = campaignSteps.createActiveCampaign(clientInfo);
        defaultAdGroup = adGroupSteps.createAdGroup(getAdGroup(), activeCampaign);
        campaignsByIds = new HashMap<>();
        DbStrategy strategy = new DbStrategy();
        strategy.setAutobudget(CampaignsAutobudget.YES);
        Campaign campaign = new Campaign()
                .withId(activeCampaign.getCampaignId())
                .withType(CampaignType.TEXT)
                .withAutobudget(strategy.isAutoBudget())
                .withStrategy(strategy)
                .withCurrency(CurrencyCode.RUB);
        campaignsByIds.put(activeCampaign.getCampaignId(), campaign);

        Campaign manualCampaign = makeManualCampaign();
        campaignsByIds.put(manualCampaign.getId(), manualCampaign);
        adGroupByIds = new HashMap<>();
        adGroupByIds.put(defaultAdGroup.getAdGroupId(), defaultAdGroup.getAdGroup());
        campaignIdsByAdGroupIds = new HashMap<>();
        campaignIdsByAdGroupIds.put(defaultAdGroup.getAdGroupId(), defaultAdGroup.getCampaignId());
    }

    private Campaign makeManualCampaign() {
        ru.yandex.direct.core.testing.steps.campaign.model0.Campaign
                campModel = activeTextCampaign(clientInfo.getClientId(), defaultUser.getUid());
        campModel.getContextSettings().setPriceCoeff(100);
        activeManualCampaign = campaignSteps.createCampaign(campModel, defaultUser.getClientInfo());
        return campaignRepository.getCampaigns(
                clientInfo.getShard(),
                Collections.singleton(activeManualCampaign.getCampaignId())
        ).get(0);
    }

    protected abstract AdGroup getAdGroup();


    protected RelevanceMatch getValidRelevanceMatch() {
        return new RelevanceMatch()
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withIsSuspended(true);

    }

    protected RelevanceMatch makeRelevanceMatchNoPrices(AdGroupInfo adGroupInfo) {
        return getValidRelevanceMatch()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withAutobudgetPriority(null)
                .withPrice(null)
                .withPriceContext(null);
    }

    protected RelevanceMatchAddOperation getFullAddOperation(RelevanceMatch relevanceMatch) {
        return getFullAddOperation(Collections.singletonList(defaultAdGroup), Collections.singletonList(relevanceMatch),
                false, null);
    }

    protected RelevanceMatchAddOperation createAddOperationWithAutoPrices(
            AdGroupInfo adGroupInfo, RelevanceMatch relevanceMatche,
            ShowConditionAutoPriceParams autoPriceParams) {
        return getFullAddOperation(Collections.singletonList(adGroupInfo), Collections.singletonList(relevanceMatche),
                true, autoPriceParams);
    }

    protected RelevanceMatchAddOperation createAddOperationWithAutoPrices(
            List<AdGroupInfo> adGroupInfos, List<RelevanceMatch> relevanceMatches,
            ShowConditionAutoPriceParams autoPriceParams) {
        return getFullAddOperation(adGroupInfos, relevanceMatches, true, autoPriceParams);
    }

    protected RelevanceMatchAddOperation getFullAddOperation(List<AdGroupInfo> adGroupInfos,
                                                             List<RelevanceMatch> relevanceMatches, boolean autoPrices,
                                                             ShowConditionAutoPriceParams autoPriceParams) {

        Map<Long, Long> campaignIdsByAdGroupIds = StreamEx.of(adGroupInfos)
                .mapToEntry(AdGroupInfo::getAdGroupId, AdGroupInfo::getCampaignId)
                .toMap();
        RelevanceMatchAddContainer relevanceMatchAddOperationContainer = RelevanceMatchAddContainer
                .createRelevanceMatchAddOperationContainer(defaultUser.getUid(),
                        defaultUser.getClientInfo().getClientId(), campaignsByIds,
                        campaignIdsByAdGroupIds);
        return relevanceMatchService
                .createFullAddOperation(defaultUser.getClientInfo().getClient().getWorkCurrency().getCurrency(),
                        defaultUser.getClientInfo().getClientId(),
                        defaultUser.getUid(),
                        relevanceMatches,
                        relevanceMatchAddOperationContainer, autoPrices, autoPriceParams
                );
    }

    protected ClientId getClientId() {
        return clientInfo.getClientId();
    }

    protected Long getOperatorUid() {
        return defaultUser.getUid();
    }
}
