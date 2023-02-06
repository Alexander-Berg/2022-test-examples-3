package ru.yandex.direct.core.entity.banner.type.price;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.BannerWithPrice;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPrice;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithPrice;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerTurboAppsRepository;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithPriceUpdatePositiveTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithPrice> {

    @Autowired
    private OldBannerTurboAppsRepository turboAppsRepository;

    private BannerPrice defaultNewBannerPrice;
    private OldBannerPrice defaultOldBannerPrice;

    @Before
    public void setUp() {
        defaultNewBannerPrice = new BannerPrice()
                .withPrice(new BigDecimal("4.00"))
                .withCurrency(BannerPricesCurrency.RUB);

        defaultOldBannerPrice = new OldBannerPrice()
                .withPrice(new BigDecimal("3.00"))
                .withCurrency(OldBannerPricesCurrency.RUB);
    }

    @Test
    public void updateBanner_UpdateBannerPrice_BannerPriceUpdated() {
        bannerInfo = createBanner(defaultOldBannerPrice);
        Long bannerId = bannerInfo.getBannerId();

        ModelChanges<TextBanner> modelChanges = createModelChanges(bannerId, defaultNewBannerPrice);
        prepareAndApplyValid(modelChanges);
        TextBanner actualBanner = getBanner(bannerId, TextBanner.class);

        assertThat(actualBanner.getBannerPrice(), equalTo(defaultNewBannerPrice));
    }

    private ModelChanges<TextBanner> createModelChanges(Long bannerId, BannerPrice bannerPrice) {
        return new ModelChanges<>(bannerId, TextBanner.class)
                .process(bannerPrice, BannerWithPrice.BANNER_PRICE);
    }

    private TextBannerInfo createBanner(OldBannerPrice bannerPrice) {
        OldTextBanner banner = activeTextBanner(null, null)
                .withBannerPrice(bannerPrice);

        return steps.bannerSteps().createActiveTextBanner(banner);
    }
}
