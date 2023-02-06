package ru.yandex.direct.core.entity.banner.type.measurers;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerWithMeasurers;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.service.BannersAddOperationFactory;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem.MEDIASCOPE;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithMeasurersUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBanner> {
    @Autowired
    public BannersAddOperationFactory addOperationFactory;

    private Long bannerId;

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        ClientInfo defaultClient = steps.clientSteps().createDefaultClient();

        CreativeInfo creativeInfo =
                steps.creativeSteps().addDefaultHtml5Creative(defaultClient, steps.creativeSteps().getNextCreativeId());

        bannerInfo = steps.bannerSteps().createBanner(
                activeCpmBanner(null, null, creativeInfo.getCreativeId())
                        .withMeasurers(null),
                defaultClient);

        bannerId = bannerInfo.getBannerId();
    }

    @Test
    public void validationTest() {
        //негативный теста на измерители в сборе, который проверит, что в целом валидация подключена и срабатывает
        List<BannerMeasurer> measurers = List.of(
                new BannerMeasurer()
                        .withBannerMeasurerSystem(MEDIASCOPE)
                        .withParams("{\"blab\": \"labla\"}"),
                new BannerMeasurer()
                        .withBannerMeasurerSystem(MEDIASCOPE)
                        .withParams("{\"foo\": \"bar\"}"));
        ModelChanges<CpmBanner> modelChanges = ModelChanges.build(bannerId, CpmBanner.class,
                CpmBanner.MEASURERS, measurers);

        var validationResult = prepareAndApplyInvalid(modelChanges);

        assertThat(validationResult, hasDefectWithDefinition(validationError(
                path(field(BannerWithMeasurers.MEASURERS.name())),
                duplicatedElement())));
    }
}
