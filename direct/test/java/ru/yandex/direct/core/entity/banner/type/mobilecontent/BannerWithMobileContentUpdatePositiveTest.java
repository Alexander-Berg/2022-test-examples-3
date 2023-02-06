package ru.yandex.direct.core.entity.banner.type.mobilecontent;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.NewMobileAppBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithMobileContent.IMPRESSION_URL;
import static ru.yandex.direct.core.entity.banner.model.BannerWithMobileContent.PRIMARY_ACTION;
import static ru.yandex.direct.core.entity.banner.model.BannerWithMobileContent.REFLECTED_ATTRIBUTES;
import static ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction.BUY;
import static ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction.DOWNLOAD;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.ICON;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.PRICE;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.RATING;
import static ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute.RATING_VOTES;
import static ru.yandex.direct.core.testing.data.TestNewMobileAppBanners.fullMobileAppBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithMobileContentUpdatePositiveTest extends BannerNewBannerInfoUpdateOperationTestBase {

    @Before
    public void before() {
        steps.trustedRedirectSteps().addValidCounters();
    }

    @Test
    public void mobileContentBanner_UpdatePrimaryAction() {
        MobileAppBanner banner = fullMobileAppBanner(null, null)
                .withPrimaryAction(DOWNLOAD);

        bannerInfo = steps.mobileAppBannerSteps().createMobileAppBanner(new NewMobileAppBannerInfo()
                .withBanner(banner));

        var changes = new ModelChanges<>(bannerInfo.getBannerId(), MobileAppBanner.class)
                .process(BUY, PRIMARY_ACTION);

        Long id = prepareAndApplyValid(changes);
        MobileAppBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getPrimaryAction(), equalTo(BUY));
    }

    @Test
    public void mobileContentBanner_UpdateReflectedAttributes() {
        MobileAppBanner banner = fullMobileAppBanner(null, null)
                .withReflectedAttributes(Map.of(NewReflectedAttribute.PRICE, true,
                        NewReflectedAttribute.RATING, true));
        bannerInfo = steps.mobileAppBannerSteps().createMobileAppBanner(new NewMobileAppBannerInfo()
                .withBanner(banner));

        var newReflectedAttributesMask = Map.of(ICON, true, PRICE, false);

        var changes = new ModelChanges<>(bannerInfo.getBannerId(), MobileAppBanner.class)
                .process(newReflectedAttributesMask, REFLECTED_ATTRIBUTES);

        Long id = prepareAndApplyValid(changes);
        MobileAppBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getReflectedAttributes(),
                equalTo(Map.of(
                        ICON, true,
                        PRICE, false,
                        RATING, true,
                        RATING_VOTES, false)));
    }

    @Test
    public void mobileContentBanner_AddImpressionUrl() {
        MobileAppBanner banner = fullMobileAppBanner(null, null)
                .withImpressionUrl(null);
        bannerInfo = steps.mobileAppBannerSteps().createMobileAppBanner(new NewMobileAppBannerInfo()
                .withBanner(banner));

        var changes = new ModelChanges<>(bannerInfo.getBannerId(), MobileAppBanner.class)
                .process("https://trusted.impression.com/new_impression", IMPRESSION_URL);

        Long id = prepareAndApplyValid(changes);
        MobileAppBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getImpressionUrl(), equalTo("https://trusted.impression.com/new_impression"));
    }

    @Test
    public void mobileContentBanner_UpdateImpressionUrl() {
        MobileAppBanner banner = fullMobileAppBanner(null, null)
                .withImpressionUrl("https://trusted.impression.com/impression");
        bannerInfo = steps.mobileAppBannerSteps().createMobileAppBanner(new NewMobileAppBannerInfo()
                .withBanner(banner));

        var changes = new ModelChanges<>(bannerInfo.getBannerId(), MobileAppBanner.class)
                .process("https://trusted.impression.com/new_impression", IMPRESSION_URL);

        Long id = prepareAndApplyValid(changes);
        MobileAppBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getImpressionUrl(), equalTo("https://trusted.impression.com/new_impression"));
    }

    @Test
    public void mobileContentBanner_RemoveImpressionUrl() {
        MobileAppBanner banner = fullMobileAppBanner(null, null)
                .withImpressionUrl("https://trusted.impression.com/impression");
        bannerInfo = steps.mobileAppBannerSteps().createMobileAppBanner(new NewMobileAppBannerInfo()
                .withBanner(banner));

        var changes = new ModelChanges<>(bannerInfo.getBannerId(), MobileAppBanner.class)
                .process(null, IMPRESSION_URL);

        Long id = prepareAndApplyValid(changes);
        MobileAppBanner actualBanner = getBanner(id);
        assertThat(actualBanner.getImpressionUrl(), is(nullValue()));
    }
}
