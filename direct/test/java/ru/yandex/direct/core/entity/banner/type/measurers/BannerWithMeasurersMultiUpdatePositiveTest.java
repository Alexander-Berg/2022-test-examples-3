package ru.yandex.direct.core.entity.banner.type.measurers;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithMeasurersMultiUpdatePositiveTest extends BannerClientInfoUpdateOperationTestBase {
    @Autowired
    public BannersAddOperationFactory addOperationFactory;

    private Long bannerId1;
    private Long bannerId2;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();

        bannerId1 = createBannerWithMeasurers(null);

        List<OldBannerMeasurer> measurers = List.of(
                new OldBannerMeasurer()
                        .withBannerMeasurerSystem(OldBannerMeasurerSystem.MEDIASCOPE)
                        .withParams("{\"json\": \"json\"}")
                        .withHasIntegration(true),
                new OldBannerMeasurer()
                        .withBannerMeasurerSystem(OldBannerMeasurerSystem.MOAT)
                        .withParams("{\"json1\": \"json1\"}")
                        .withHasIntegration(false));
        bannerId2 = createBannerWithMeasurers(measurers);
    }

    private Long createBannerWithMeasurers(List<OldBannerMeasurer> measurers) {
        CreativeInfo creativeInfo =
                steps.creativeSteps().addDefaultHtml5Creative(clientInfo, steps.creativeSteps().getNextCreativeId());

        BannerInfo bannerInfo = steps.bannerSteps().createBanner(
                activeCpmBanner(null, null, creativeInfo.getCreativeId())
                        .withMeasurers(measurers),
                clientInfo);

        return bannerInfo.getBannerId();
    }

    @Test
    public void setMeasurersToOneBannerAndClearMeasurersInSecondBanner() {
        var measurers1 = List.of(
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.SIZMEK)
                        .withParams("{\"json\": \"jsonSIZMEK\"}")
                        .withHasIntegration(true),
                new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.MOAT)
                        .withParams("{\"json1\": \"jsonWEBORAMA\"}")
                        .withHasIntegration(false));

        ModelChanges<CpmBanner> modelChanges1 = ModelChanges.build(bannerId1,
                CpmBanner.class, CpmBanner.MEASURERS, measurers1);
        ModelChanges<CpmBanner> modelChanges2 = ModelChanges.build(bannerId2,
                CpmBanner.class, CpmBanner.MEASURERS, null);

        prepareAndApplyValid(asList(modelChanges1, modelChanges2));

        CpmBanner actualBanner1 = getBanner(bannerId1);
        CpmBanner actualBanner2 = getBanner(bannerId2);

        assertThat(actualBanner1.getMeasurers()).containsOnlyElementsOf(measurers1);
        assertThat(actualBanner2.getMeasurers()).isEmpty();
    }

    @Test
    public void changeMeasurersInTwoBanners() {
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

        ModelChanges<CpmBanner> modelChanges1 = ModelChanges.build(bannerId1,
                CpmBanner.class, CpmBanner.MEASURERS, measurers1);
        ModelChanges<CpmBanner> modelChanges2 = ModelChanges.build(bannerId2,
                CpmBanner.class, CpmBanner.MEASURERS, measurers2);

        prepareAndApplyValid(asList(
                modelChanges1.castModelUp(BannerWithSystemFields.class),
                modelChanges2.castModelUp(BannerWithSystemFields.class)));

        CpmBanner actualBanner1 = getBanner(bannerId1);
        CpmBanner actualBanner2 = getBanner(bannerId2);

        assertThat(actualBanner1.getMeasurers()).containsOnlyElementsOf(measurers1);
        assertThat(actualBanner2.getMeasurers()).containsOnlyElementsOf(measurers2);
    }
}
