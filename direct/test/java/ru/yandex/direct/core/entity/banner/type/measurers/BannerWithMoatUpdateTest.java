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
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithMoatUpdateTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBanner> {
    private static final String DEFAULT_PARAMS = "{\"yandex\": 173}";
    private static final String PARAMS_MOAT_USE_UNSTABLE_SCRIPT = "{\"yandex\": 173, \"use_unstable_script\": true}";
    private ClientInfo defaultClientInfo;
    List<BannerMeasurer> measurers;

    @Before
    public void setUp() throws Exception {
        measurers = List.of(
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.MOAT)
                        .withParams(DEFAULT_PARAMS)
                        .withHasIntegration(false));
        List<OldBannerMeasurer> oldMeasurers = List.of(
                new OldBannerMeasurer()
                        .withBannerMeasurerSystem(OldBannerMeasurerSystem.MOAT)
                        .withParams("{}")
                        .withHasIntegration(false));
        defaultClientInfo = steps.clientSteps().createDefaultClient();
        long creativeId = steps.creativeSteps().getNextCreativeId();
        steps.creativeSteps().addDefaultHtml5Creative(defaultClientInfo, creativeId);
        bannerInfo = steps.bannerSteps().createBanner(
                activeCpmBanner(null, null, creativeId)
                        .withMeasurers(oldMeasurers), defaultClientInfo);
    }

    @Test
    public void updateBanner_paramsChanged() {
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(measurers, BannerWithMeasurers.MEASURERS);
        steps.featureSteps().addClientFeature(defaultClientInfo.getClientId(),
                FeatureName.MOAT_USE_UNSTABLE_SCRIPT, false);
        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);
        assertThat(actualBanner.getMeasurers().get(0).getParams(), equalTo(DEFAULT_PARAMS));
    }

    @Test
    public void updateBanner_moat_use_unstable_script() {
        var modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), CpmBanner.class)
                .process(measurers, BannerWithMeasurers.MEASURERS);
        steps.featureSteps().addClientFeature(defaultClientInfo.getClientId(),
                FeatureName.MOAT_USE_UNSTABLE_SCRIPT, true);
        Long id = prepareAndApplyValid(modelChanges);
        CpmBanner actualBanner = getBanner(id, CpmBanner.class);
        assertThat(actualBanner.getMeasurers().get(0).getParams(), equalTo(PARAMS_MOAT_USE_UNSTABLE_SCRIPT));
    }
}
