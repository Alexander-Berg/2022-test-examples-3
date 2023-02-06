package ru.yandex.direct.core.entity.banner.type.logos;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.StatusBannerLogoModerate;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithLogoMultiUpdatePositiveTest extends BannerClientInfoUpdateOperationTestBase {
    @Autowired
    public BannersAddOperationFactory addOperationFactory;

    private CreativeInfo creativeInfo;
    private String imageHash1;
    private String imageHash2;
    private String imageHash3;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        clientInfo = adGroupInfo.getClientInfo();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(clientInfo,
                steps.creativeSteps().getNextCreativeId());

        imageHash1 = steps.bannerSteps().createLogoImageFormat(clientInfo).getImageHash();
        imageHash2 = steps.bannerSteps().createLogoImageFormat(clientInfo).getImageHash();
        imageHash3 = steps.bannerSteps().createLogoImageFormat(clientInfo).getImageHash();
    }

    @Test
    public void update() {
        BannerInfo bannerWithoutLogo = steps.bannerSteps().createBanner(
                activeCpmBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeInfo.getCreativeId()),
                clientInfo);
        BannerInfo bannerWithLogo = steps.bannerSteps().createBanner(
                activeCpmBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId(), creativeInfo.getCreativeId())
                        .withAdGroupId(adGroupInfo.getAdGroupId())
                        .withLogoImageHash(imageHash1)
                        .withLogoStatusModerate(StatusBannerLogoModerate.YES),
                clientInfo);

        Long bannerId1 = bannerWithoutLogo.getBannerId();
        Long bannerId2 = bannerWithLogo.getBannerId();

        ModelChanges<CpmBanner> modelChanges1 = ModelChanges.build(bannerId1, CpmBanner.class,
                CpmBanner.LOGO_IMAGE_HASH, imageHash2);
        ModelChanges<CpmBanner> modelChanges2 = ModelChanges.build(bannerId2, CpmBanner.class,
                CpmBanner.LOGO_IMAGE_HASH, imageHash3);

        prepareAndApplyValid(asList(modelChanges1, modelChanges2));

        CpmBanner actualBanner1 = getBanner(bannerId1);
        CpmBanner actualBanner2 = getBanner(bannerId2);

        assertThat(actualBanner1.getLogoImageHash()).isEqualTo(imageHash2);
        assertThat(actualBanner1.getLogoStatusModerate()).isEqualTo(BannerLogoStatusModerate.READY);
        assertThat(actualBanner2.getLogoImageHash()).isEqualTo(imageHash3);
        assertThat(actualBanner2.getLogoStatusModerate()).isEqualTo(BannerLogoStatusModerate.READY);
    }
}
