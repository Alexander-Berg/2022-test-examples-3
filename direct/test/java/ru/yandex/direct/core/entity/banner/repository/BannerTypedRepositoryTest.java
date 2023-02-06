package ru.yandex.direct.core.entity.banner.repository;

import java.util.List;

import jdk.jfr.Description;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithBody;
import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.BannerWithLanguage;
import ru.yandex.direct.core.entity.banner.model.BannerWithTurboLandingParams;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.Steps;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.repository.filter.BannerFilterFactory.bannerIdFilter;
import static ru.yandex.direct.core.testing.steps.ClientSteps.DEFAULT_SHARD;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class BannerTypedRepositoryTest {

    @Autowired
    private Steps steps;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    @Test
    public void getBannersSafely_severalInterfaces() {
        var bannerInfo = steps.textBannerSteps().createDefaultTextBanner();

        var foundBanners = bannerTypedRepository
                .getSafely(DEFAULT_SHARD, bannerIdFilter(singletonList(bannerInfo.getBannerId())),
                        List.of(BannerWithHref.class, BannerWithLanguage.class));
        var banners = mapList(foundBanners, (banner) -> (TextBanner) banner);

        assertThat(banners).hasSize(1);
        SoftAssertions.assertSoftly(softly -> {
            var banner = banners.get(0);
            softly.assertThat(banner.getHref()).isNotNull();
            softly.assertThat(banner.getLanguage()).isNotNull();
            softly.assertThat(banner.getStatusModerate()).isNull();
        });
    }

    @Test
    @Description("Проверяем, что список классов передаваемый в getSafely интерпретируется через 'или', а не через 'и'.")
    public void getBannersSafely_ThreeInterfaces_BannerExtendsOnlyOne() {
        var dynamicBannerInfo = steps.dynamicBannerSteps().createDefaultDynamicBanner();
        var dynamicBanner = (DynamicBanner) dynamicBannerInfo.getBanner();
        checkState(!(dynamicBanner instanceof BannerWithTurboLandingParams));

        var foundBanners = bannerTypedRepository
                .getSafely(DEFAULT_SHARD, bannerIdFilter(List.of(dynamicBannerInfo.getBannerId())),
                        List.of(BannerWithTurboLandingParams.class, BannerWithHref.class, BannerWithBody.class));
        var banners = mapList(foundBanners, (banner) -> (DynamicBanner) banner);

        assertThat(banners).hasSize(1);
        SoftAssertions.assertSoftly(softly -> {
            var banner = banners.get(0);
            softly.assertThat(banner.getHref()).isNotNull();
            softly.assertThat(banner.getBody()).isNotNull();
            softly.assertThat(banner.getStatusModerate()).isNull();
        });
    }

}
