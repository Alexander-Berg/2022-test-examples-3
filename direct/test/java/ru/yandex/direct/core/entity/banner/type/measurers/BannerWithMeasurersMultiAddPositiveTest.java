package ru.yandex.direct.core.entity.banner.type.measurers;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithMeasurersMultiAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {
    private CreativeInfo creativeInfo;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(),
                steps.creativeSteps().getNextCreativeId());
    }

    @Test
    public void oneBannerWithMeasurersAndOneWithout() {
        List<BannerMeasurer> measurers = List.of(
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.OMI)
                        .withParams("{\"json\": \"json\"}")
                        .withHasIntegration(true),
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.ADRIVER)
                        .withParams("{\"json1\": \"json1\"}")
                        .withHasIntegration(false));

        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMeasurers(measurers);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMeasurers(null);

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getMeasurers()).containsOnlyElementsOf(measurers);
        assertThat(actualBanner2.getMeasurers()).isEmpty();
    }

    @Test
    public void severalBannersWithMeasurers() {
        var measurers1 = List.of(
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.OMI)
                        .withParams("{\"json\": \"json\"}")
                        .withHasIntegration(true),
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.ADRIVER)
                        .withParams("{\"json1\": \"json1\"}")
                        .withHasIntegration(false));
        var measurers2 = List.of(
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.SIZMEK)
                        .withParams("{\"json\": \"json\"}")
                        .withHasIntegration(true),
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.ADLOOX)
                        .withParams("{\"json1\": \"json1\"}")
                        .withHasIntegration(false));

        CpmBanner banner1 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMeasurers(measurers1);
        CpmBanner banner2 = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMeasurers(measurers2);

        List<Long> bannerIds = prepareAndApplyValid(asList(banner1, banner2));

        CpmBanner actualBanner1 = getBanner(bannerIds.get(0));
        CpmBanner actualBanner2 = getBanner(bannerIds.get(1));

        assertThat(actualBanner1.getMeasurers()).containsOnlyElementsOf(measurers1);
        assertThat(actualBanner2.getMeasurers()).containsOnlyElementsOf(measurers2);
    }
}
