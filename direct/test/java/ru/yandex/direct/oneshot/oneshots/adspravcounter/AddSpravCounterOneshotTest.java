package ru.yandex.direct.oneshot.oneshots.adspravcounter;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.BannerRelationsRepository;
import ru.yandex.direct.core.entity.banner.repository.old.type.OldBannerWithOrganizationSupport;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampMetrikaCountersRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.organizations.repository.OrganizationRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbschema.ppc.enums.MetrikaCountersSource;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.oneshot.configuration.OneshotTest;
import ru.yandex.direct.oneshot.oneshots.addspravcounter.AddSpravCounterOneshot;
import ru.yandex.direct.oneshot.oneshots.addspravcounter.AddSpravState;
import ru.yandex.direct.oneshot.oneshots.addspravcounter.service.SpravService;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.converter.CampaignConverter.SPRAV;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;

@OneshotTest
@RunWith(SpringRunner.class)
public class AddSpravCounterOneshotTest {
    public static final long COMMON_USER_COUNTER = 1L;
    public static final long SPRAV_COUNTER = 2L;
    public static final long SPRAV_COUNTER_SECOND = 3L;
    public static final long PERMALINK_ID = 123;
    public static final long PERMALINK_ID_SECOND = 1234;
    public static final long PERMALINK_ID_WITHOUT_COUNTER = 999;

    private AddSpravCounterOneshot oneshot;

    private SpravService spravService;

    @Autowired
    OrganizationRepository organizationRepository;

    @Autowired
    CampMetrikaCountersRepository campMetrikaCountersRepository;

    @Autowired
    BannerRelationsRepository bannerRelationsRepository;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    OldBannerWithOrganizationSupport bannerWithOrganizationSupport;

    @Autowired
    TestCampaignRepository testCampaignRepository;

    @Autowired
    DslContextProvider contextProvider;

    @Autowired
    MetrikaClientStub metrikaClientStub;

    @Autowired
    Steps steps;

    private UserInfo defaultUser;

    @Before
    public void before() {
        spravService = mock(SpravService.class);

        oneshot = new AddSpravCounterOneshot(spravService, organizationRepository, campMetrikaCountersRepository,
                bannerRelationsRepository);
        defaultUser = steps.userSteps().createDefaultUser();

        when(spravService.getCounterIdByPermalinkId(any())).thenReturn(Map.of(PERMALINK_ID, SPRAV_COUNTER,
                PERMALINK_ID_SECOND, SPRAV_COUNTER_SECOND));

        metrikaClientStub.addUserCounter(defaultUser.getUid(), Map.of((int) SPRAV_COUNTER, SPRAV,
                (int) SPRAV_COUNTER_SECOND, SPRAV,
                (int) COMMON_USER_COUNTER, ""));
    }

