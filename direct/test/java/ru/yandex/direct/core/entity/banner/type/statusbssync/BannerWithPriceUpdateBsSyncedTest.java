package ru.yandex.direct.core.entity.banner.type.statusbssync;

import java.math.BigDecimal;

import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPrice;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;

import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithPriceUpdateBsSyncedTest extends BannerWithRelatedEntityUpdateBsSyncedTestBase<TextBanner> {

    @Override
    protected Long createPlainBanner() {
        return steps.bannerSteps().createBanner(activeTextBanner(), clientInfo).getBannerId();
    }

    @Override
    protected Long createBannerWithRelatedEntity() {
        OldBannerPrice bannerPrice = new OldBannerPrice()
                .withPrice(BigDecimal.TEN)
                .withCurrency(OldBannerPricesCurrency.RUB);

        OldTextBanner banner = activeTextBanner().withBannerPrice(bannerPrice);
        TextBannerInfo bannerInfo = steps.bannerSteps().createBanner(banner, clientInfo);
        return bannerInfo.getBannerId();
    }

    @Override
    protected ModelChanges<TextBanner> getPlainModelChanges(Long bannerId) {
        return new ModelChanges<>(bannerId, TextBanner.class);
    }

    @Override
    protected void setNewRelatedEntity(ModelChanges<TextBanner> modelChanges) {
        BannerPrice bannerPrice = new BannerPrice()
                .withPrice(BigDecimal.valueOf(3))
                .withCurrency(BannerPricesCurrency.RUB);

        modelChanges.process(bannerPrice, TextBanner.BANNER_PRICE);
    }

    @Override
    protected void deleteRelatedEntity(ModelChanges<TextBanner> modelChanges) {
        modelChanges.process(null, TextBanner.BANNER_PRICE);
    }
}
