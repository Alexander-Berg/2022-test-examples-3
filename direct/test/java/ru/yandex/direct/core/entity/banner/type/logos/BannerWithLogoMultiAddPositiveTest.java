package ru.yandex.direct.core.entity.banner.type.logos;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithLogoMultiAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {
    private CreativeInfo creativeInfo;
    private String imageHash1;
    private String imageHash2;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(),
                steps.creativeSteps().getNextCreativeId());
        imageHash1 = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo()).getImageHash();
        imageHash2 = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo()).getImageHash();
    }

    @Test
    public void oneBannerWithLogoAndOneWithout() {
        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withLogoImageHash(imageHash1);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId());

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getLogoImageHash()).isEqualTo(imageHash1);
        assertThat(actualBanner1.getLogoStatusModerate()).isEqualTo(BannerLogoStatusModerate.READY);
        assertThat(actualBanner2.getLogoImageHash()).isNull();
    }

    @Test
    public void severalBannersWithLogo() {
        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withLogoImageHash(imageHash1);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withLogoImageHash(imageHash2);

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getLogoImageHash()).isEqualTo(imageHash1);
        assertThat(actualBanner1.getLogoStatusModerate()).isEqualTo(BannerLogoStatusModerate.READY);
        assertThat(actualBanner2.getLogoImageHash()).isEqualTo(imageHash2);
        assertThat(actualBanner2.getLogoStatusModerate()).isEqualTo(BannerLogoStatusModerate.READY);
    }
}
