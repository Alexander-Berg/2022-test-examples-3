package ru.yandex.direct.core.entity.banner.type.pixels;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem;
import ru.yandex.direct.core.entity.banner.model.BannerWithPixels;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.pixels.Provider;
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.noRightsToPixel;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adriverPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.tnsPixelUrl;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerPixelsAndMeasurersTest extends BannerAdGroupInfoAddOperationTestBase {
    //если пиксель запрещён по пермишенам и не совместим с измерителями, то покажем только ошибку про пермишены.
    private CreativeInfo creativeInfo;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps()
                .addDefaultHtml5Creative(adGroupInfo.getClientInfo(), steps.creativeSteps().getNextCreativeId());
        steps.clientPixelProviderSteps().addClientPixelProviderPermissionCpmBanner(adGroupInfo.getClientInfo(),
                Provider.TNS);
    }

    @Test
    public void invalidPixelWithMeasurer_invalidPixelWithMeasurerError() {
        //разрешён, не совместим. Одна ошибка про совместимость
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMeasurers(List.of(new BannerMeasurer()
                        .withBannerMeasurerSystem(BannerMeasurerSystem.MEDIASCOPE)
                        .withParams("{\"json1\": \"json1\"}")
                        .withHasIntegration(false)))
                .withPixels(List.of(tnsPixelUrl()));
        var vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectWithDefinition(validationError(path(field(BannerWithPixels.PIXELS.name()), index(0)),
                BannerDefects.invalidPixelWithMeasurer(tnsPixelUrl(), BannerMeasurerSystem.MEDIASCOPE.name()))));
    }

    @Test
    public void noRightsToPixel_oneError() {
        //не разрешён, не совместим. Одна ошибка про пермишены
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMeasurers(List.of(new BannerMeasurer()
                    .withBannerMeasurerSystem(BannerMeasurerSystem.ADRIVER)
                    .withParams("{\"json1\": \"json1\"}")
                    .withHasIntegration(false)))
                .withPixels(List.of(adriverPixelUrl()));
        var vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectWithDefinition(validationError(path(field(BannerWithPixels.PIXELS.name()), index(0)),
                noRightsToPixel(adriverPixelUrl(), Set.of(PixelProvider.TNS, PixelProvider.ADFOX)))));
        assertThat("нет других ошибок", vr.flattenErrors(), hasSize(1));
    }
}
