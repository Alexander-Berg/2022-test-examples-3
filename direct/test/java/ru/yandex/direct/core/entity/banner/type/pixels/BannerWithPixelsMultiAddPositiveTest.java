package ru.yandex.direct.core.entity.banner.type.pixels;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.pixels.Provider;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adriverPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.tnsPixelUrl;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithPixelsMultiAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {
    private CreativeInfo creativeInfo;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(),
                steps.creativeSteps().getNextCreativeId());
        steps.clientPixelProviderSteps().addClientPixelProviderPermissionCpmBanner(adGroupInfo.getClientInfo(),
                Provider.TNS);
        steps.clientPixelProviderSteps().addClientPixelProviderPermissionCpmBanner(adGroupInfo.getClientInfo(),
                Provider.ADRIVER);
    }

    @Test
    public void oneBannerWithPixelsAndOneWithout() {
        List<String> pixels1 = asList(tnsPixelUrl(), adriverPixelUrl());

        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withPixels(pixels1);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withPixels(emptyList());

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getPixels()).containsOnlyElementsOf(pixels1);
        assertThat(actualBanner2.getPixels()).isEmpty();
    }

    @Test
    public void severalBannersWithPixels() {
        List<String> pixels1 = asList(tnsPixelUrl(), adriverPixelUrl());
        List<String> pixels2 = asList(tnsPixelUrl(), adfoxPixelUrl());

        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withPixels(pixels1);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withPixels(pixels2);

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getPixels()).containsOnlyElementsOf(pixels1);
        assertThat(actualBanner2.getPixels()).containsOnlyElementsOf(pixels2);
    }
}
