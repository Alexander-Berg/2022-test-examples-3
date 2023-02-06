package ru.yandex.direct.core.entity.banner.type.displayhreftexts;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHrefTexts;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithDisplayHrefTextsAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String PREFIX = "Яндекс.Бизнес";
    private static final String SUFFIX = "ТПК Абсолют";

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
    }

    @Test
    public void withPrefix() {
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withDisplayHrefPrefix(PREFIX);

        Long id = prepareAndApplyValid(banner);

        BannerWithDisplayHrefTexts actualBanner = getBanner(id);
        assertThat(actualBanner.getDisplayHrefPrefix()).isEqualTo(PREFIX);
        assertThat(actualBanner.getDisplayHrefSuffix()).isEqualTo(null);
    }

    @Test
    public void withSuffix() {
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withDisplayHrefSuffix(SUFFIX);

        Long id = prepareAndApplyValid(banner);

        BannerWithDisplayHrefTexts actualBanner = getBanner(id);
        assertThat(actualBanner.getDisplayHrefPrefix()).isEqualTo(null);
        assertThat(actualBanner.getDisplayHrefSuffix()).isEqualTo(SUFFIX);
    }

    @Test
    public void withBoth() {
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withDisplayHrefPrefix(PREFIX)
                .withDisplayHrefSuffix(SUFFIX);

        Long id = prepareAndApplyValid(banner);

        BannerWithDisplayHrefTexts actualBanner = getBanner(id);
        assertThat(actualBanner.getDisplayHrefPrefix()).isEqualTo(PREFIX);
        assertThat(actualBanner.getDisplayHrefSuffix()).isEqualTo(SUFFIX);
    }
}
