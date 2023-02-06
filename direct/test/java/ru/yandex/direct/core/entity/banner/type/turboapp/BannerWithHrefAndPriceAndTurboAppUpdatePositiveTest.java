package ru.yandex.direct.core.entity.banner.type.turboapp;


import java.math.BigDecimal;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.BannerTurboAppType;
import ru.yandex.direct.core.entity.banner.model.BannerWithPrice;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.TurboAppInfo;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPrice;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerTurboApp;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerTurboAppsRepository;
import ru.yandex.direct.core.entity.banner.service.BannerTurboAppService;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.stub.TurboAppsClientStub;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.turboapps.client.model.TurboAppInfoResponse;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.direct.core.entity.banner.model.BannerTurboAppType.FEATURE;
import static ru.yandex.direct.core.entity.banner.model.BannerTurboAppType.OFFER;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestTurboApps.DEFAULT_CONTENT;
import static ru.yandex.direct.core.testing.data.TestTurboApps.defaultTurboAppResponse;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithHrefAndPriceAndTurboAppUpdatePositiveTest
        extends BannerOldBannerInfoUpdateOperationTestBase<OldTextBanner> {

    private static final String HREF_WITH_TURBO_APP = "https://url.with.turbo.app.ru";
    private static final String HREF_WITHOUT_TURBO_APP = "https://url.without.turbo.app.ru";

    @Autowired
    private OldBannerTurboAppsRepository bannerTurboAppsRepository;

    @Autowired
    private TurboAppsClientStub turboAppsClient;

    @Autowired
    private BannerTurboAppService bannerTurboAppService;

    @Autowired
    private TurboAppsInfoRepository turboAppsInfoRepository;

    private ClientInfo clientInfo;
    private CampaignInfo campaignInfo;
    private TurboAppInfoResponse turboAppInfoResponse;

    private BannerPrice defaultNewBannerPrice;
    private OldBannerPrice defaultOldBannerPrice;


    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);

        turboAppInfoResponse = defaultTurboAppResponse();
        turboAppsClient.addTurboAppInfo(HREF_WITH_TURBO_APP, turboAppInfoResponse);

        bannerTurboAppService.setHasTurboAppForCampaigns(clientInfo.getShard(), List.of(campaignInfo.getCampaignId()), true);

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.TURBO_APP_ALLOWED, true);

        defaultNewBannerPrice = new BannerPrice()
                .withPrice(new BigDecimal("4.00"))
                .withCurrency(BannerPricesCurrency.RUB);

        defaultOldBannerPrice = new OldBannerPrice()
                .withPrice(new BigDecimal("3.00"))
                .withCurrency(OldBannerPricesCurrency.RUB);
    }

    @Test
    public void updateBanner_ChangeHrefToHrefWithTurboApp_FeatureTurboAppAdded() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(HREF_WITH_TURBO_APP, TextBanner.HREF);

        prepareAndApplyValid(modelChanges);

        TextBanner actual = getBanner(bannerInfo.getBannerId());

        assertSoftly(softly -> {
            softly.assertThat(actual.getTurboAppInfoId()).isNotNull();
            softly.assertThat(actual.getTurboAppContent()).isEqualTo(DEFAULT_CONTENT);
            softly.assertThat(actual.getTurboAppType()).isEqualTo(BannerTurboAppType.FEATURE);
        });

        checkTurboApp(bannerInfo.getShard(), actual.getTurboAppInfoId());
    }

    @Test
    public void updateBanner_AddBannerPriceAndChangeHrefToHrefWithTurboApp_OfferTurboAppAdded() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo);

        BannerPrice price = new BannerPrice()
                .withPrice(new BigDecimal("4.00"))
                .withCurrency(BannerPricesCurrency.RUB);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(price, TextBanner.BANNER_PRICE)
                .process(HREF_WITH_TURBO_APP, TextBanner.HREF);

        prepareAndApplyValid(modelChanges);

        TextBanner actual = getBanner(bannerInfo.getBannerId());

        assertSoftly(softly -> {
            softly.assertThat(actual.getTurboAppInfoId()).isNotNull();
            softly.assertThat(actual.getTurboAppContent()).isEqualTo(DEFAULT_CONTENT);
            softly.assertThat(actual.getTurboAppType()).isEqualTo(BannerTurboAppType.OFFER);
        });

        checkTurboApp(bannerInfo.getShard(), actual.getTurboAppInfoId());
    }

    private void checkTurboApp(int shard, Long turboAppInfoId) {
        List<TurboAppInfo> infos = turboAppsInfoRepository.getTurboAppInfoByInfoIds(shard, singleton(turboAppInfoId));

        assertThat(infos).isNotEmpty();
        TurboAppInfo info = infos.get(0);

        assertSoftly(softly -> {
            softly.assertThat(info.getTurboAppId()).isEqualTo(turboAppInfoResponse.getAppId());
            softly.assertThat(info.getClientId()).isEqualTo(clientInfo.getClientId().asLong());
            softly.assertThat(info.getContent()).isEqualTo(turboAppInfoResponse.getMetaContent());
        });
    }

    @Test
    public void updateBanner_ChangeHrefToHrefWithoutTurboApp_TurboAppDeleted() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner(campaignInfo);
        steps.turboAppSteps().addBannerTurboApp(bannerInfo, turboAppInfoResponse);

        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(HREF_WITHOUT_TURBO_APP, TextBanner.HREF);

        prepareAndApplyValid(modelChanges);

        TextBanner actual = getBanner(bannerInfo.getBannerId());

        assertSoftly(softly -> {
            softly.assertThat(actual.getTurboAppInfoId()).isNull();
            softly.assertThat(actual.getTurboAppContent()).isNull();
            softly.assertThat(actual.getTurboAppType()).isNull();
        });
    }

    @Test
    public void updateBanner_AddBannerPrice_TurboAppChangedFromFeatureToOffer() {
        bannerInfo = createBanner();
        Long bannerId = bannerInfo.getBannerId();
        createTurboApp(FEATURE);

        ModelChanges<TextBanner> modelChanges = createModelChanges(bannerId, defaultNewBannerPrice);
        prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(bannerId, TextBanner.class);

        List<OldBannerTurboApp> actualTurboApps =
                bannerTurboAppsRepository.getTurboAppByBannerIds(bannerInfo.getShard(), singleton(bannerId));

        Assert.assertThat(actualBanner.getBannerPrice(), equalTo(defaultNewBannerPrice));
        assertThatTurboAppTypeIs(actualTurboApps, OFFER);
    }

    @Test
    public void updateBanner_RemoveBannerPrice_TurboAppChangedFromOfferToFeature() {
        bannerInfo = createBanner(defaultOldBannerPrice);
        Long bannerId = bannerInfo.getBannerId();
        createTurboApp(OFFER);

        ModelChanges<TextBanner> modelChanges = createModelChanges(bannerId, null);
        prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(bannerId, TextBanner.class);

        List<OldBannerTurboApp> actualTurboApps =
                bannerTurboAppsRepository.getTurboAppByBannerIds(bannerInfo.getShard(), singleton(bannerId));

        Assert.assertThat(actualBanner.getBannerPrice(), nullValue());
        assertThatTurboAppTypeIs(actualTurboApps, FEATURE);
    }

    private ModelChanges<TextBanner> createModelChanges(Long bannerId, BannerPrice bannerPrice) {
        return new ModelChanges<>(bannerId, TextBanner.class)
                .process(bannerPrice, BannerWithPrice.BANNER_PRICE);
    }

    private void assertThatTurboAppTypeIs(List<OldBannerTurboApp> actualTurboApps, BannerTurboAppType type) {
        Assert.assertThat(actualTurboApps, hasSize(1));
        Assert.assertThat(actualTurboApps.get(0).getBannerTurboAppType(), equalTo(type));
    }

    private TextBannerInfo createBanner() {
        return createBanner(null);
    }

    private TextBannerInfo createBanner(OldBannerPrice bannerPrice) {
        OldTextBanner banner = activeTextBanner(null, null)
                .withBannerPrice(bannerPrice);

        return steps.bannerSteps().createActiveTextBanner(banner);
    }


    private void createTurboApp(BannerTurboAppType bannerTurboAppType) {
        TurboAppInfo info = steps.turboAppSteps()
                .createDefaultTurboAppInfo(bannerInfo.getShard(), bannerInfo.getClientId().asLong());

        steps.turboAppSteps().addBannerTurboAppAndTurboAppInfo(bannerInfo.getShard(), bannerInfo.getBannerId(),
                info, DEFAULT_CONTENT, bannerTurboAppType);
    }
}
