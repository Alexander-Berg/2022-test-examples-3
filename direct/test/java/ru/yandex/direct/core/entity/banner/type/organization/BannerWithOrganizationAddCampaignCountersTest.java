package ru.yandex.direct.core.entity.banner.type.organization;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.container.BannersAddOperationContainerImpl;
import ru.yandex.direct.core.entity.banner.model.BannerWithOrganization;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.campaign.model.MetrikaCounter;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampMetrikaCountersRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.strategy.model.StrategyWithMetrikaCounters;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo;
import ru.yandex.direct.core.testing.stub.OrganizationsClientStub;
import ru.yandex.direct.dbschema.ppc.enums.MetrikaCountersSource;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.rbac.RbacService;
import ru.yandex.direct.regions.Region;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.core.testing.data.TestOrganizations.defaultActiveOrganization;
import static ru.yandex.direct.dbschema.ppc.Tables.METRIKA_COUNTERS;
import static ru.yandex.direct.feature.FeatureName.ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithOrganizationAddCampaignCountersTest extends BannerAdGroupInfoAddOperationTestBase {

    private long permalinkIdFirst = -1L;
    private static final long METRIKA_COUNTER_ID = 2L;
    private static final long CAMPAIGN_COUNTER_ID = 99999L;

    @Autowired
    private RbacService rbacService;

    @Autowired
    private OrganizationsClientStub organizationsClient;

    @Autowired
    private BannerWithOrganizationAddOperationTypeSupport bannerWithOrganizationAddOperationTypeSupport;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    @Autowired
    DslContextProvider contextProvider;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    CampMetrikaCountersRepository campMetrikaCountersRepository;

    private ClientInfo clientInfo;
    private TextCampaignInfo textCampaignInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        var clientId = clientInfo.getClientId();
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo);
        var organization1 = defaultActiveOrganization(clientId);
        var chiefUid = rbacService.getChiefByClientId(clientId);

        permalinkIdFirst = organization1.getPermalinkId();
        organizationsClient.addUidsAndCounterIdsByPermalinkId(permalinkIdFirst, List.of(chiefUid), METRIKA_COUNTER_ID);
        var campaignMetrikaCounter = new MetrikaCounter()
                .withId(CAMPAIGN_COUNTER_ID)
                .withHasEcommerce(true);

        campMetrikaCountersRepository.updateMetrikaCounters(adGroupInfo.getShard(),
                Map.of(adGroupInfo.getCampaignId(), List.of(campaignMetrikaCounter)));

        reset(organizationsClient);
    }

    @Test
    public void addNewPermalinkToNewBanner_FeatureDisabled_CounterIdNotAddedToCampaign() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(),
                ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, false);

        TextBanner banner = clientTextBanner()
                .withPermalinkId(permalinkIdFirst)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getPermalinkId(), equalTo(banner.getPermalinkId()));

        checkCampaignCountersHasCounter(adGroupInfo.getCampaignId(), List.of(CAMPAIGN_COUNTER_ID));
        verify(organizationsClient, times(0)).getOrganizationsCountersData(any(), any(), any());
        checkSpravCounterSource(adGroupInfo.getCampaignId(), CAMPAIGN_COUNTER_ID, MetrikaCountersSource.unknown);
    }

    @Test
    public void addNewPermalinkToNewBanner_CounterIdIsAddedToCampaign() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(),
                ADDING_ORGANIZATIONS_COUNTERS_TO_CAMPAIGN_ON_ADDING_ORGANIZATIONS_TO_ADS, true);

        TextBanner banner = clientTextBanner()
                .withPermalinkId(permalinkIdFirst)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        TextBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getPermalinkId(), equalTo(banner.getPermalinkId()));

        checkCampaignCountersHasCounter(adGroupInfo.getCampaignId(),
                List.of(METRIKA_COUNTER_ID, CAMPAIGN_COUNTER_ID));
        checkSpravCounterSource(adGroupInfo.getCampaignId(), METRIKA_COUNTER_ID,
                MetrikaCountersSource.sprav);
        checkSpravCounterSource(adGroupInfo.getCampaignId(), CAMPAIGN_COUNTER_ID,
                MetrikaCountersSource.unknown);
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

    private void checkCampaignCountersHasCounter(Long campaignId, List<Long> expectedCounterIds) {
        TextCampaign actualCampaign =
                (TextCampaign) campaignTypedRepository.getTypedCampaigns(clientInfo.getShard(),
                        Set.of(campaignId)).get(0);
        List<Long> metrikaCounters = actualCampaign.getMetrikaCounters();

        Assertions.assertThat(metrikaCounters)
                .containsExactlyInAnyOrder(expectedCounterIds.toArray(new Long[]{}));
    }

    @Test
    public void addOrganizationCountersToBannersCampaigns_checkAddCountersToStrategy() {
        textCampaignInfo = createTextCampaignWithStrategy();

        BannerWithOrganization banner = fullTextBanner(textCampaignInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withPermalinkId(permalinkIdFirst);

        bannerWithOrganizationAddOperationTypeSupport.addOrganizationCountersToBannersCampaigns(
                contextProvider.ppc(clientInfo.getShard()),
                getBannersAddOperationContainer(banner),
                List.of(banner)
        );

        checkCampaignCounters(textCampaignInfo, List.of(METRIKA_COUNTER_ID));
    }

    private TextCampaignInfo createTextCampaignWithStrategy() {
        TextCampaign textCampaign = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo);
        return steps.textCampaignSteps().createCampaign(clientInfo, textCampaign);
    }

    private void checkCampaignCounters(TextCampaignInfo campaignInfo, List<Long> counters) {
        Assertions.assertThat(getStrategyCountersForCampaign(campaignInfo))
                .hasSameElementsAs(counters);
    }

    @NotNull
    private BannersAddOperationContainerImpl getBannersAddOperationContainer(BannerWithOrganization banner) {
        var bannerContainer = new BannersAddOperationContainerImpl(
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
                ModerationMode.FORCE_MODERATE,
                false,
                false,
                false
        );

        int cid = (int)textCampaignInfo.getId();

        bannerContainer.setIndexToCampaignMap(Map.of(cid, textCampaignInfo.getTypedCampaign()));
        bannerContainer.setBannerToIndexMap(new IdentityHashMap<>(Map.of(banner, cid)));

        return bannerContainer;
    }

    private List<Long> getStrategyCountersForCampaign(TextCampaignInfo campaignInfo) {
        return getCountersForStrategy(campaignInfo.getTypedCampaign().getStrategyId());
    }

    private List<Long> getCountersForStrategy(Long sid) {
        var strategies =
                StreamEx.of(strategyTypedRepository.getTyped(clientInfo.getShard(), List.of(sid)))
                        .select(StrategyWithMetrikaCounters.class)
                        .toList();
        if (strategies.isEmpty()) return emptyList();
        return strategies.get(0).getMetrikaCounters();
    }

}
