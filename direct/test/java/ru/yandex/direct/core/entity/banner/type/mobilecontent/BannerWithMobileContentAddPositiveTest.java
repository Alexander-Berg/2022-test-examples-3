package ru.yandex.direct.core.entity.banner.type.mobilecontent;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithMobileContent;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction.BUY;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.ICON;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.PRICE;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.RATING;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.RATING_VOTES;
import static ru.yandex.direct.core.testing.data.TestNewMobileAppBanners.clientMobileAppBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithMobileContentAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Before
    public void before() {
        steps.trustedRedirectSteps().addValidCounters();
    }

    @Test
    public void addMobileContentBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup();

        var reflectedAttributes = Map.of(ICON, true, PRICE, false);
        var expectedReflectedAttributes = Map.of(ICON, true, PRICE, false, RATING, false, RATING_VOTES, false);
        MobileAppBanner banner = clientMobileAppBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withHref("https://trusted1.com")
                .withImpressionUrl("https://trusted.impression.com/impression")
                .withPrimaryAction(BUY)
                .withReflectedAttributes(reflectedAttributes);

        Long bannerId = prepareAndApplyValid(banner);

        BannerWithMobileContent actualBanner = getBanner(bannerId, BannerWithMobileContent.class);
        assertThat(actualBanner.getImpressionUrl()).isEqualTo("https://trusted.impression.com/impression");
        assertThat(actualBanner.getPrimaryAction()).isEqualTo(BUY);
        assertThat(actualBanner.getReflectedAttributes()).isEqualTo(expectedReflectedAttributes);
    }

    @Test
    public void addMobileContentBannerWithoutImpressionUrl() {
        adGroupInfo = steps.adGroupSteps().createActiveMobileContentAdGroup();

        var reflectedAttributes = Map.of(ICON, true, PRICE, false);
        var expectedReflectedAttributes = Map.of(ICON, true, PRICE, false, RATING, false, RATING_VOTES, false);
        MobileAppBanner banner = clientMobileAppBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withHref("https://trusted1.com")
                .withImpressionUrl(null)
                .withPrimaryAction(BUY)
                .withReflectedAttributes(reflectedAttributes);

        Long bannerId = prepareAndApplyValid(banner);

        BannerWithMobileContent actualBanner = getBanner(bannerId, BannerWithMobileContent.class);
        assertThat(actualBanner.getImpressionUrl()).isNull();
        assertThat(actualBanner.getPrimaryAction()).isEqualTo(BUY);
        assertThat(actualBanner.getReflectedAttributes()).isEqualTo(expectedReflectedAttributes);
    }
}
