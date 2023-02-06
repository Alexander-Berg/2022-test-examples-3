package ru.yandex.direct.core.entity.relevancematch.repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.relevancematch.model.RelevanceMatch;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestRelevanceMatches;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.core.testing.steps.RelevanceMatchSteps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.operation.AddedModelId;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchRepositoryTest {
    @Autowired
    protected UserSteps userSteps;
    @Autowired
    protected CampaignSteps campaignSteps;
    @Autowired
    protected CampaignService campaignService;
    @Autowired
    protected AdGroupSteps adGroupSteps;
    @Autowired
    protected RelevanceMatchSteps relevanceMatchSteps;

    @Autowired
    private RelevanceMatchRepository relevanceMatchRepository;

    @Autowired
    private DslContextProvider dslContextProvider;


    private UserInfo defaultUser;
    private CampaignInfo activeCampaign;
    private AdGroupInfo defaultAdGroup;
    private RelevanceMatch relevanceMatch;

    @Before
    public void before() {
        defaultUser = userSteps.createDefaultUser();
        activeCampaign = campaignSteps.createActiveCampaign(defaultUser.getClientInfo());
        Long campaignId = activeCampaign.getCampaignId();
        defaultAdGroup = adGroupSteps.createAdGroup(defaultTextAdGroup(campaignId), activeCampaign);
        relevanceMatch = TestRelevanceMatches.defaultRelevanceMatch()
                .withCampaignId(campaignId)
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withAutobudgetPriority(3)
                .withIsDeleted(false)
                .withIsSuspended(true);
    }

    @Test
    public void addRelevanceMatches_addedCorrectly() {
        List<AddedModelId> relevanceMatchesAddedModelIds = relevanceMatchRepository
                .addRelevanceMatches(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(relevanceMatch), singleton(defaultAdGroup.getAdGroupId()));

        List<Long> relevanceMatchesIds = mapList(relevanceMatchesAddedModelIds, AddedModelId::getId);
        Map<Long, RelevanceMatch> relevanceMatchesByIds = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        relevanceMatchesIds);

        DefaultCompareStrategy compareStrategy = getCompareStrategy();
        assertThat(relevanceMatchesByIds.get(relevanceMatchesIds.get(0)), beanDiffer(relevanceMatch).useCompareStrategy(
                compareStrategy));
    }

    @Test
    public void addRelevanceMatches_adGroupHasDeletedRelevanceMatch_addedCorrectly() {
        AddedModelId addedRelevanceMatchId = relevanceMatchRepository
                .addRelevanceMatches(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(relevanceMatch.withIsDeleted(true)),
                        singleton(defaultAdGroup.getAdGroupId()))
                .get(0);

        RelevanceMatch toAdd = TestRelevanceMatches.defaultRelevanceMatch()
                .withAdGroupId(relevanceMatch.getAdGroupId())
                .withCampaignId(relevanceMatch.getCampaignId())
                .withAutobudgetPriority(relevanceMatch.getAutobudgetPriority());

        List<AddedModelId> relevanceMatchesAddedModelIds = relevanceMatchRepository
                .addRelevanceMatches(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(toAdd), singleton(defaultAdGroup.getAdGroupId()));

        assertThat(relevanceMatchesAddedModelIds.get(0), is(addedRelevanceMatchId));

        Map<Long, RelevanceMatch> result =
                relevanceMatchRepository.getRelevanceMatchesByAdGroupIds(defaultAdGroup.getShard(),
                        defaultAdGroup.getClientId(),
                        singleton(defaultAdGroup.getAdGroupId()));
        assertThat(result.size(), is(1));
        assertThat(result.get(defaultAdGroup.getAdGroupId()).getIsDeleted(), is(false));
    }

    @Test
    public void addRelevanceMatches_groupHasNotDeletedRelevanceMatch_NotUpdatedExistsRelevanceMatch() {
        AddedModelId addedRelevanceMatchId = relevanceMatchRepository
                .addRelevanceMatches(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(relevanceMatch),
                        singleton(defaultAdGroup.getAdGroupId()))
                .get(0);

        RelevanceMatch toAdd = TestRelevanceMatches.defaultRelevanceMatch()
                .withAdGroupId(defaultAdGroup.getAdGroupId())
                .withCampaignId(defaultAdGroup.getCampaignId())
                .withAutobudgetPriority(1);

        List<AddedModelId> relevanceMatchesAddedModelIds = relevanceMatchRepository
                .addRelevanceMatches(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(toAdd), singleton(defaultAdGroup.getAdGroupId()));

        AddedModelId expectedRelevanceMatchId = AddedModelId.ofExisting(addedRelevanceMatchId.getId());
        assertThat(relevanceMatchesAddedModelIds.get(0), is(expectedRelevanceMatchId));

        List<Long> ids = mapList(relevanceMatchesAddedModelIds, AddedModelId::getId);
        Map<Long, RelevanceMatch> relevanceMatchesByIds = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(), ids);

        DefaultCompareStrategy compareStrategy = getCompareStrategy();
        assertThat(relevanceMatchesByIds.get(ids.get(0)),
                beanDiffer(relevanceMatch).useCompareStrategy(compareStrategy));
    }

    @Test
    public void getRelevanceMatchByGroupIds() {
        relevanceMatchRepository
                .addRelevanceMatches(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(relevanceMatch), singleton(defaultAdGroup.getAdGroupId())
                );

        Map<Long, RelevanceMatch> relevanceMatchesByGroupIds =
                relevanceMatchRepository.getRelevanceMatchesByAdGroupIds(defaultUser.getShard(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(defaultAdGroup.getAdGroupId()));

        DefaultCompareStrategy compareStrategy = getCompareStrategy();
        assertThat(relevanceMatchesByGroupIds.get(defaultAdGroup.getAdGroupId()),
                beanDiffer(relevanceMatch).useCompareStrategy(
                        compareStrategy));
    }

    @Test
    public void getRelevanceMatchByGroupIdsWithoutDeleted_AdGroupDoesntHaveDeleted() {
        relevanceMatchRepository
                .addRelevanceMatches(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(relevanceMatch), singleton(defaultAdGroup.getAdGroupId())
                );

        Map<Long, RelevanceMatch> relevanceMatchesByGroupIds =
                relevanceMatchRepository.getRelevanceMatchesByAdGroupIds(defaultUser.getShard(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(defaultAdGroup.getAdGroupId()),
                        false);

        DefaultCompareStrategy compareStrategy = getCompareStrategy();
        assertThat(relevanceMatchesByGroupIds.get(defaultAdGroup.getAdGroupId()),
                beanDiffer(relevanceMatch).useCompareStrategy(
                        compareStrategy));
    }

    @Test
    public void getRelevanceMatchByGroupIdsWithDeleted_AdGroupDoesntHaveDeleted() {
        relevanceMatchRepository
                .addRelevanceMatches(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(relevanceMatch), singleton(defaultAdGroup.getAdGroupId())
                );

        Map<Long, RelevanceMatch> relevanceMatchesByGroupIds =
                relevanceMatchRepository.getRelevanceMatchesByAdGroupIds(defaultUser.getShard(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(defaultAdGroup.getAdGroupId()),
                        true);

        DefaultCompareStrategy compareStrategy = getCompareStrategy();
        assertThat(relevanceMatchesByGroupIds.get(defaultAdGroup.getAdGroupId()),
                beanDiffer(relevanceMatch).useCompareStrategy(
                        compareStrategy));
    }

    @Test
    public void getRelevanceMatchByGroupIdsWithoutDeleted_AdGroupHasDeleted() {
        relevanceMatchRepository
                .addRelevanceMatches(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(relevanceMatch.withIsDeleted(true)), singleton(defaultAdGroup.getAdGroupId())
                );

        Map<Long, RelevanceMatch> relevanceMatchesByGroupIds =
                relevanceMatchRepository.getRelevanceMatchesByAdGroupIds(defaultUser.getShard(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(defaultAdGroup.getAdGroupId()),
                        false);

        assertThat(relevanceMatchesByGroupIds.size(), is(0));
    }

    @Test
    public void getRelevanceMatchByGroupIdsWithDeleted_AdGroupHasDeleted() {
        relevanceMatchRepository
                .addRelevanceMatches(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(relevanceMatch.withIsDeleted(true)), singleton(defaultAdGroup.getAdGroupId())
                );

        Map<Long, RelevanceMatch> relevanceMatchesByGroupIds =
                relevanceMatchRepository.getRelevanceMatchesByAdGroupIds(defaultUser.getShard(),
                        defaultUser.getClientInfo().getClientId(),
                        singletonList(defaultAdGroup.getAdGroupId()),
                        true);

        DefaultCompareStrategy compareStrategy = getCompareStrategy();
        assertThat(relevanceMatchesByGroupIds.get(defaultAdGroup.getAdGroupId()),
                beanDiffer(relevanceMatch).useCompareStrategy(
                        compareStrategy));
    }


    @Test
    public void updateRelevanceMatch() {
        int autobudgetPriority = 7;

        List<Long> relevanceMatchIds = relevanceMatchSteps
                .addRelevanceMatchToAdGroup(Collections.singletonList(relevanceMatch), defaultAdGroup);

        ModelChanges<RelevanceMatch> modelChanges = new ModelChanges<>(relevanceMatchIds.get(0), RelevanceMatch.class);
        modelChanges.process(autobudgetPriority, RelevanceMatch.AUTOBUDGET_PRIORITY);
        relevanceMatchRepository
                .update(dslContextProvider.ppc(defaultUser.getShard()).configuration(),
                        Collections.singletonList(modelChanges.applyTo(relevanceMatch)));

        Map<Long, RelevanceMatch> relevanceMatchesByIds = relevanceMatchRepository
                .getRelevanceMatchesByIds(defaultUser.getShard(), defaultUser.getClientInfo().getClientId(),
                        relevanceMatchIds);

        DefaultCompareStrategy compareStrategy = getCompareStrategy();
        RelevanceMatch expectedRelevanceMatch = relevanceMatch.withAutobudgetPriority(autobudgetPriority);
        assertThat(relevanceMatchesByIds.get(relevanceMatchIds.get(0)),
                beanDiffer(expectedRelevanceMatch).useCompareStrategy(
                        compareStrategy));
    }


    private DefaultCompareStrategy getCompareStrategy() {
        return DefaultCompareStrategies.allFieldsExcept(
                BeanFieldPath.newPath("id"),
                BeanFieldPath.newPath("statusBsSynced"),
                BeanFieldPath.newPath("lastChangeTime")
        );
    }
}
