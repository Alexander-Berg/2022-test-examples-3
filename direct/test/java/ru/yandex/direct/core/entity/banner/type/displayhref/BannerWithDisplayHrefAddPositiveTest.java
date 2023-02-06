package ru.yandex.direct.core.entity.banner.type.displayhref;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerDisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHref.DISPLAY_HREF;
import static ru.yandex.direct.core.entity.banner.model.BannerWithDisplayHref.DISPLAY_HREF_STATUS_MODERATE;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithDisplayHrefAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String VALID_DISPLAY_HREF = "/display-href";

    @Test
    public void validDisplayHref() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();

        TextBanner banner = fullTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withDisplayHref(VALID_DISPLAY_HREF);
        Long id = prepareAndApplyValid(banner);

        assertThat(getBanner(id), allOf(
                hasProperty(DISPLAY_HREF.name(), equalTo(VALID_DISPLAY_HREF)),
                hasProperty(DISPLAY_HREF_STATUS_MODERATE.name(), equalTo(BannerDisplayHrefStatusModerate.READY))
        ));
    }
}
