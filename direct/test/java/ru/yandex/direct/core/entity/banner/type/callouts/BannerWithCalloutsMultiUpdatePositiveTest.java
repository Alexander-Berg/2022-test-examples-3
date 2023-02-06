package ru.yandex.direct.core.entity.banner.type.callouts;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithCallouts;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerClientInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithCalloutsMultiUpdatePositiveTest extends BannerClientInfoUpdateOperationTestBase {

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void test() {
        Long calloutId1 = steps.calloutSteps().createDefaultCallout(clientInfo).getId();
        Long calloutId2 = steps.calloutSteps().createDefaultCallout(clientInfo).getId();
        Long calloutId3 = steps.calloutSteps().createDefaultCallout(clientInfo).getId();

        List<Long> old1 = List.of(calloutId1, calloutId2);
        List<Long> new1 = List.of(calloutId3, calloutId2, calloutId1);

        List<Long> old2 = List.of();
        List<Long> new2 = List.of(calloutId1);

        List<Long> old3 = List.of(calloutId3, calloutId2, calloutId1);
        List<Long> new3 = List.of();

        List<Long> old4 = List.of(calloutId2);

        TextBannerInfo bannerInfo1 = createBanner(old1);
        TextBannerInfo bannerInfo2 = createBanner(old2);
        TextBannerInfo bannerInfo3 = createBanner(old3);
        TextBannerInfo bannerInfo4 = createBanner(old4);

        ModelChanges<TextBanner> modelChanges1 =
                new ModelChanges<>(bannerInfo1.getBannerId(), TextBanner.class)
                        .process(new1, BannerWithCallouts.CALLOUT_IDS);
        ModelChanges<TextBanner> modelChanges2 =
                new ModelChanges<>(bannerInfo2.getBannerId(), TextBanner.class)
                        .process(new2, BannerWithCallouts.CALLOUT_IDS);
        ModelChanges<TextBanner> modelChanges3 =
                new ModelChanges<>(bannerInfo3.getBannerId(), TextBanner.class)
                        .process(new3, BannerWithCallouts.CALLOUT_IDS);

        prepareAndApplyValid(List.of(modelChanges1, modelChanges2, modelChanges3));

        BannerWithCallouts actualBanner1 = getBanner(bannerInfo1.getBannerId(), TextBanner.class);
        assertThat(actualBanner1.getCalloutIds(), equalTo(new1));
        BannerWithCallouts actualBanner2 = getBanner(bannerInfo2.getBannerId(), TextBanner.class);
        assertThat(actualBanner2.getCalloutIds(), equalTo(new2));
        BannerWithCallouts actualBanner3 = getBanner(bannerInfo3.getBannerId(), TextBanner.class);
        assertThat(actualBanner3.getCalloutIds(), equalTo(new3));
        BannerWithCallouts actualBanner4 = getBanner(bannerInfo4.getBannerId(), TextBanner.class);
        // баннер не менялся, поэтому значение сохранилось
        assertThat(actualBanner4.getCalloutIds(), equalTo(old4));
    }

    private TextBannerInfo createBanner(List<Long> callouts) {
        // степы кривоваты, покороче не получилось написать. кажется не критично в свете скорого перехода
        // на новые степы
        OldTextBanner banner = activeTextBanner().withCalloutIds(callouts);
        return steps.bannerSteps().createBanner(banner, clientInfo);
    }

}
