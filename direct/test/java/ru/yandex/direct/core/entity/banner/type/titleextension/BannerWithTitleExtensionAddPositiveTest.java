package ru.yandex.direct.core.entity.banner.type.titleextension;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTitleExtensionAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String TITLE_EXTENSION = "some title extension";

    @Test
    public void addValidTitleExtension() {
        adGroupInfo = steps.adGroupSteps().createDefaultAdGroup();
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTitleExtension(TITLE_EXTENSION);

        Long id = prepareAndApplyValid(banner);

        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getTitleExtension(), equalTo(TITLE_EXTENSION));
    }
}
