package ru.yandex.direct.core.entity.relevancematch.service.modifyoperation;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import one.util.streamex.StreamEx;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignsAutobudget;
import ru.yandex.direct.core.entity.campaign.model.DbStrategy;
import ru.yandex.direct.core.entity.relevancematch.container.RelevanceMatchAddContainer;
import ru.yandex.direct.core.entity.relevancematch.container.RelevanceMatchModification;
import ru.yandex.direct.core.entity.relevancematch.container.RelevanceMatchModificationResult;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchMapping;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModifyOperation;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.operation.AddedModelId;
import ru.yandex.direct.result.Result;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchModifyOperationTest extends RelevanceMatchModificationBaseTest {
    @Override
    protected AdGroup getAdGroup() {
        return defaultTextAdGroup(activeCampaign.getCampaignId());
    }

    @Test
    public void addRelevanceMatch_Prepare_Success() {
        List<RelevanceMatch> relevanceMatchListToAdd = Collections.singletonList(new RelevanceMatch()
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withPrice(BigDecimal.TEN)
                .withPriceContext(BigDecimal.TEN.add(BigDecimal.ONE))
                .withIsSuspended(true));

        RelevanceMatchModification relevanceMatchModification = new RelevanceMatchModification()
                .withRelevanceMatchAdd(relevanceMatchListToAdd)
                .withRelevanceMatchUpdate(Collections.emptyList())
                .withRelevanceMatchIdsDelete(Collections.emptyList());

        RelevanceMatchModifyOperation relevanceMatchModifyOperation =
                getFullModifyOperation(relevanceMatchModification);

        Optional<Result<RelevanceMatchModificationResult>> prepare = relevanceMatchModifyOperation.prepare();
        assertThat(prepare.isPresent()).isFalse();

    }

    @Test
    public void addRelevanceMatch_Apply_Success() {
        List<RelevanceMatch> relevanceMatchListToAdd = Collections.singletonList(new RelevanceMatch()
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withPrice(BigDecimal.TEN)
                .withPriceContext(BigDecimal.TEN.add(BigDecimal.ONE))
                .withIsSuspended(true));

        RelevanceMatchModification relevanceMatchModification = new RelevanceMatchModification()
                .withRelevanceMatchAdd(relevanceMatchListToAdd)
                .withRelevanceMatchUpdate(Collections.emptyList())
                .withRelevanceMatchIdsDelete(Collections.emptyList());

        RelevanceMatchModifyOperation relevanceMatchModifyOperation =
                getFullModifyOperation(relevanceMatchModification);

        Result<RelevanceMatchModificationResult> result = relevanceMatchModifyOperation.prepareAndApply();
        Assert.assertThat(result.getResult().getRelevanceMatchAddResult(), not(empty()));
    }

    @Test
    public void fullModifyRelevanceMatch_Apply_Success() {
        CampaignInfo secondActiveCampaignInfo = campaignSteps.createActiveCampaign(defaultUser.getClientInfo());
        CampaignInfo thirdActiveCampaignInfo = campaignSteps.createActiveCampaign(defaultUser.getClientInfo());

        AdGroupInfo secondCampsAdGroup = adGroupSteps.createAdGroup(getAdGroup(), secondActiveCampaignInfo);
        AdGroupInfo thirdCampsAdGroup = adGroupSteps.createAdGroup(getAdGroup(), thirdActiveCampaignInfo);

        DbStrategy secondCampaignStrategy = new DbStrategy();
        secondCampaignStrategy.setAutobudget(CampaignsAutobudget.YES);
        Campaign secondCampaign = new Campaign()
                .withId(secondActiveCampaignInfo.getCampaignId())
                .withAutobudget(secondCampaignStrategy.isAutoBudget())
                .withStrategy(secondCampaignStrategy)
                .withType(CampaignType.TEXT)
                .withCurrency(CurrencyCode.RUB);
        Campaign thirdCampaign = new Campaign()
                .withId(thirdActiveCampaignInfo.getCampaignId())
                .withAutobudget(false)
                .withCurrency(CurrencyCode.RUB)
                .withStrategy(new DbStrategy())
                .withType(CampaignType.TEXT)
                .withCurrency(CurrencyCode.RUB);
        campaignsByIds.put(secondActiveCampaignInfo.getCampaignId(), secondCampaign);
        campaignsByIds.put(thirdActiveCampaignInfo.getCampaignId(), thirdCampaign);
        adGroupByIds.put(secondCampsAdGroup.getAdGroupId(), secondCampsAdGroup.getAdGroup());
        adGroupByIds.put(thirdCampsAdGroup.getAdGroupId(), thirdCampsAdGroup.getAdGroup());
        campaignIdsByAdGroupIds.put(secondCampsAdGroup.getAdGroupId(), secondCampsAdGroup.getCampaignId());
        campaignIdsByAdGroupIds.put(thirdCampsAdGroup.getAdGroupId(), thirdCampsAdGroup.getCampaignId());


        List<RelevanceMatch> relevanceMatchListToAdd = Collections.singletonList(new RelevanceMatch()
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withPrice(BigDecimal.TEN)
                .withPriceContext(BigDecimal.TEN.add(BigDecimal.ONE))
                .withIsSuspended(true));
        RelevanceMatch relevanceMatchToDelete = getValidRelevanceMatch()
                .withAdGroupId(secondCampsAdGroup.getAdGroupId());
        RelevanceMatch relevanceMatchToUpdate = new RelevanceMatch()
                .withAdGroupId(thirdCampsAdGroup.getAdGroupId())
                .withPrice(BigDecimal.ONE)
                .withPriceContext(BigDecimal.TEN.add(BigDecimal.ONE))
                .withIsSuspended(true);

        RelevanceMatchAddContainer relevanceMatchAddOperationContainer = RelevanceMatchAddContainer
                .createRelevanceMatchAddOperationContainer(
                        defaultUser.getUid(), defaultUser.getClientInfo().getClientId(), campaignsByIds,
                        campaignIdsByAdGroupIds);

        List<Result<AddedModelId>> r = relevanceMatchService
                .createFullAddOperation(defaultUser.getClientInfo().getClient().getWorkCurrency().getCurrency(),
                        defaultUser.getClientInfo().getClientId(),
                        defaultUser.getUid(),
                        Arrays.asList(relevanceMatchToDelete, relevanceMatchToUpdate),
                        relevanceMatchAddOperationContainer, false, null).prepareAndApply()
                .getResult();
        List<Long> relevanceMatchIds = StreamEx.of(r)
                .map(Result::getResult)
                .map(AddedModelId::getId)
                .toList();

        List<RelevanceMatch> relevanceMatchListToUpdate = Collections.singletonList(relevanceMatchToUpdate
                .withId(relevanceMatchIds.get(1))
                .withPrice(BigDecimal.TEN)
                .withPriceContext(BigDecimal.TEN.add(BigDecimal.ONE))
        );

        RelevanceMatchModification relevanceMatchModification =
                new RelevanceMatchModification()
                        .withRelevanceMatchAdd(relevanceMatchListToAdd)
                        .withRelevanceMatchUpdate(RelevanceMatchMapping
                                .relevanceMatchesToCoreModelChanges(relevanceMatchListToUpdate))
                        .withRelevanceMatchIdsDelete(relevanceMatchIds.subList(0, 1));

        RelevanceMatchModifyOperation relevanceMatchModifyOperation =
                getFullModifyOperation(relevanceMatchModification);

        Result<RelevanceMatchModificationResult> result = relevanceMatchModifyOperation.prepareAndApply();
        Assert.assertThat(result.isSuccessful(), equalTo(true));
    }

    @Test
    public void addRelevanceMatchWithInvalidPrice_Apply_Error() {
        List<RelevanceMatch> relevanceMatchListToAdd = Collections.singletonList(new RelevanceMatch()
                .withCampaignId(activeCampaign.getCampaignId())
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withPrice(BigDecimal.ZERO)
                .withIsSuspended(true));

        RelevanceMatchModification relevanceMatchModification = new RelevanceMatchModification()
                .withRelevanceMatchAdd(relevanceMatchListToAdd)
                .withRelevanceMatchUpdate(Collections.emptyList())
                .withRelevanceMatchIdsDelete(Collections.emptyList());

        RelevanceMatchModifyOperation relevanceMatchModifyOperation =
                getFullModifyOperation(relevanceMatchModification);

        Result<RelevanceMatchModificationResult> result = relevanceMatchModifyOperation.prepareAndApply();
        Assert.assertThat(result.getErrors(), not(empty()));
    }

    @Test
    public void addRelevanceMatchToOtherClientsGroup_Apply_Error() {
        UserInfo otherUser = userSteps.createDefaultUser();
        AdGroupInfo otherClientAdGroup = adGroupSteps.createActiveTextAdGroup(otherUser.getClientInfo());
        List<RelevanceMatch> relevanceMatchListToAdd = Collections.singletonList(new RelevanceMatch()
                .withAdGroupId(otherClientAdGroup.getAdGroupId())
                .withPrice(BigDecimal.ZERO)
                .withIsSuspended(true));

        RelevanceMatchModification relevanceMatchModification = new RelevanceMatchModification()
                .withRelevanceMatchAdd(relevanceMatchListToAdd)
                .withRelevanceMatchUpdate(Collections.emptyList())
                .withRelevanceMatchIdsDelete(Collections.emptyList());

        RelevanceMatchModifyOperation relevanceMatchModifyOperation =
                getFullModifyOperation(relevanceMatchModification);

        Result<RelevanceMatchModificationResult> result = relevanceMatchModifyOperation.prepareAndApply();
        Assert.assertThat(result.getErrors(), not(empty()));
    }
}
