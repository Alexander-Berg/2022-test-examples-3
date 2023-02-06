package ru.yandex.direct.core.entity.adgroup.service;

import java.util.List;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignModifyRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.entity.minuskeywordspack.repository.MinusKeywordsPackRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.result.Result;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.clientTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

public class AdGroupsAddOperationTestBase {

    @Autowired
    protected Steps steps;

    @Autowired
    protected DslContextProvider dslContextProvider;

    @Autowired
    protected CampaignSteps campaignSteps;

    @Autowired
    protected CampaignRepository campaignRepository;

    @Autowired
    protected AdGroupRepository adGroupRepository;

    @Autowired
    protected MinusKeywordsPackRepository minusKeywordsPackRepository;

    @Autowired
    protected GeoTreeFactory geoTreeFactory;

    @Autowired
    protected CampaignModifyRepository campaignModifyRepository;

    @Autowired
    private AdGroupsAddOperationFactory addOperationFactory;

    protected ClientInfo clientInfo;
    protected CampaignInfo campaignInfo;
    protected Long campaignId;
    protected long operatorUid;
    protected ClientId clientId;
    protected GeoTree geoTree;
    protected int shard;

    @Before
    public void before() {
        geoTree = geoTreeFactory.getGlobalGeoTree();

        campaignInfo = createModeratedCampaign();
        campaignId = campaignInfo.getCampaignId();

        initClientData(campaignInfo.getClientInfo());
    }

    protected void initClientData(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
        this.operatorUid = clientInfo.getUid();
        this.clientId = clientInfo.getClientId();
        this.shard = clientInfo.getShard();
    }

    protected AdGroup adGroupWithInvalidName() {
        return clientTextAdGroup(campaignInfo.getCampaignId()).withName("");
    }

    protected AdGroup adGroupWithInvalidType() {
        return clientTextAdGroup(campaignInfo.getCampaignId()).withType(AdGroupType.DYNAMIC);
    }

    protected AdGroup adGroupWithInvalidMinusKeywords() {
        return clientTextAdGroup(campaignInfo.getCampaignId()).withMinusKeywords(singletonList("[]"));
    }

    protected CampaignInfo createModeratedCampaign() {
        Campaign campaign = activeTextCampaign(null, null)
                .withStatusModerate(StatusModerate.YES);
        return campaignSteps.createCampaign(new CampaignInfo().withCampaign(campaign));
    }

    protected CampaignInfo createDraftCampaign() {
        Campaign draftCampaign = newTextCampaign(null, null)
                .withStatusModerate(StatusModerate.NEW);
        return campaignSteps.createCampaign(draftCampaign, clientInfo);
    }

    protected CampaignInfo createRejectedCampaign() {
        Campaign draftCampaign = newTextCampaign(null, null)
                .withStatusModerate(StatusModerate.NO);
        return campaignSteps.createCampaign(draftCampaign, clientInfo);
    }

    protected AdGroupsAddOperation createAddOperation(Applicability applicability, List<AdGroup> models) {
        return createAddOperation(applicability, models, operatorUid, clientId, geoTree, shard, true);
    }

    protected AdGroupsAddOperation createAddOperation(List<AdGroup> models, boolean saveDraft) {
        return createAddOperation(Applicability.FULL, models, operatorUid, clientId, geoTree, shard, saveDraft);
    }

    protected AdGroupsAddOperation createFullAddOperation(List<AdGroup> models) {
        return createAddOperation(Applicability.FULL, models, operatorUid, clientId, geoTree, shard, true);
    }

    protected AdGroupsAddOperation createAddOperation(Applicability applicability,
                                                      List<AdGroup> models, long operatorUid, ClientId clientId, GeoTree geoTree, int shard, boolean saveDraft) {
        return addOperationFactory.newInstance(applicability, models, geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE, operatorUid, clientId, shard, saveDraft,
                true);
    }

    protected List<Long> addAndCheckResultIsSuccessful(AdGroup adGroup, boolean saveDraft) {
        return addAndCheckResultIsSuccessful(singletonList(adGroup), saveDraft);
    }

    protected List<Long> addAndCheckResultIsSuccessful(List<AdGroup> adGroups, boolean saveDraft) {
        AdGroupsAddOperation operation = createAddOperation(adGroups, saveDraft);
        MassResult<Long> result = operation.prepareAndApply();
        result.getValidationResult().flattenErrors().forEach(System.out::println);
        assumeThat(result, isFullySuccessful());
        return mapList(result.getResult(), Result::getResult);
    }
}