    @Test
    public void execute_CampaignHasOneCounterOneOrganization_AddSpravCounter() {
        TextCampaign campaign = defaultTextCampaignWithSystemFields(defaultUser.getClientInfo());
        campaign.setMetrikaCounters(List.of(COMMON_USER_COUNTER));

        var typedCampaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), campaign);

        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(typedCampaignInfo.toCampaignInfo());
        TextBannerInfo textBannerInfo = steps.bannerSteps().createActiveTextBanner(adGroup);

        addPermalinkToBanner(defaultUser.getShard(), textBannerInfo.getBanner(), PERMALINK_ID);

        oneshot.execute(null, new AddSpravState(0,
                textBannerInfo.getBannerId() - 1), defaultUser.getShard());

        Map<Long, MetrikaCountersSource> sources =
                testCampaignRepository.getMetrikaCountersSources(textBannerInfo.getShard(),
                        campaign.getId(), List.of(COMMON_USER_COUNTER, SPRAV_COUNTER));

        assertThat(sources).isEqualTo(Map.of(COMMON_USER_COUNTER, MetrikaCountersSource.unknown,
                SPRAV_COUNTER, MetrikaCountersSource.sprav));
    }

    @Test
    public void execute_CampaignHasOneCounterOneOrganizationButArchivedBanner_DontAddSpravCounter() {
        TextCampaign campaign = defaultTextCampaignWithSystemFields(defaultUser.getClientInfo());
        campaign.setMetrikaCounters(List.of(COMMON_USER_COUNTER));

        var typedCampaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), campaign);

        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(typedCampaignInfo.toCampaignInfo());
        TextBannerInfo textBannerInfo = steps.bannerSteps().createBanner(
                activeTextBanner(adGroup.getCampaignId(), adGroup.getAdGroupId())
                        .withStatusArchived(true),
                adGroup);

        addPermalinkToBanner(defaultUser.getShard(), textBannerInfo.getBanner(), PERMALINK_ID);

        oneshot.execute(null, new AddSpravState(0,
                textBannerInfo.getBannerId() - 1), defaultUser.getShard());

        Map<Long, MetrikaCountersSource> sources =
                testCampaignRepository.getMetrikaCountersSources(textBannerInfo.getShard(),
                        campaign.getId(), List.of(COMMON_USER_COUNTER, SPRAV_COUNTER));

        assertThat(sources).isEqualTo(Map.of(COMMON_USER_COUNTER, MetrikaCountersSource.unknown));
    }


    @Test
    public void executeTwoStages_CampaignHasOneCounterOneOrganization_AddSpravCounter() {
        TextCampaign campaign = defaultTextCampaignWithSystemFields(defaultUser.getClientInfo());
        campaign.setMetrikaCounters(List.of(COMMON_USER_COUNTER));

        var typedCampaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), campaign);

        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(typedCampaignInfo.toCampaignInfo());
        TextBannerInfo textBannerInfo = steps.bannerSteps().createActiveTextBanner(adGroup);

        addPermalinkToBanner(defaultUser.getShard(), textBannerInfo.getBanner(), PERMALINK_ID);

        AddSpravState executeResult = oneshot.execute(null,
                new AddSpravState(0, textBannerInfo.getBannerId() - 1),
                defaultUser.getShard());

        TextBannerInfo textBannerInfoSecond = steps.bannerSteps().createActiveTextBanner(adGroup);
        addPermalinkToBanner(defaultUser.getShard(), textBannerInfoSecond.getBanner(), PERMALINK_ID_SECOND);

        oneshot.execute(null, executeResult, defaultUser.getShard());

        Map<Long, MetrikaCountersSource> sources =
                testCampaignRepository.getMetrikaCountersSources(textBannerInfo.getShard(),
                        campaign.getId(), List.of(COMMON_USER_COUNTER, SPRAV_COUNTER, SPRAV_COUNTER_SECOND));

        assertThat(sources).isEqualTo(Map.of(COMMON_USER_COUNTER, MetrikaCountersSource.unknown,
                SPRAV_COUNTER, MetrikaCountersSource.sprav, SPRAV_COUNTER_SECOND, MetrikaCountersSource.sprav));
    }

    @Test
    public void execute_CampaignHasTwoCountersOneOrganization_AddSpravCounter() {
        TextCampaign campaign = defaultTextCampaignWithSystemFields(defaultUser.getClientInfo());
        campaign.setMetrikaCounters(List.of(COMMON_USER_COUNTER, SPRAV_COUNTER));

        var typedCampaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), campaign);

        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(typedCampaignInfo.toCampaignInfo());
        TextBannerInfo textBannerInfo = steps.bannerSteps().createActiveTextBanner(adGroup);

        addPermalinkToBanner(defaultUser.getShard(), textBannerInfo.getBanner(), PERMALINK_ID);

        oneshot.execute(null, new AddSpravState(0,
                textBannerInfo.getBannerId() - 1), defaultUser.getShard());

        Map<Long, MetrikaCountersSource> sources =
                testCampaignRepository.getMetrikaCountersSources(textBannerInfo.getShard(),
                        campaign.getId(), List.of(COMMON_USER_COUNTER, SPRAV_COUNTER));

        assertThat(sources).isEqualTo(Map.of(COMMON_USER_COUNTER, MetrikaCountersSource.unknown,
                SPRAV_COUNTER, MetrikaCountersSource.sprav));
    }

    @Test
    public void execute_CampaignHasBannerWithPermalinkWithoutCounter() {
        TextCampaign campaign = defaultTextCampaignWithSystemFields(defaultUser.getClientInfo());
        campaign.setMetrikaCounters(List.of(COMMON_USER_COUNTER));

        var typedCampaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), campaign);

        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(typedCampaignInfo.toCampaignInfo());
        TextBannerInfo textBannerInfo = steps.bannerSteps().createActiveTextBanner(adGroup);
        addPermalinkToBanner(defaultUser.getShard(), textBannerInfo.getBanner(), PERMALINK_ID_WITHOUT_COUNTER);
        oneshot.execute(null, new AddSpravState(0,
                textBannerInfo.getBannerId() - 1), defaultUser.getShard());

        Map<Long, MetrikaCountersSource> sources =
                testCampaignRepository.getMetrikaCountersSources(textBannerInfo.getShard(),
                        campaign.getId(), List.of(COMMON_USER_COUNTER, SPRAV_COUNTER));

        assertThat(sources).isEqualTo(Map.of(COMMON_USER_COUNTER, MetrikaCountersSource.unknown));
    }

    @Test
    public void execute_CampaignHasCounterOfOrganizationWithoutPermalinkOnBanner_CounterSourceNotChanged() {
        TextCampaign campaign = defaultTextCampaignWithSystemFields(defaultUser.getClientInfo());
        campaign.setMetrikaCounters(List.of(COMMON_USER_COUNTER, SPRAV_COUNTER));

        var typedCampaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), campaign);

        AdGroupInfo adGroup = steps.adGroupSteps().createDefaultAdGroup(typedCampaignInfo.toCampaignInfo());
        TextBannerInfo textBannerInfo = steps.bannerSteps().createActiveTextBanner(adGroup);
        addPermalinkToBanner(defaultUser.getShard(), textBannerInfo.getBanner(), PERMALINK_ID_WITHOUT_COUNTER);
        oneshot.execute(null, new AddSpravState(0,
                textBannerInfo.getBannerId() - 1), defaultUser.getShard());

        Map<Long, MetrikaCountersSource> sources =
                testCampaignRepository.getMetrikaCountersSources(textBannerInfo.getShard(),
                        campaign.getId(), List.of(COMMON_USER_COUNTER, SPRAV_COUNTER));

        assertThat(sources).isEqualTo(Map.of(COMMON_USER_COUNTER, MetrikaCountersSource.unknown,
                SPRAV_COUNTER, MetrikaCountersSource.sprav));
    }

    private void addPermalinkToBanner(int shard, OldTextBanner textBanner, Long permalinkId) {
        AppliedChanges<OldTextBanner> changes = getPermalinkChange(textBanner, permalinkId);

        bannerWithOrganizationSupport.updateBannerPermalinks(contextProvider.ppc(shard),
                List.of(changes));
    }

    private AppliedChanges<OldTextBanner> getPermalinkChange(OldTextBanner textBanner, Long permalinkId) {
        return new ModelChanges<>(textBanner.getId(), OldTextBanner.class)
                .process(permalinkId, OldTextBanner.PERMALINK_ID)
                .applyTo(textBanner);
    }
}
