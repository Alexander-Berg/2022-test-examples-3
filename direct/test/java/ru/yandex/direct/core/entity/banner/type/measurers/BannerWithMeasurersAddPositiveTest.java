package ru.yandex.direct.core.entity.banner.type.measurers;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.model.BannerWithMeasurers;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.feature.FeatureName;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithMeasurersAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {
    private static final String PARAMS_MOAT_USE_UNSTABLE_SCRIPT = "{\"use_unstable_script\": true}";
    private CreativeInfo creativeInfo;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(),
                steps.creativeSteps().getNextCreativeId());
    }

    @Test
    public void measurersNotEmpty() {
        List<BannerMeasurer> measurers = List.of(
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.MOAT)
                        .withParams("{\"json\": \"json\"}")
                        .withHasIntegration(true),
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.SIZMEK)
                        .withParams("{\"json1\": \"json1\"}")
                        .withHasIntegration(false));
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMeasurers(measurers);

        Long id = prepareAndApplyValid(banner);

        BannerWithMeasurers actualBanner = getBanner(id);
        assertThat(actualBanner.getMeasurers(), containsInAnyOrder(measurers.get(0), measurers.get(1)));
    }

    @Test
    public void measurersEmpty() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMeasurers(emptyList());

        Long id = prepareAndApplyValid(banner);

        BannerWithMeasurers actualBanner = getBanner(id);
        assertThat(actualBanner.getMeasurers(), equalTo(emptyList()));
    }

    @Test
    public void moatUseUnstableScript() {
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(),
                FeatureName.MOAT_USE_UNSTABLE_SCRIPT, true);
        List<BannerMeasurer> measurers = List.of(
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.MOAT)
                        .withParams("{}")
                        .withHasIntegration(true));
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMeasurers(measurers);

        Long id = prepareAndApplyValid(banner);

        BannerWithMeasurers actualBanner = getBanner(id);
        assertThat(actualBanner.getMeasurers().get(0).getParams(), equalTo(PARAMS_MOAT_USE_UNSTABLE_SCRIPT));
    }
}
