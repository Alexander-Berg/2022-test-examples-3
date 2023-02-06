package ru.yandex.direct.core.entity.banner.type.callouts;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithCallouts;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithCalloutsAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void calloutsNotEmpty() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        Long calloutId1 = steps.calloutSteps().createDefaultCallout(adGroupInfo.getClientInfo()).getId();
        Long calloutId2 = steps.calloutSteps().createDefaultCallout(adGroupInfo.getClientInfo()).getId();
        Long calloutId3 = steps.calloutSteps().createDefaultCallout(adGroupInfo.getClientInfo()).getId();
        List<Long> calloutIds = List.of(calloutId1, calloutId2, calloutId3);
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCalloutIds(calloutIds);

        Long id = prepareAndApplyValid(banner);

        BannerWithCallouts actualBanner = getBanner(id);
        assertThat(actualBanner.getCalloutIds(), equalTo(calloutIds));
    }

    @Test
    public void calloutsEmpty() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCalloutIds(emptyList());

        Long id = prepareAndApplyValid(banner);

        BannerWithCallouts actualBanner = getBanner(id);
        assertThat(actualBanner.getCalloutIds(), equalTo(emptyList()));
    }

}
