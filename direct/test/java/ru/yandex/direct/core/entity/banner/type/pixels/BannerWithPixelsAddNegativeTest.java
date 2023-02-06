package ru.yandex.direct.core.entity.banner.type.pixels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.BannerWithPixels;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static org.thymeleaf.util.SetUtils.singletonSet;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.noRightsToPixel;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.mailRuTop100PixelUrl;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithPixelsAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final Path DEFAULT_PATH_ITEM = path(field(BannerWithPixels.PIXELS.name()), index(0));

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Set<String> pixelsToSave;

    @Parameterized.Parameter(2)
    public Defect<Void> expectedDefect;

    @Parameterized.Parameter(3)
    public Path path;

    private CreativeInfo creativeInfo;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "pixels == [null]",
                        singletonSet(null),
                        CommonDefects.notNull(),
                        DEFAULT_PATH_ITEM
                },
                {
                        "не выдан доступ в client_pixel_providers для Mail.ru top-100",
                        singletonSet(mailRuTop100PixelUrl()),
                        noRightsToPixel(mailRuTop100PixelUrl(), Set.of(PixelProvider.ADFOX)),
                        DEFAULT_PATH_ITEM
                },
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps()
                .addDefaultHtml5Creative(adGroupInfo.getClientInfo(), steps.creativeSteps().getNextCreativeId());
    }

    @Test
    public void validationTest() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withPixels(ifNotNull(pixelsToSave, ArrayList::new));
        var vr = prepareAndApplyInvalid(banner);

        Assert.assertThat(vr, hasDefectWithDefinition(validationError(path, expectedDefect)));
    }
}
