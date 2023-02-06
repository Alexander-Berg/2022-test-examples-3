package ru.yandex.direct.core.entity.banner.type.price;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithPriceAddPositiveTest extends BannerAdGroupInfoAddOperationTestBase {

    @Test
    public void validBannerPriceForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        BannerPrice bannerPrice = new BannerPrice()
                .withPrice(new BigDecimal("3.00"))
                .withCurrency(BannerPricesCurrency.RUB);

        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withBannerPrice(bannerPrice);

        Long id = prepareAndApplyValid(banner);
        TextBanner actualBanner = getBanner(id, TextBanner.class);
        assertThat(actualBanner.getBannerPrice(), equalTo(bannerPrice));
    }
}
