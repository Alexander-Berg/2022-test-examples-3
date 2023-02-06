package ru.yandex.direct.core.entity.banner.type.organization;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainer;
import ru.yandex.direct.core.entity.banner.container.BannersUpdateOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.BannerWithOrganization;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.MetrikaCounter;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampMetrikaCountersRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.entity.strategy.model.StrategyWithMetrikaCounters;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.VcardInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.repository.TestOrganizationRepository;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbschema.ppc.enums.MetrikaCountersSource;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.entity.banner.model.BannerWithOrganization.PERMALINK_ID;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestBanners.fillTextDefaultSystemFields;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;
import static ru.yandex.direct.core.testing.data.TestVcards.fullVcard;
import static ru.yandex.direct.dbschema.ppc.Tables.METRIKA_COUNTERS;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationUpdateCampaignCountersTest extends
        BannerOldBannerInfoUpdateOperationTestBase<OldTextBanner> {

    private long permalinkIdFirst = -1L;
    private long permalinkIdSecond = -1L;
    private long permalinkIdThird = -1L;
    private long otherPermalinkId = -1L;
    private static final long FIRST_METRIKA_COUNTER_ID = 2L;
    private static final long SECOND_METRIKA_COUNTER_ID = 5L;
    private static final long THIRD_METRIKA_COUNTER_ID = 6L;
    private static final long OTHER_METRIKA_COUNTER_ID = 113L;
    private static final long CAMPAIGN_COUNTER_ID = 99999L;

    @Autowired
    public TestOrganizationRepository testOrganizationRepository;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    private BannerWithOrganizationUpdateOperationTypeSupport bannerWithOrganizationUpdateOperationTypeSupport;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    DslContextProvider contextProvider;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    OrganizationRepository organizationRepository;

    @Autowired
    CampMetrikaCountersRepository campMetrikaCountersRepository;

    private ClientInfo clientInfo;

    private AdGroupInfo adGroupInfo;

    private TextCampaignInfo textCampaignInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var clientId = clientInfo.getClientId();
        var organization1 = defaultActiveOrganization(clientId);
        var organization2 = defaultActiveOrganization(clientId);
        var organization3 = defaultActiveOrganization(clientId);
        var organizationOther = defaultActiveOrganization(clientId);
        var chiefUid = rbacService.getChiefByClientId(clientId);
        permalinkIdFirst = organization1.getPermalinkId();
        permalinkIdSecond = organization2.getPermalinkId();
        permalinkIdThird = organization3.getPermalinkId();
        otherPermalinkId = organizationOther.getPermalinkId();
        List<Long> chiefUids = List.of(chiefUid);
        organizationsClient.addUidsAndCounterIdsByPermalinkId(permalinkIdFirst, chiefUids, FIRST_METRIKA_COUNTER_ID);
        organizationsClient.addUidsAndCounterIdsByPermalinkId(permalinkIdSecond, chiefUids, SECOND_METRIKA_COUNTER_ID);
        organizationsClient.addUidsAndCounterIdsByPermalinkId(permalinkIdThird, chiefUids, THIRD_METRIKA_COUNTER_ID);
        organizationsClient.addUidsAndCounterIdsByPermalinkId(otherPermalinkId, chiefUids, OTHER_METRIKA_COUNTER_ID);

        var campaignMetrikaCounter = new MetrikaCounter()
                .withId(CAMPAIGN_COUNTER_ID)
                .withHasEcommerce(true);

        campMetrikaCountersRepository.updateMetrikaCounters(adGroupInfo.getShard(),
                Map.of(adGroupInfo.getCampaignId(), List.of(campaignMetrikaCounter)));

        reset(organizationsClient);
    }

    @Test
    public void addNewPermalinkToExistingBanner_FeatureDisabled_CounterIdNotAddedToCampaign() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, false);
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo);

        updateMetrikaCounters(bannerInfo);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdFirst, PERMALINK_ID);
        prepareAndApplyValid(modelChanges);

        checkCampaignCountersHasCounter(clientInfo.getShard(), bannerInfo.getCampaignId(),
                List.of(CAMPAIGN_COUNTER_ID));
        verify(organizationsClient, times(0)).getOrganizationsCountersData(any(), any(), any());
    }

    @Test
    public void addNewPermalinkToExistingBanner_CounterIdIsAddedToCampaign() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner(), adGroupInfo);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdFirst, PERMALINK_ID);
        prepareAndApplyValid(modelChanges);

        checkCampaignCountersHasCounter(clientInfo.getShard(), bannerInfo.getCampaignId(),
                List.of(FIRST_METRIKA_COUNTER_ID, CAMPAIGN_COUNTER_ID));
        checkSpravCounterSource(bannerInfo.getCampaignId(), FIRST_METRIKA_COUNTER_ID,
                MetrikaCountersSource.sprav);
        checkSpravCounterSource(bannerInfo.getCampaignId(), CAMPAIGN_COUNTER_ID,
                MetrikaCountersSource.unknown);
    }

    @Test
    public void addNewPermalinkIdsToExistingBanners_CounterIdIsAddedToCampaign() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner()
                .withPermalinkId(permalinkIdFirst), adGroupInfo);
        TextBannerInfo secondActiveTextBanner = steps.bannerSteps().createActiveTextBanner(
                bannerInfo.getCampaignInfo());
        var adGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        TextBannerInfo thirdActiveTextBanner = steps.bannerSteps().createActiveTextBanner(adGroupInfo2);

        var modelChanges1 = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(null, PERMALINK_ID);
        var modelChanges2 = new ModelChanges<>(secondActiveTextBanner.getBannerId(), TextBanner.class)
                .process(permalinkIdSecond, PERMALINK_ID);
        var modelChanges3 = new ModelChanges<>(thirdActiveTextBanner.getBannerId(), TextBanner.class)
                .process(permalinkIdFirst, PERMALINK_ID);

        prepareAndApplyValid(List.of(modelChanges1, modelChanges2, modelChanges3));

        checkCampaignCountersHasCounter(clientInfo.getShard(), bannerInfo.getCampaignId(),
                List.of(SECOND_METRIKA_COUNTER_ID, CAMPAIGN_COUNTER_ID));
        checkSpravCounterSource(bannerInfo.getCampaignId(), SECOND_METRIKA_COUNTER_ID,
                MetrikaCountersSource.sprav);
        checkSpravCounterSource(bannerInfo.getCampaignId(), CAMPAIGN_COUNTER_ID,
                MetrikaCountersSource.unknown);
    }

    @Test
    public void updateBanner_FeatureEnabledAndPermalinkUnchanged_CounterIdNotAddedToCampaign() {
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner()
                .withPermalinkId(permalinkIdFirst), adGroupInfo);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdFirst, PERMALINK_ID);
        prepareAndApplyValid(modelChanges);

        checkCampaignCountersHasCounter(clientInfo.getShard(), bannerInfo.getCampaignId(),
                List.of(CAMPAIGN_COUNTER_ID));
        checkSpravCounterSource(bannerInfo.getCampaignId(), CAMPAIGN_COUNTER_ID,
                MetrikaCountersSource.unknown);
    }

    @Test
    public void updateBannerWithNewPermalink_FeatureEnabled_CounterIdAddedToCampaign() {
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner()
                .withPermalinkId(permalinkIdFirst), adGroupInfo);
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdSecond, PERMALINK_ID);
        prepareAndApplyValid(modelChanges);

        checkCampaignCountersHasCounter(clientInfo.getShard(), bannerInfo.getCampaignId(),
                List.of(CAMPAIGN_COUNTER_ID, SECOND_METRIKA_COUNTER_ID));
        checkSpravCounterSource(bannerInfo.getCampaignId(), CAMPAIGN_COUNTER_ID,
                MetrikaCountersSource.unknown);
        checkSpravCounterSource(bannerInfo.getCampaignId(), SECOND_METRIKA_COUNTER_ID,
                MetrikaCountersSource.sprav);
    }

    @Test
    public void addBannerPermalinkToBanner_CounterAlreadyAddedToCampaign_CounterIdIsNotAddedToCampaign() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner()
                .withPermalinkId(permalinkIdFirst), adGroupInfo);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(permalinkIdFirst, PERMALINK_ID);
        prepareAndApplyValid(modelChanges);

        checkCampaignCountersHasCounter(clientInfo.getShard(), bannerInfo.getCampaignId(),
                List.of(CAMPAIGN_COUNTER_ID, FIRST_METRIKA_COUNTER_ID));
        checkSpravCounterSource(bannerInfo.getCampaignId(), FIRST_METRIKA_COUNTER_ID,
                MetrikaCountersSource.sprav);
        checkSpravCounterSource(bannerInfo.getCampaignId(), CAMPAIGN_COUNTER_ID,
                MetrikaCountersSource.unknown);
    }

    @Test
    public void deleteBannerPermalinkToBanner_PermalinkWasOnlyOneForCampaign_CounterIdIsDeletedToCampaign() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner()
                .withPermalinkId(permalinkIdFirst), adGroupInfo);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(null, PERMALINK_ID);
        prepareAndApplyValid(modelChanges);

        checkCampaignCountersHasCounter(clientInfo.getShard(), bannerInfo.getCampaignId(),
                List.of(CAMPAIGN_COUNTER_ID));
        checkSpravCounterSource(bannerInfo.getCampaignId(), CAMPAIGN_COUNTER_ID,
                MetrikaCountersSource.unknown);
    }

    @Test
    public void deleteBannerPermalinkToBanner_PermalinkWasNotOnlyOneForCampaign_CounterIdIsNotDeletedToCampaign() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner()
                .withPermalinkId(permalinkIdFirst), adGroupInfo);

        OldTextBanner banner = activeTextBanner(null, null)
                .withPermalinkId(permalinkIdFirst);
        fillTextDefaultSystemFields(banner);
        TextBannerInfo secondActiveTextBanner = steps.bannerSteps().createBannerInActiveTextAdGroup(new TextBannerInfo()
                .withBanner(banner)
                .withCampaignInfo(bannerInfo.getCampaignInfo()));

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(null, PERMALINK_ID);
        prepareAndApplyValid(modelChanges);

        checkCampaignCountersHasCounter(clientInfo.getShard(), bannerInfo.getCampaignId(),
                List.of(FIRST_METRIKA_COUNTER_ID, CAMPAIGN_COUNTER_ID));
        checkSpravCounterSource(bannerInfo.getCampaignId(), CAMPAIGN_COUNTER_ID,
                MetrikaCountersSource.unknown);
    }

    @Test
    public void updateTwoBannersOfDifferentCampaigns_CampaignsHasPermalinks_CampaignsCountersUpdateCorrectly() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(),
                FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);
        bannerInfo = steps.bannerSteps().createBanner(activeTextBanner()
                .withPermalinkId(permalinkIdFirst), adGroupInfo);

        OldTextBanner banner = activeTextBanner(null, null)
                .withPermalinkId(permalinkIdSecond);
        fillTextDefaultSystemFields(banner);
        TextBannerInfo secondActiveTextBannerOfFirstCampaign = steps.bannerSteps().createBannerInActiveTextAdGroup(
                new TextBannerInfo()
                        .withBanner(banner)
                        .withCampaignInfo(bannerInfo.getCampaignInfo()));

        OldTextBanner banner2 = activeTextBanner(null, null)
                .withPermalinkId(permalinkIdThird);
        fillTextDefaultSystemFields(banner);
        TextBannerInfo thirdActiveTextBannerOfFirstCampaign = steps.bannerSteps().createBannerInActiveTextAdGroup(
                new TextBannerInfo()
                        .withBanner(banner2)
                        .withCampaignInfo(bannerInfo.getCampaignInfo()));

        var adGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        OldTextBanner banner3 = activeTextBanner(null, null)
                .withPermalinkId(permalinkIdFirst);
        fillTextDefaultSystemFields(banner);
        TextBannerInfo firstActiveTextBannerOfSecondCampaign = steps.bannerSteps().createBannerInActiveTextAdGroup(
                new TextBannerInfo()
                        .withBanner(banner3)
                        .withAdGroupInfo(adGroupInfo2));

        OldTextBanner banner4 = activeTextBanner(null, null)
                .withPermalinkId(permalinkIdSecond);
        fillTextDefaultSystemFields(banner);
        TextBannerInfo secondActiveTextBannerOfSecondCampaign = steps.bannerSteps().createBannerInActiveTextAdGroup(
                new TextBannerInfo()
                        .withBanner(banner4)
                        .withCampaignInfo(firstActiveTextBannerOfSecondCampaign.getCampaignInfo()));

        OldTextBanner banner5 = activeTextBanner(null, null)
                .withPermalinkId(permalinkIdThird);
        fillTextDefaultSystemFields(banner);
        TextBannerInfo thirdActiveTextBannerOfSecondCampaign = steps.bannerSteps().createBannerInActiveTextAdGroup(
                new TextBannerInfo()
                        .withBanner(banner5)
                        .withCampaignInfo(firstActiveTextBannerOfSecondCampaign.getCampaignInfo()));

        var changesFirstCampaign = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(otherPermalinkId, PERMALINK_ID);
        var changesSecondCampaign = new ModelChanges<>(secondActiveTextBannerOfSecondCampaign.getBannerId(),
                TextBanner.class)
                .process(otherPermalinkId, PERMALINK_ID);

        List<Long> ids = prepareAndApplyValid(List.of(changesFirstCampaign, changesSecondCampaign));

        checkCampaignCountersHasCounter(clientInfo.getShard(), bannerInfo.getCampaignId(),
                List.of(OTHER_METRIKA_COUNTER_ID,
                        SECOND_METRIKA_COUNTER_ID, THIRD_METRIKA_COUNTER_ID,
                        CAMPAIGN_COUNTER_ID));
        checkSpravCounterSource(bannerInfo.getCampaignId(), SECOND_METRIKA_COUNTER_ID,
                MetrikaCountersSource.sprav);
        checkSpravCounterSource(bannerInfo.getCampaignId(), OTHER_METRIKA_COUNTER_ID,
                MetrikaCountersSource.sprav);
        checkSpravCounterSource(bannerInfo.getCampaignId(), THIRD_METRIKA_COUNTER_ID,
                MetrikaCountersSource.sprav);
        checkSpravCounterSource(bannerInfo.getCampaignId(), CAMPAIGN_COUNTER_ID, MetrikaCountersSource.unknown);

        checkCampaignCountersHasCounter(firstActiveTextBannerOfSecondCampaign.getShard(),
                firstActiveTextBannerOfSecondCampaign.getCampaignId(),
                List.of(FIRST_METRIKA_COUNTER_ID, OTHER_METRIKA_COUNTER_ID,
                        THIRD_METRIKA_COUNTER_ID));
        checkSpravCounterSource(firstActiveTextBannerOfSecondCampaign.getCampaignId(),
                FIRST_METRIKA_COUNTER_ID, MetrikaCountersSource.sprav);
        checkSpravCounterSource(firstActiveTextBannerOfSecondCampaign.getCampaignId(),
                OTHER_METRIKA_COUNTER_ID, MetrikaCountersSource.sprav);
        checkSpravCounterSource(firstActiveTextBannerOfSecondCampaign.getCampaignId(),
                THIRD_METRIKA_COUNTER_ID, MetrikaCountersSource.sprav);
    }

    @Test
    public void updateRelatedEntitiesInTransaction_checkUpdateCountersToStrategy() {
        textCampaignInfo = createTextCampaignWithStrategy();

        BannerWithOrganization banner = fullTextBanner(textCampaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withPermalinkId(permalinkIdFirst)
                .withPreferVCardOverPermalink(false);

        steps.textBannerSteps().createBanner(new NewTextBannerInfo()
                .withBanner(banner)
                .withCampaignInfo(textCampaignInfo)
                .withClientInfo(clientInfo)
                .withVcardInfo(new VcardInfo()
                        .withClientInfo(clientInfo)
                        .withCampaignInfo(textCampaignInfo)
                        .withVcard(fullVcard().withPermalink(permalinkIdFirst))));

        BannersUpdateOperationContainer bannersUpdateOperationContainer = getBannersUpdateOperationContainer(banner);

        DSLContext dslContext = dslContextProvider.ppc(bannersUpdateOperationContainer.getShard());

        var modelChanges = new ModelChanges<>(banner.getId(), BannerWithOrganization.class)
                .process(permalinkIdSecond, PERMALINK_ID)
                .applyTo(banner);

        bannerWithOrganizationUpdateOperationTypeSupport.updateRelatedEntitiesInTransaction(
                dslContext,
                bannersUpdateOperationContainer,
                List.of(modelChanges));

        checkCampaignStrategyCounters(textCampaignInfo, List.of(FIRST_METRIKA_COUNTER_ID));
    }

    private void checkSpravCounterSource(Long campaignId, Long counterId, MetrikaCountersSource source) {
        List<MetrikaCountersSource> sources = contextProvider.ppc(adGroupInfo.getShard())
                .select(METRIKA_COUNTERS.SOURCE)
                .from(METRIKA_COUNTERS)
                .where(METRIKA_COUNTERS.CID.eq(campaignId)
                        .and(METRIKA_COUNTERS.METRIKA_COUNTER.eq(counterId)))
                .fetch(METRIKA_COUNTERS.SOURCE);
        Assertions.assertThat(sources).hasSize(1);
        Assertions.assertThat(sources.get(0)).isEqualTo(source);
    }

    private void checkCampaignCountersHasCounter(int shard, Long campaignId, List<Long> expectedCounterIds) {
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(shard,
                        Set.of(campaignId)).get(0);
        List<Long> metrikaCounters = actualCampaign.getMetrikaCounters();

        Assertions.assertThat(metrikaCounters)
                .containsExactlyInAnyOrder(expectedCounterIds.toArray(new Long[]{}));
    }

    private void updateMetrikaCounters(AbstractBannerInfo<? extends OldTextBanner> activeTextBanner) {
        var campaignMetrikaCounter = new MetrikaCounter()
                .withId(CAMPAIGN_COUNTER_ID)
                .withHasEcommerce(true);
        campMetrikaCountersRepository.updateMetrikaCounters(clientInfo.getShard(),
                Map.of(activeTextBanner.getCampaignId(), List.of(campaignMetrikaCounter)));
    }

    private TextCampaignInfo createTextCampaignWithStrategy() {
        TextCampaign textCampaign = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo);
        return steps.textCampaignSteps().createCampaign(clientInfo, textCampaign);
    }

    private void checkCampaignStrategyCounters(TextCampaignInfo campaignInfo, List<Long> counters) {
        Assertions.assertThat(getStrategyCountersForCampaign(campaignInfo))
                .hasSameElementsAs(counters);
    }

    @NotNull
    private BannersUpdateOperationContainerImpl getBannersUpdateOperationContainer(BannerWithOrganization banner) {
        var bannerContainer = new BannersUpdateOperationContainerImpl(
                clientInfo.getShard(),
                clientInfo.getUid(),
                rbacService.getUidRole(clientInfo.getUid()),
                clientInfo.getClientId(),
                clientInfo.getUid(),
                rbacService.getChiefByClientId(clientInfo.getClientId()),
                Region.RUSSIA_REGION_ID,
                Set.of(
                        FeatureName.TOGETHER_UPDATING_STRATEGY_AND_CAMPAIGN_METRIKA_COUNTERS.getName(),
                        FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS.getName()
                ),
                ModerationMode.DEFAULT,
                false,
                false,
                false
        );

        int cid = (int)textCampaignInfo.getId();

        bannerContainer.setIndexToCampaignMap(Map.of(cid, textCampaignInfo.getTypedCampaign()));
        bannerContainer.setBannerToIndexMap(new IdentityHashMap<>(Map.of(banner, cid)));
        bannerContainer.setClientOrganizations(Map.of());

        return bannerContainer;
    }

    private List<Long> getStrategyCountersForCampaign(TextCampaignInfo campaignInfo) {
        return getCountersForStrategy(campaignInfo.getTypedCampaign().getStrategyId());
    }

    private List<Long> getCountersForStrategy(Long strategyId) {
        var strategies =
                StreamEx.of(strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(strategyId)))
                        .select(StrategyWithMetrikaCounters.class)
                        .toList();
        if (strategies.isEmpty()) return emptyList();
        return strategies.get(0).getMetrikaCounters();
    }
}
