package ru.yandex.direct.core.entity.banner.type.turboapp;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.BannerTurboAppType;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.BannerTurboAppService;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.stub.TurboAppsClientStub;
import ru.yandex.direct.feature.FeatureName;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.data.TestTurboApps.DEFAULT_CONTENT;
import static ru.yandex.direct.core.testing.data.TestTurboApps.defaultTurboAppResponse;


@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithHrefAndPriceAndTurboAppAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String HREF = "https://ya.ru";

    @Autowired
    private TurboAppsClientStub turboAppsClient;

    @Autowired
    private BannerTurboAppService bannerTurboAppService;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        var campaignInfo = steps.campaignSteps().createActiveTextCampaign(clientInfo);
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo);

        turboAppsClient.addTurboAppInfo(HREF, defaultTurboAppResponse());

        bannerTurboAppService.setHasTurboAppForCampaigns(clientInfo.getShard(), List.of(campaignInfo.getCampaignId()), true);
    }

    @Test
    public void validFeatureTurboAppForTextBannerWithFeatureEnabled() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.TURBO_APP_ALLOWED, true);

        var banner = clientTextBanner()
                .withBannerPrice(null)
                .withHref(HREF)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        TextBanner actualBanner = getBanner(id);
        assertSoftly(softly -> {
            softly.assertThat(actualBanner.getTurboAppInfoId()).isNotNull();
            softly.assertThat(actualBanner.getTurboAppContent()).isEqualTo(DEFAULT_CONTENT);
            softly.assertThat(actualBanner.getTurboAppType()).isEqualTo(BannerTurboAppType.FEATURE);
        });
    }

    @Test
    public void validOfferTurboAppForTextBannerWithFeatureEnabled() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.TURBO_APP_ALLOWED, true);

        var banner = clientTextBanner()
                .withBannerPrice(new BannerPrice()
                        .withPrice(new BigDecimal("4.00"))
                        .withCurrency(BannerPricesCurrency.RUB))
                .withHref(HREF)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        TextBanner actualBanner = getBanner(id);
        assertSoftly(softly -> {
            softly.assertThat(actualBanner.getTurboAppInfoId()).isNotNull();
            softly.assertThat(actualBanner.getTurboAppContent()).isEqualTo(DEFAULT_CONTENT);
            softly.assertThat(actualBanner.getTurboAppType()).isEqualTo(BannerTurboAppType.OFFER);
        });
    }

    @Test
    public void validTurboAppForTextBannerWithFeatureDisabled() {
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.TURBO_APP_ALLOWED, false);

        var banner = clientTextBanner()
                .withBannerPrice(null)
                .withHref(HREF)
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        TextBanner actualBanner = getBanner(id);
        assertSoftly(softly -> {
            softly.assertThat(actualBanner.getTurboAppInfoId()).isNull();
            softly.assertThat(actualBanner.getTurboAppContent()).isNull();
            softly.assertThat(actualBanner.getTurboAppType()).isNull();
        });
    }
}
