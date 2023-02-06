package ru.yandex.direct.core.entity.banner.type.name;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerNameStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithName;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithNameAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {
    private static final String DEFAULT_NAME = "NAME123";

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
    }

    @Test
    public void withName() {
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withName(DEFAULT_NAME);

        Long id = prepareAndApplyValid(banner);

        BannerWithName actualBanner = getBanner(id);
        assertThat(actualBanner.getName()).isEqualTo(DEFAULT_NAME);
        assertThat(actualBanner.getNameStatusModerate()).isEqualTo(BannerNameStatusModerate.READY);
    }

    @Test
    public void withoutName() {
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withName(null);

        Long id = prepareAndApplyValid(banner);

        BannerWithName actualBanner = getBanner(id);
        assertThat(actualBanner.getName()).isNull();
        assertThat(actualBanner.getNameStatusModerate()).isNull();
    }
}
