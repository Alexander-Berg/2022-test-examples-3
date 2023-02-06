package ru.yandex.direct.intapi.entity.metrika.service;

import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.BannerImageInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.IntApiException;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.metrika.model.MetrikaBannersParam;
import ru.yandex.direct.intapi.entity.metrika.model.MetrikaBannersResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaBannersServiceTest {

    @Autowired
    private MetrikaBannersService metrikaBannersService;

    @Autowired
    private Steps steps;

    @Test
    public void getBanners() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner();

        steps.bsFakeSteps().setOrderId(bannerInfo.getCampaignInfo());

        steps.bsFakeSteps().setBsBannerId(bannerInfo);

        MetrikaBannersParam param = new MetrikaBannersParam()
                .withBannerId(bannerInfo.getBanner().getBsBannerId())
                .withOrderId(bannerInfo.getCampaignInfo().getCampaign().getOrderId());

        List<MetrikaBannersResult> results = metrikaBannersService.getBanners(Collections.singletonList(param));

        assumeThat("В списке результатов 1 элемент", results, hasSize(1));

        MetrikaBannersResult expected = (MetrikaBannersResult) new MetrikaBannersResult()
                .withBid(bannerInfo.getBannerId())
                .withBody(bannerInfo.getBanner().getBody())
                .withDomain(bannerInfo.getBanner().getDomain())
                .withTitle(bannerInfo.getBanner().getTitle())
                .withIsImageBanner(false)
                .withOrderId(param.getOrderId())
                .withBannerId(param.getBannerId());

        assertThat("В ответе верные данные", results.get(0), beanDiffer(expected));
    }

    @Test
    public void getImageBanners() {
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner();

        steps.bsFakeSteps().setOrderId(bannerInfo.getCampaignInfo());

        BannerImageInfo<TextBannerInfo> bannerImageInfo = steps.bannerSteps().createBannerImage(bannerInfo);

        steps.bsFakeSteps().setBsBannerId(bannerImageInfo);

        Long bsBannerId = bannerImageInfo.getBannerImage().getBsBannerId();

        MetrikaBannersParam param = new MetrikaBannersParam()
                .withBannerId(bsBannerId)
                .withOrderId(bannerInfo.getCampaignInfo().getCampaign().getOrderId());

        List<MetrikaBannersResult> results = metrikaBannersService.getBanners(Collections.singletonList(param));

        assumeThat("В списке результатов 1 элемент", results, hasSize(1));

        MetrikaBannersResult expected = (MetrikaBannersResult) new MetrikaBannersResult()
                .withBid(bannerInfo.getBannerId())
                .withBody(bannerInfo.getBanner().getBody())
                .withDomain(bannerInfo.getBanner().getDomain())
                .withTitle(bannerInfo.getBanner().getTitle())
                .withIsImageBanner(true)
                .withOrderId(param.getOrderId())
                .withBannerId(param.getBannerId());

        assertThat("В ответе верные данные", results.get(0), beanDiffer(expected));
    }

    @Test(expected = IntApiException.class)
    public void throwsExceptionWhenValidationFails() {
        metrikaBannersService.getBanners(null);
    }
}
