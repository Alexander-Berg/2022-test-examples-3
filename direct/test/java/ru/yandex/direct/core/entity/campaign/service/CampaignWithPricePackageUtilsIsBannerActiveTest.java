package ru.yandex.direct.core.entity.campaign.service;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.CampaignWithPricePackageUtils.isBannerActive;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithPricePackageUtilsIsBannerActiveTest {

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public OldCpmBanner banner;

    @Parameterized.Parameter(2)
    public Boolean expectedResult;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return asList(new Object[][]{
                {"активный баннер",
                        activeBanner(), true},
                {"остановленный баннер",
                        activeBanner().withStatusShow(false), false},
                {"архивный баннер",
                        activeBanner().withStatusArchived(true), false},
                {"баннер не прошедший модерацию",
                        activeBanner().withStatusModerate(OldBannerStatusModerate.NO), false},
                {"баннер не прошедший пост модерацию",
                        activeBanner().withStatusPostModerate(OldBannerStatusPostModerate.NO), false}
        });
    }

    private static OldCpmBanner activeBanner() {
        return new OldCpmBanner()
                .withStatusShow(true)
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withStatusPostModerate(OldBannerStatusPostModerate.YES)
                .withStatusArchived(false);
    }

    @Test
    public void test() {
        assertThat(isBannerActive(banner)).isEqualTo(expectedResult);
    }
}
