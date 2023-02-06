package ru.yandex.direct.core.entity.banner.type.pixels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adriverPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.tnsPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.yaAudiencePixelUrl;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmBanner;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.CommonUtils.nvl;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithPixelsUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBanner> {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Set<String> initialPixels;

    @Parameterized.Parameter(2)
    public Set<String> newPixels;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "update pixels empty -> empty",
                        emptySet(),
                        emptySet(),
                },
                {
                        "update pixels empty -> one",
                        emptySet(),
                        Set.of(tnsPixelUrl()),
                },
                {
                        "update pixels empty -> two",
                        emptySet(),
                        Set.of(tnsPixelUrl(), adfoxPixelUrl()),
                },
                {
                        "update pixels one -> empty",
                        Set.of(tnsPixelUrl()),
                        emptySet(),
                },
                {
                        "update pixels one -> the same",
                        Set.of(tnsPixelUrl()),
                        Set.of(tnsPixelUrl()),
                },
                {
                        "update pixels one -> another",
                        Set.of(tnsPixelUrl()),
                        Set.of(adfoxPixelUrl()),
                },
                {
                        "update pixels one -> one + another",
                        Set.of(tnsPixelUrl()),
                        Set.of(adfoxPixelUrl(), tnsPixelUrl()),
                },
                {
                        "update pixels two -> empty",
                        Set.of(adfoxPixelUrl(), tnsPixelUrl()),
                        emptySet(),
                },
                {
                        "update pixels two -> the same",
                        Set.of(adfoxPixelUrl(), tnsPixelUrl()),
                        Set.of(adfoxPixelUrl(), tnsPixelUrl()),
                },
                {
                        "update pixels two -> one of two",
                        Set.of(adfoxPixelUrl(), tnsPixelUrl()),
                        Set.of(tnsPixelUrl()),
                },
                {
                        "update pixels two -> one another",
                        Set.of(adfoxPixelUrl(), tnsPixelUrl()),
                        Set.of(yaAudiencePixelUrl()),
                },
                {
                        "update pixels two -> one of two and one another",
                        Set.of(adfoxPixelUrl(), tnsPixelUrl()),
                        Set.of(tnsPixelUrl(), yaAudiencePixelUrl()),
                },
                {
                        "update pixels two -> two another",
                        Set.of(adfoxPixelUrl(), tnsPixelUrl()),
                        Set.of(adriverPixelUrl(), yaAudiencePixelUrl()),
                },
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        var adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        ClientInfo defaultClient = adGroupInfo.getClientInfo();
        steps.clientPixelProviderSteps().addCpmBannerPixelsPermissions(defaultClient);
        CreativeInfo creativeInfo = steps.creativeSteps()
                .addDefaultHtml5Creative(defaultClient, steps.creativeSteps().getNextCreativeId());
        bannerInfo = steps.bannerSteps().createBanner(
                activeCpmBanner(null, null, creativeInfo.getCreativeId())
                        .withPixels(new ArrayList<>(initialPixels)), adGroupInfo);
    }

    @Test
    public void pixelsAreUpdatedWell() {
        Long bannerId = bannerInfo.getBannerId();
        ModelChanges<CpmBanner> modelChanges = ModelChanges.build(bannerId, CpmBanner.class,
                CpmBanner.PIXELS, ifNotNull(newPixels, ArrayList::new));

        prepareAndApplyValid(modelChanges);

        CpmBanner actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getPixels()).containsOnlyElementsOf(nvl(newPixels, emptySet()));
    }
}
