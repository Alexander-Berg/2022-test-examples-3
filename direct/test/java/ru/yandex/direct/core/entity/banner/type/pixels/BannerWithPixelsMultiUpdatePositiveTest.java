package ru.yandex.direct.core.entity.banner.type.pixels;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adriverPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.tnsPixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithPixelsMultiUpdatePositiveTest extends BannerClientInfoUpdateOperationTestBase {

    private BannerInfo bannerInfo1;
    private BannerInfo bannerInfo2;

    @Before
    public void before() {
        var adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        clientInfo = adGroupInfo.getClientInfo();
        steps.clientPixelProviderSteps().addCpmBannerPixelsPermissions(clientInfo);
        CreativeInfo creativeInfo =
                steps.creativeSteps().addDefaultHtml5Creative(clientInfo, steps.creativeSteps().getNextCreativeId());

        bannerInfo1 = steps.bannerSteps().createBanner(
                activeCpmBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeInfo.getCreativeId())
                        .withPixels(null),
                adGroupInfo);

        bannerInfo2 = steps.bannerSteps().createBanner(
                activeCpmBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeInfo.getCreativeId())
                        .withPixels(singletonList(adfoxPixelUrl())),
                adGroupInfo);
    }

    @Test
    public void setPixelsToOneBannerAndClearPixelsInSecondBanner() {
        List<String> pixels1 = asList(tnsPixelUrl(), adriverPixelUrl());

        ModelChanges<CpmBanner> modelChanges1 = ModelChanges.build(bannerInfo1.getBannerId(),
                CpmBanner.class, CpmBanner.PIXELS, pixels1);
        ModelChanges<CpmBanner> modelChanges2 = ModelChanges.build(bannerInfo2.getBannerId(),
                CpmBanner.class, CpmBanner.PIXELS, emptyList());

        prepareAndApplyValid(asList(modelChanges1, modelChanges2));

        CpmBanner actualBanner1 = getBanner(bannerInfo1.getBannerId());
        CpmBanner actualBanner2 = getBanner(bannerInfo2.getBannerId());

        assertThat(actualBanner1.getPixels()).containsOnlyElementsOf(pixels1);
        assertThat(actualBanner2.getPixels()).isEmpty();
    }

    @Test
    public void changePixelsInTwoBanners() {
        List<String> pixels1 = asList(tnsPixelUrl(), adriverPixelUrl());
        List<String> pixels2 = asList(tnsPixelUrl(), adfoxPixelUrl());

        ModelChanges<CpmBanner> modelChanges1 = ModelChanges.build(bannerInfo1.getBannerId(),
                CpmBanner.class, CpmBanner.PIXELS, pixels1);
        ModelChanges<CpmBanner> modelChanges2 = ModelChanges.build(bannerInfo2.getBannerId(),
                CpmBanner.class, CpmBanner.PIXELS, pixels2);

        prepareAndApplyValid(asList(
                modelChanges1.castModelUp(BannerWithSystemFields.class),
                modelChanges2.castModelUp(BannerWithSystemFields.class)));

        CpmBanner actualBanner1 = getBanner(bannerInfo1.getBannerId());
        CpmBanner actualBanner2 = getBanner(bannerInfo2.getBannerId());

        assertThat(actualBanner1.getPixels()).containsOnlyElementsOf(pixels1);
        assertThat(actualBanner2.getPixels()).containsOnlyElementsOf(pixels2);
    }
}
