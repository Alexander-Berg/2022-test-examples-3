package ru.yandex.direct.core.entity.banner.type.logos;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithLogo;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithLogoAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {
    private CreativeInfo creativeInfo;
    private String imageHash;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(),
                steps.creativeSteps().getNextCreativeId());
        imageHash = steps.bannerSteps().createLogoImageFormat(adGroupInfo.getClientInfo()).getImageHash();

    }

    @Test
    public void withLogo() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withLogoImageHash(imageHash);

        Long id = prepareAndApplyValid(banner);

        BannerWithLogo actualBanner = getBanner(id);
        assertThat(actualBanner.getLogoImageHash()).isEqualTo(imageHash);
        assertThat(actualBanner.getLogoStatusModerate()).isEqualTo(BannerLogoStatusModerate.READY);
    }

    @Test
    public void withoutLogo() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withLogoImageHash(null);

        Long id = prepareAndApplyValid(banner);

        BannerWithLogo actualBanner = getBanner(id);
        assertThat(actualBanner.getLogoImageHash()).isNull();
        assertThat(actualBanner.getLogoStatusModerate()).isNull();
    }
}
