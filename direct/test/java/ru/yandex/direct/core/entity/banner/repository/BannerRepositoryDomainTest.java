package ru.yandex.direct.core.entity.banner.repository;

import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithHref;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.repository.filter.BannerFilterFactory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerRepositoryDomainTest {
    private static final String DOMAIN = "newbannerepodomain.ru";
    @Autowired
    private Steps steps;
    @Autowired
    private BannerTypedRepository typedRepository;

    private NewTextBannerInfo bannerInfo;

    @Before
    public void setUp() {
        bannerInfo = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                .withBanner(fullTextBanner()
                .withDomain(DOMAIN))
        );
    }

    @Test
    public void testGetBannersByDomain() {
        TextBanner expectedBanner = bannerInfo.getBanner();
        var banners = typedRepository.getSafely(bannerInfo.getShard(),
                BannerFilterFactory.bannerDomainFilter(Set.of(expectedBanner.getDomain())), BannerWithHref.class);
        assumeThat(banners, hasSize(1));

        SoftAssertions.assertSoftly(softly -> {
            var actualBanner = banners.get(0);
            softly.assertThat(actualBanner.getDomain()).isEqualTo(expectedBanner.getDomain());
            softly.assertThat(actualBanner.getHref()).isEqualTo(expectedBanner.getHref());
            softly.assertThat(actualBanner.getId()).isEqualTo(expectedBanner.getId());
            softly.assertThat(actualBanner.getAdGroupId()).isEqualTo(expectedBanner.getAdGroupId());
            softly.assertThat(actualBanner.getCampaignId()).isEqualTo(expectedBanner.getCampaignId());
        });
    }
}
