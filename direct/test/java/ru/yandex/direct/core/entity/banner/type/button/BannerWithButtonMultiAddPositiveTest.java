package ru.yandex.direct.core.entity.banner.type.button;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerButtonStatusModerate;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.i18n.I18NBundle;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithButtonMultiAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {
    private static final String HREF = "https://ya.ru";
    private static final String HREF_1 = "https://yandex.ru";
    private static final String CAPTION_CUSTOM_TEXT = "Купить зайчиков";

    private CreativeInfo creativeInfo;

    @Before
    public void before() {
        LocaleContextHolder.setLocale(I18NBundle.RU);
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(),
                steps.creativeSteps().getNextCreativeId());
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.ASSET_BUTTON_CUSTOM_TEXT, true);
    }

    @Test
    public void oneBannerWithButtonAndOneWithout() {
        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withButtonAction(ButtonAction.DOWNLOAD)
                .withButtonHref(HREF);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getButtonAction()).isEqualTo(ButtonAction.DOWNLOAD);
        assertThat(actualBanner1.getButtonCaption()).isEqualTo("Скачать");
        assertThat(actualBanner1.getButtonHref()).isEqualTo(HREF);
        assertThat(actualBanner1.getButtonStatusModerate()).isEqualTo(BannerButtonStatusModerate.READY);
        assertThat(actualBanner2.getButtonAction()).isNull();
    }

    @Test
    public void severalBannersWithButton() {
        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withButtonAction(ButtonAction.DOWNLOAD)
                .withButtonHref(HREF);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withButtonAction(ButtonAction.CUSTOM_TEXT)
                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                .withButtonHref(HREF_1);

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getButtonAction()).isEqualTo(ButtonAction.DOWNLOAD);
        assertThat(actualBanner1.getButtonCaption()).isEqualTo("Скачать");
        assertThat(actualBanner1.getButtonHref()).isEqualTo(HREF);
        assertThat(actualBanner1.getButtonStatusModerate()).isEqualTo(BannerButtonStatusModerate.READY);

        assertThat(actualBanner2.getButtonAction()).isEqualTo(ButtonAction.CUSTOM_TEXT);
        assertThat(actualBanner2.getButtonCaption()).isEqualTo(CAPTION_CUSTOM_TEXT);
        assertThat(actualBanner2.getButtonHref()).isEqualTo(HREF_1);
        assertThat(actualBanner2.getButtonStatusModerate()).isEqualTo(BannerButtonStatusModerate.READY);
    }
}
