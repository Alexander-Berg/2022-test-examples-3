package ru.yandex.direct.core.entity.banner.type.measurers;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerMeasurer;
import ru.yandex.direct.core.entity.banner.model.BannerWithMeasurers;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerMeasurerSystem.MEDIASCOPE;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithMeasurersAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {
    private CreativeInfo creativeInfo;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(),
                steps.creativeSteps().getNextCreativeId());
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
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withMeasurers(measurers);

        var validationResult = prepareAndApplyInvalid(banner);

        assertThat(validationResult, hasDefectWithDefinition(validationError(
                path(field(BannerWithMeasurers.MEASURERS.name())),
                duplicatedElement())));
    }
}
