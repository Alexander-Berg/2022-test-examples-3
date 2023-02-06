package ru.yandex.direct.intapi.entity.metrika.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.turbolanding.model.OldBannerTurboLanding;
import ru.yandex.direct.core.entity.calltrackingsettings.repository.CalltrackingSettingsRepository;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campcalltrackingsettings.repository.CampCalltrackingSettingsRepository;
import ru.yandex.direct.core.testing.data.TestBanners;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.DomainInfo;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.dbschema.ppc.enums.MetrikaCountersSource;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.metrika.model.MetrikaCampaignsResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaCampaignsServiceTest {

    private static final String DOMAIN_POSTFIX = ".com";
    private static final String PHONE_1 = "+78005553535";
    private static final Long CALLTRACKING_COUNTER_ID = 123456L;

    @Autowired
    private MetrikaCampaignsService metrikaCampaignsService;

    @Autowired
    private Steps steps;

    @Autowired
    TestCampaignRepository testCampaignRepository;

    @Autowired
    CampCalltrackingSettingsRepository campCalltrackingSettingsRepository;

    @Autowired
    CalltrackingSettingsRepository calltrackingSettingsRepository;

    @Test
    public void getCampaigns() {
        Campaign campaign = TestCampaigns.newTextCampaign(null, null);
        campaign.setMetrikaCounters(asList(1L, 2L));

        CampaignInfo campaignInfo = new CampaignInfo().withCampaign(campaign);

        campaignInfo = steps.campaignSteps().createCampaign(campaignInfo);

        steps.bsFakeSteps().setOrderId(campaignInfo);

        List<MetrikaCampaignsResult> results = metrikaCampaignsService
                .getCampaigns(Collections.singletonList(campaignInfo.getCampaign().getOrderId()));

        assumeThat("В списке результатов 1 элемент", results, hasSize(1));

        List<Long> expectedCounters = campaignInfo.getCampaign().getMetrikaCounters();
        MetrikaCampaignsResult expected = new MetrikaCampaignsResult()
                .withCid(campaignInfo.getCampaignId())
                .withClientId(campaignInfo.getClientId().asLong())
                .withName(campaignInfo.getCampaign().getName())
                .withCurrency(campaignInfo.getCampaign().getBalanceInfo().getCurrency().toString())
                .withMetrikaCounters(expectedCounters)
                .withOrderId(campaignInfo.getCampaign().getOrderId());

        assertThat("В ответе верные данные", results.get(0), beanDiffer(expected));
    }

    @Test
    public void getCampaigns_CampHasOnlySpravCounterAndYclidTrackDisabled_ReturnsSpravCounter() {
        UserInfo defaultUser = steps.userSteps().createDefaultUser();
        TextCampaign textCampaign = defaultTextCampaignWithSystemFields(defaultUser.getClientInfo());
        long counterId = RandomNumberUtils.nextPositiveInteger();
        textCampaign
                .withMetrikaCounters(List.of(counterId))
                .withHasAddMetrikaTagToUrl(false);

        var campaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), textCampaign);

        Long orderId = steps.bsFakeSteps().setOrderId(campaignInfo.getShard(), campaignInfo.getClientId(),
                campaignInfo.getId());

        testCampaignRepository.setCounterSource(campaignInfo.getShard(), campaignInfo.getId(), counterId,
                MetrikaCountersSource.sprav);

        List<MetrikaCampaignsResult> results = metrikaCampaignsService.getCampaigns(Collections.singletonList(orderId));

        MetrikaCampaignsResult expected = new MetrikaCampaignsResult()
                .withCid(campaignInfo.getId())
                .withClientId(campaignInfo.getClientId().asLong())
                .withName(campaignInfo.getTypedCampaign().getName())
                .withCurrency(campaignInfo.getTypedCampaign().getCurrency().toString())
                .withMetrikaCounters(List.of(counterId))
                .withOrderId(orderId);

        assertThat("В ответе верные данные", results, beanDiffer(List.of(expected)));
    }

    @Test
    public void getCampaigns_CampHasOnlySpravCounterAndYclidTrackEnabled_ReturnsEmptyCounters() {
        UserInfo defaultUser = steps.userSteps().createDefaultUser();
        TextCampaign textCampaign = defaultTextCampaignWithSystemFields(defaultUser.getClientInfo());
        long counterId = RandomNumberUtils.nextPositiveInteger();
        textCampaign
                .withMetrikaCounters(List.of(counterId))
                .withHasAddMetrikaTagToUrl(true);

        var campaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), textCampaign);

        Long orderId = steps.bsFakeSteps().setOrderId(campaignInfo.getShard(), campaignInfo.getClientId(),
                campaignInfo.getId());

        testCampaignRepository.setCounterSource(campaignInfo.getShard(), campaignInfo.getId(), counterId,
                MetrikaCountersSource.sprav);

        List<MetrikaCampaignsResult> results = metrikaCampaignsService.getCampaigns(Collections.singletonList(orderId));

        MetrikaCampaignsResult expected = new MetrikaCampaignsResult()
                .withCid(campaignInfo.getId())
                .withClientId(campaignInfo.getClientId().asLong())
                .withName(campaignInfo.getTypedCampaign().getName())
                .withCurrency(campaignInfo.getTypedCampaign().getCurrency().toString())
                .withMetrikaCounters(emptyList())
                .withOrderId(orderId);

        assertThat("В ответе верные данные", results, beanDiffer(List.of(expected)));
    }

    @Test
    public void getCampaigns_CampHasSpravCounterAndUsersCounterAndYclidTrackEnabled_ReturnsBothCounter() {
        UserInfo defaultUser = steps.userSteps().createDefaultUser();
        TextCampaign textCampaign = defaultTextCampaignWithSystemFields(defaultUser.getClientInfo());
        long spravCounterId = RandomNumberUtils.nextPositiveInteger();
        long usersCounterId = spravCounterId + 1;
        textCampaign
                .withMetrikaCounters(List.of(spravCounterId, usersCounterId))
                .withHasAddMetrikaTagToUrl(true);

        var campaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), textCampaign);

        Long orderId = steps.bsFakeSteps().setOrderId(campaignInfo.getShard(), campaignInfo.getClientId(),
                campaignInfo.getId());

        testCampaignRepository.setCounterSource(campaignInfo.getShard(), campaignInfo.getId(), spravCounterId,
                MetrikaCountersSource.sprav);
        testCampaignRepository.setCounterSource(campaignInfo.getShard(), campaignInfo.getId(), usersCounterId,
                MetrikaCountersSource.unknown);

        List<MetrikaCampaignsResult> results = metrikaCampaignsService.getCampaigns(Collections.singletonList(orderId));

        MetrikaCampaignsResult expected = new MetrikaCampaignsResult()
                .withCid(campaignInfo.getId())
                .withClientId(campaignInfo.getClientId().asLong())
                .withName(campaignInfo.getTypedCampaign().getName())
                .withCurrency(campaignInfo.getTypedCampaign().getCurrency().toString())
                .withMetrikaCounters(List.of(spravCounterId, usersCounterId))
                .withOrderId(orderId);

        assertThat("В ответе верные данные", results, beanDiffer(List.of(expected)));
    }

    @Test
    public void getCampaigns_CampHasSpravCounterAndTurboCounterAndYclidTrackEnabled_ReturnsBothCounter() {
        UserInfo defaultUser = steps.userSteps().createDefaultUser();
        TextCampaign textCampaign = defaultTextCampaignWithSystemFields(defaultUser.getClientInfo());
        long spravCounterId = RandomNumberUtils.nextPositiveInteger();
        long turboCounterId = spravCounterId + 1;
        textCampaign
                .withMetrikaCounters(List.of(spravCounterId))
                .withHasAddMetrikaTagToUrl(true);

        var campaignInfo = steps.textCampaignSteps()
                .createCampaign(defaultUser.getClientInfo(), textCampaign);

        Long orderId = steps.bsFakeSteps().setOrderId(campaignInfo.getShard(), campaignInfo.getClientId(),
                campaignInfo.getId());

        testCampaignRepository.setCounterSource(campaignInfo.getShard(), campaignInfo.getId(), spravCounterId,
                MetrikaCountersSource.sprav);

        addTuboCounter(campaignInfo.getClientId(), campaignInfo.getId(), 3334L, campaignInfo.getShard(),
                List.of(turboCounterId));

        List<MetrikaCampaignsResult> results = metrikaCampaignsService.getCampaigns(Collections.singletonList(orderId));

        MetrikaCampaignsResult expected = new MetrikaCampaignsResult()
                .withCid(campaignInfo.getId())
                .withClientId(campaignInfo.getClientId().asLong())
                .withName(campaignInfo.getTypedCampaign().getName())
                .withCurrency(campaignInfo.getTypedCampaign().getCurrency().toString())
                .withMetrikaCounters(List.of(spravCounterId, turboCounterId))
                .withOrderId(orderId);

        assertThat("В ответе верные данные", results, beanDiffer(List.of(expected)));
    }

    @Test(expected = IntApiException.class)
    public void throwsExceptionWhenValidationFails() {
        metrikaCampaignsService.getCampaigns(null);
    }

    @Test
    public void getCampaigns_addTurboCounters_whenYclidTrackDisabledAndNoUserCountersOnCampaign() {
        // В кампанию без собственных счётчиков не добавляем счётчики турболендингов
        Campaign campaignWithoutCounters = TestCampaigns.newTextCampaign(null, null);

        CampaignInfo campaignInfo = new CampaignInfo().withCampaign(campaignWithoutCounters);

        campaignInfo = steps.campaignSteps().createCampaign(campaignInfo);

        steps.bsFakeSteps().setOrderId(campaignInfo);
        List<Long> counterIds = asList(22L, 33L);
        long bannerId = 3333L;
        addTuboCounter(campaignInfo.getClientId(), campaignWithoutCounters.getId(), bannerId, campaignInfo.getShard(),
                counterIds);

        List<MetrikaCampaignsResult> results = metrikaCampaignsService
                .getCampaigns(Collections.singletonList(campaignInfo.getCampaign().getOrderId()));

        assumeThat("В списке результатов 1 элемент", results, hasSize(1));

        MetrikaCampaignsResult expected = new MetrikaCampaignsResult()
                .withCid(campaignInfo.getCampaignId())
                .withClientId(campaignInfo.getClientId().asLong())
                .withName(campaignInfo.getCampaign().getName())
                .withCurrency(campaignInfo.getCampaign().getBalanceInfo().getCurrency().toString())
                .withMetrikaCounters(counterIds)
                .withOrderId(campaignInfo.getCampaign().getOrderId());

        assertThat("В ответе верные данные", results.get(0), beanDiffer(expected));
    }

    @Test
    public void getCampaigns_addTurboCounters_whenYclidTrackEnabledAndNoUserCountersOnCampaign() {
        // В кампанию без собственных счётчиков не добавляем счётчики турболендингов
        Campaign campaignWithoutCounters = TestCampaigns.newTextCampaign(null, null)
                .withHasAddMetrikaTagToUrl(true);
        CampaignInfo campaignInfo = new CampaignInfo().withCampaign(campaignWithoutCounters);

        campaignInfo = steps.campaignSteps().createCampaign(campaignInfo);

        steps.bsFakeSteps().setOrderId(campaignInfo);
        OldBannerTurboLanding defaultBannerTurboLanding =
                steps.turboLandingSteps().createDefaultBannerTurboLanding(campaignInfo.getClientId());

        long bannerId = 3335L;

        List<Long> turboCounters = asList(22L, 33L);

        addTuboCounter(campaignInfo.getClientId(), campaignWithoutCounters.getId(), bannerId, campaignInfo.getShard(),
                turboCounters);

        List<MetrikaCampaignsResult> results = metrikaCampaignsService
                .getCampaigns(Collections.singletonList(campaignInfo.getCampaign().getOrderId()));

        assumeThat("В списке результатов 1 элемент", results, hasSize(1));

        // хотфикс: всегда добавляем турбо счётчики
        MetrikaCampaignsResult expected = new MetrikaCampaignsResult()
                .withCid(campaignInfo.getCampaignId())
                .withClientId(campaignInfo.getClientId().asLong())
                .withName(campaignInfo.getCampaign().getName())
                .withCurrency(campaignInfo.getCampaign().getBalanceInfo().getCurrency().toString())
                .withMetrikaCounters(turboCounters)
                .withOrderId(campaignInfo.getCampaign().getOrderId());

        assertThat("В ответе верные данные", results.get(0), beanDiffer(expected));
    }

    @Test
    public void getCampaigns_CampHasCalltrackingCounter() {
        Campaign campaign = TestCampaigns.newTextCampaign(null, null);
        campaign.setMetrikaCounters(asList(1L, 2L));

        CampaignInfo campaignInfo = new CampaignInfo().withCampaign(campaign);

        campaignInfo = steps.campaignSteps().createCampaign(campaignInfo);

        steps.bsFakeSteps().setOrderId(campaignInfo);

        DomainInfo domainInfo = steps.domainSteps().createDomain(campaignInfo.getShard(), DOMAIN_POSTFIX);

        Long calltrackingSettingsId = steps.calltrackingSettingsSteps().add(campaignInfo.getClientId(),
                domainInfo.getDomainId(),
                CALLTRACKING_COUNTER_ID,
                List.of(PHONE_1));
        steps.campCalltrackingSettingsSteps()
                .link(campaignInfo.getShard(), campaignInfo.getCampaignId(), calltrackingSettingsId);

        List<MetrikaCampaignsResult> results = metrikaCampaignsService
                .getCampaigns(Collections.singletonList(campaignInfo.getCampaign().getOrderId()));

        assumeThat("В списке результатов 1 элемент", results, hasSize(1));
        List<Long> expectedCounters = new ArrayList<>(campaignInfo.getCampaign().getMetrikaCounters());
        expectedCounters.add(CALLTRACKING_COUNTER_ID);
        MetrikaCampaignsResult expected = new MetrikaCampaignsResult()
                .withCid(campaignInfo.getCampaignId())
                .withClientId(campaignInfo.getClientId().asLong())
                .withName(campaignInfo.getCampaign().getName())
                .withCurrency(campaignInfo.getCampaign().getBalanceInfo().getCurrency().toString())
                .withMetrikaCounters(expectedCounters)
                .withOrderId(campaignInfo.getCampaign().getOrderId());

        assertThat("В ответе верные данные", results.get(0), beanDiffer(expected));
    }

    private void addTuboCounter(ClientId clientId, Long campaignId, Long bannerId, Integer shard,
                                List<Long> counterIds) {
        OldBannerTurboLanding defaultBannerTurboLanding =
                steps.turboLandingSteps().createDefaultBannerTurboLanding(clientId);

        // copy-paste из MetrikaCountersServiceTest
        OldTextBanner textBanner = TestBanners.defaultTextBanner(campaignId, 1111L);
        textBanner.setId(bannerId);
        textBanner.setTurboLandingStatusModerate(defaultBannerTurboLanding.getStatusModerate());
        textBanner.setTurboLandingId(defaultBannerTurboLanding.getId());
        steps.bannerSteps().createBanner(textBanner);
        steps.turboLandingSteps()
                .addBannerToBannerTurbolandingsTableOrUpdate(campaignId, List.of(textBanner));
        steps.bannerSteps().addTurbolandingMetricaCounters(shard, textBanner, counterIds);
    }

}
