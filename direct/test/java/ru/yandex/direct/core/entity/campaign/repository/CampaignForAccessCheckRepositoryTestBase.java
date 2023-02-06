package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.CampaignForAccessCheck;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignTypeKinds;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignForAccessCheckRepositoryAdapter;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.CampaignSubObjectRetriever;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public abstract class CampaignForAccessCheckRepositoryTestBase<T extends CampaignForAccessCheck> {

    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public BiFunction<Steps, CampaignInfo, Long> entitySupplier;

    @Parameterized.Parameter(2)
    public Function<CampaignAccessCheckRepository, CampaignSubObjectRetriever<T>> subObjectRetrieverSupplier;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                    "Relevance match",
                        (BiFunction<Steps, CampaignInfo, Long>)
                                CampaignForAccessCheckRepositoryTestBase::createRelevanceMatch,
                        (Function<CampaignAccessCheckRepository, CampaignSubObjectRetriever>)
                                (t-> ((CampaignAccessCheckRepository) t)::getCampaignsWithTypeByRelevanceMatchIds)
                },
                {
                    "Dyn condition domain type",
                        (BiFunction<Steps, CampaignInfo, Long>)
                                CampaignForAccessCheckRepositoryTestBase::createDomainDynamicCondition,
                        (Function<CampaignAccessCheckRepository, CampaignSubObjectRetriever>)
                                (t-> ((CampaignAccessCheckRepository) t)::getCampaignsByDynCondIdsFromDomainTypeAdgroup)
                },
                {
                    "Dyn condition feed type",
                        (BiFunction<Steps, CampaignInfo, Long>)
                                CampaignForAccessCheckRepositoryTestBase::createFeedDynamicCondition,
                        (Function<CampaignAccessCheckRepository, CampaignSubObjectRetriever>)
                                (t-> ((CampaignAccessCheckRepository) t)::getCampaignsByDynCondIdsFromFeedTypeAdgroup)
                },
                {
                    "Vcard",
                        (BiFunction<Steps, CampaignInfo, Long>)
                                CampaignForAccessCheckRepositoryTestBase::createVcard,
                        (Function<CampaignAccessCheckRepository, CampaignSubObjectRetriever>)
                                (t-> ((CampaignAccessCheckRepository) t)::getCampaignsWithTypeByVcardIds)
                },
                {
                    "Bid",
                        (BiFunction<Steps, CampaignInfo, Long>)
                                CampaignForAccessCheckRepositoryTestBase::createBid,
                        (Function<CampaignAccessCheckRepository, CampaignSubObjectRetriever>)
                                (t-> ((CampaignAccessCheckRepository) t)::getCampaignsWithTypeByBidIds)
                },
                {
                    "Dyn cond id",
                        (BiFunction<Steps, CampaignInfo, Long>)
                                CampaignForAccessCheckRepositoryTestBase::createDynCondId,
                        (Function<CampaignAccessCheckRepository, CampaignSubObjectRetriever>)
                                (t-> ((CampaignAccessCheckRepository) t)::getCampaignsByDynCondIds)
                },
                {
                    "Banner",
                        (BiFunction<Steps, CampaignInfo, Long>)
                                CampaignForAccessCheckRepositoryTestBase::createBanner,
                        (Function<CampaignAccessCheckRepository, CampaignSubObjectRetriever>)
                                (t-> ((CampaignAccessCheckRepository) t)::getCampaignsForAccessCheckByBannerIds)
                },
                {
                    "AdGroup",
                        (BiFunction<Steps, CampaignInfo, Long>)
                                CampaignForAccessCheckRepositoryTestBase::createAdGroup,
                        (Function<CampaignAccessCheckRepository, CampaignSubObjectRetriever>)
                                (t-> ((CampaignAccessCheckRepository) t)::getCampaignsForAccessCheckByAdGroupIds)
                },
                {
                    "Campaign",
                        (BiFunction<Steps, CampaignInfo, Long>)
                                (a, b) -> ((CampaignInfo) b).getCampaignId(),
                        (Function<CampaignAccessCheckRepository, CampaignSubObjectRetriever>)
                                (t-> ((CampaignAccessCheckRepository) t)::getCampaignsForAccessCheckByCampaignIds)
                },
        });
    }

    @Autowired
    protected Steps steps;

    @Autowired
    protected CampaignAccessCheckRepository campaignAccessCheckRepository;

    protected abstract
    CampaignForAccessCheckRepositoryAdapter<T> getAllClientCampaignsForAccessCheckRepositoryAdapter(ClientId clientId);

    protected abstract CampaignForAccessCheckRepositoryAdapter<T> getAllowableCampaignsForAccessCheckRepositoryAdapter(
            ClientId clientId, Set<CampaignType> campaignTypes);

    @Test
    public void twoCampaignsTwoClients_OneCampaignReturnedForClient() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        Long entityId1 = entitySupplier.apply(steps, campaignInfo);
        Long entityId2 = entitySupplier.apply(steps, steps.campaignSteps().createDefaultCampaign());

        Map<Long, T> campaignsByEnityIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getAllClientCampaignsForAccessCheckRepositoryAdapter(campaignInfo.getClientId()),
                        asList(entityId1, entityId2));

        assertThat(campaignsByEnityIdMap.keySet(), contains(entityId1));
        assertThat(campaignsByEnityIdMap.keySet(), not(contains(entityId2)));
    }

    @Test
    public void invalidClient_CampaignNotReturned() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        Long entityId = entitySupplier.apply(steps, campaignInfo);

        Map<Long, T> campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getAllowableCampaignsForAccessCheckRepositoryAdapter(
                                ClientId.fromLong(campaignInfo.getClientId().asLong() + 1), CampaignTypeKinds.BASE),
                        asList(entityId));

        assertThat(campaignsByIdMap.keySet().isEmpty(), is(true));
    }

    @Test
    public void allowableClientBaseCampaignTypes_CampaignReturned() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        Long entityId = entitySupplier.apply(steps, campaignInfo);

        Map<Long, T> campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getAllowableCampaignsForAccessCheckRepositoryAdapter(
                                campaignInfo.getClientId(), CampaignTypeKinds.BASE),
                        asList(entityId));

        assertThat(campaignsByIdMap.keySet(), contains(entityId));
    }

    @Test
    public void allowableClientAllCampaignTypes_CampaignReturned() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        Long entityId = entitySupplier.apply(steps, campaignInfo);

        Map<Long, T> campaignsByIdMap = subObjectRetrieverSupplier
                .apply(campaignAccessCheckRepository).get(
                        campaignInfo.getShard(),
                        getAllClientCampaignsForAccessCheckRepositoryAdapter(campaignInfo.getClientId()),
                        asList(entityId));

        assertThat(campaignsByIdMap.keySet(), contains(entityId));
    }

    private static Long createRelevanceMatch(Steps stepsLocal, CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = stepsLocal.adGroupSteps().createDefaultAdGroup(campaignInfo);
        return stepsLocal.relevanceMatchSteps().addDefaultRelevanceMatchToAdGroup(adGroupInfo);
    }

    private static Long createDomainDynamicCondition(Steps stepsLocal, CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = stepsLocal.adGroupSteps().createActiveDynamicTextAdGroup(campaignInfo);
        return stepsLocal.dynamicTextAdTargetsSteps().createDefaultDynamicTextAdTarget(adGroupInfo)
                .getDynamicConditionId();
    }

    private static Long createFeedDynamicCondition(Steps stepsLocal, CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = stepsLocal.adGroupSteps().createActiveDynamicFeedAdGroup(campaignInfo);
        return stepsLocal.dynamicTextAdTargetsSteps().createDefaultDynamicFeedAdTarget(adGroupInfo)
                .getDynamicConditionId();
    }

    private static Long createVcard(Steps stepsLocal, CampaignInfo campaignInfo) {
        return stepsLocal.vcardSteps().createVcard(campaignInfo).getVcardId();
    }

    private static Long createBid(Steps stepsLocal, CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = stepsLocal.adGroupSteps().createDefaultAdGroup(campaignInfo);
        return stepsLocal.keywordSteps().createKeyword(adGroupInfo).getId();
    }

    private static Long createDynCondId(Steps stepsLocal, CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = stepsLocal.adGroupSteps().createDefaultAdGroup(campaignInfo);
        return stepsLocal.dynamicTextAdTargetsSteps().createDefaultDynamicTextAdTarget(adGroupInfo)
                .getDynamicConditionId();
    }

    private static Long createBanner(Steps stepsLocal, CampaignInfo campaignInfo) {
        AdGroupInfo adGroupInfo = stepsLocal.adGroupSteps().createDefaultAdGroup(campaignInfo);
        return stepsLocal.bannerSteps().createDefaultBanner(adGroupInfo).getBannerId();
    }

    private static Long createAdGroup(Steps stepsLocal, CampaignInfo campaignInfo) {
        return stepsLocal.adGroupSteps().createDefaultAdGroup(campaignInfo).getAdGroupId();
    }
}
