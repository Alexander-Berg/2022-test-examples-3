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
import ru.yandex.direct.core.entity.banner.model.pixels.Provider;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.adfoxPixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.mailRuTop100PixelUrl;
import static ru.yandex.direct.core.testing.data.BannerPixelsTestData.tnsPixelUrl;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.CommonUtils.nvl;

@CoreTest
@RunWith(Parameterized.class)
public class BannerWithPixelsAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public Set<String> pixelsToSave;

    private CreativeInfo creativeInfo;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        "pixels == emptyList()",
                        emptySet(),
                },
                {
                        "one pixel",
                        Set.of(tnsPixelUrl()),
                },
                {
                        "two pixels",
                        Set.of(tnsPixelUrl(), adfoxPixelUrl()),
                },
                {
                        "Mail.ru top-100 pixel",
                        Set.of(mailRuTop100PixelUrl()),
                },
        });
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps()
                .addDefaultHtml5Creative(adGroupInfo.getClientInfo(), steps.creativeSteps().getNextCreativeId());
        steps.clientPixelProviderSteps().addCpmBannerPixelsPermissions(adGroupInfo.getClientInfo());
        steps.clientPixelProviderSteps().addClientPixelProviderPermissionCpmBanner(adGroupInfo.getClientInfo(), Provider.MAIL_RU_TOP_100);
    }

    @Test
    public void pixelsAreSavedWell() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withPixels(ifNotNull(pixelsToSave, ArrayList::new));
        Long bannerId = prepareAndApplyValid(banner);

        CpmBanner actualBanner = getBanner(bannerId);
        assertThat(actualBanner.getPixels()).containsOnlyElementsOf(nvl(pixelsToSave, emptySet()));
    }
}
