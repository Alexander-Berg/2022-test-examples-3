package ru.yandex.direct.core.entity.banner.type.leadformattributes;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithLeadformAttributes;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithLeadformAttributesAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String HREF = "https://yandex.ru";
    private static final String BUTTON_TEXT = "Оставить заявку";

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
    }

    @Test
    public void withHrefAndButtonText() {
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withLeadformHref(HREF)
                .withLeadformButtonText(BUTTON_TEXT);

        Long id = prepareAndApplyValid(banner);

        BannerWithLeadformAttributes actualBanner = getBanner(id);
        assertThat(actualBanner.getLeadformHref()).isEqualTo(HREF);
        assertThat(actualBanner.getLeadformButtonText()).isEqualTo(BUTTON_TEXT);
    }
}
