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
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.thymeleaf.util.SetUtils.singletonSet;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithPixelsUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBanner> {

    private static final Path DEFAULT_PATH_ITEM = path(field(BannerWithPixels.PIXELS.name()), index(0));

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Set<String> initialPixels;

    @Parameterized.Parameter(2)
    public Set<String> newPixels;

    @Parameterized.Parameter(3)
    public Defect<Void> expectedDefect;

    @Parameterized.Parameter(4)
    public Path path;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "update pixels empty -> [null]",
                        emptySet(),
                        singletonSet(null),
                        CommonDefects.notNull(),
                        DEFAULT_PATH_ITEM
                },
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        ClientInfo defaultClient = steps.clientSteps().createDefaultClient();
        steps.clientPixelProviderSteps().addCpmBannerPixelsPermissions(defaultClient);
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultHtml5Creative(defaultClient, steps.creativeSteps().getNextCreativeId());
        bannerInfo = steps.bannerSteps().createBanner(
                activeCpmBanner(null, null, creativeInfo.getCreativeId())
                        .withPixels(new ArrayList<>(initialPixels)), defaultClient);
    }

    @Test
    public void pixelsAreUpdatedWell() {
        Long bannerId = bannerInfo.getBannerId();
        ModelChanges<CpmBanner> modelChanges = ModelChanges.build(bannerId, CpmBanner.class,
                CpmBanner.PIXELS, ifNotNull(newPixels, ArrayList::new));

        var vr =prepareAndApplyInvalid(modelChanges);

        Assert.assertThat(vr, hasDefectWithDefinition(validationError(path, expectedDefect)));
    }
}
