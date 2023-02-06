package ru.yandex.direct.core.entity.banner.type.button;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerButtonStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithButton;
import ru.yandex.direct.core.entity.banner.model.ButtonAction;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.i18n.I18NBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithButtonAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {
    private static final String HREF = "https://ya.ru";
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
    public void withButton() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withButtonAction(ButtonAction.DOWNLOAD)
                .withButtonHref(HREF);

        Long id = prepareAndApplyValid(banner);

        BannerWithButton actualBanner = getBanner(id);
        assertThat(actualBanner.getButtonAction()).isEqualTo(ButtonAction.DOWNLOAD);
        assertThat(actualBanner.getButtonCaption()).isEqualTo("Скачать");
        assertThat(actualBanner.getButtonHref()).isEqualTo(HREF);
        assertThat(actualBanner.getButtonStatusModerate()).isEqualTo(BannerButtonStatusModerate.READY);
    }

    @Test
    public void withButton_CustomText() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withButtonAction(ButtonAction.CUSTOM_TEXT)
                .withButtonCaption(CAPTION_CUSTOM_TEXT)
                .withButtonHref(HREF);

        Long id = prepareAndApplyValid(banner);

        BannerWithButton actualBanner = getBanner(id);
        assertThat(actualBanner.getButtonAction()).isEqualTo(ButtonAction.CUSTOM_TEXT);
        assertThat(actualBanner.getButtonCaption()).isEqualTo(CAPTION_CUSTOM_TEXT);
        assertThat(actualBanner.getButtonHref()).isEqualTo(HREF);
        assertThat(actualBanner.getButtonStatusModerate()).isEqualTo(BannerButtonStatusModerate.READY);
    }

    @Test
    public void withoutButton() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        Long id = prepareAndApplyValid(banner);

        BannerWithButton actualBanner = getBanner(id);
        assertThat(actualBanner.getButtonAction()).isNull();
        assertThat(actualBanner.getButtonStatusModerate()).isNull();
    }
}
