package ru.yandex.direct.core.testing.steps;

import java.math.BigDecimal;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.BannerPricesPrefix;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPrice;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerPricesPrefix;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerPricesRepository;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.test.utils.differ.BigDecimalDiffer;

import static java.util.Collections.singleton;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;

public class BannerPriceSteps {
    public static final DefaultCompareStrategy PRICE_COMPARE_STRATEGY = DefaultCompareStrategies.allFields()
            .forFields(newPath("price"), newPath("priceOld")).useDiffer(new BigDecimalDiffer());

    @Autowired
    private OldBannerPricesRepository bannerPricesRepository;

    @Nullable
    public OldBannerPrice getBannerPrice(AbstractBannerInfo<?> bannerInfo) {
        return getBannerPrice(bannerInfo.getShard(), bannerInfo.getBannerId());
    }

    @Nullable
    public OldBannerPrice getBannerPrice(int shard, long bannerId) {
        return bannerPricesRepository.getBannerPricesByBannerIds(shard, singleton(bannerId)).get(bannerId);
    }

    public static OldBannerPrice defaultBannerPrice() {
        return new OldBannerPrice()
                .withPrice(BigDecimal.ONE)
                .withPriceOld(BigDecimal.TEN)
                .withCurrency(OldBannerPricesCurrency.RUB)
                .withPrefix(OldBannerPricesPrefix.FROM);
    }

    public static BannerPrice defaultNewBannerPrice() {
        return new BannerPrice()
                .withPrice(BigDecimal.ONE)
                .withPriceOld(BigDecimal.TEN)
                .withCurrency(BannerPricesCurrency.RUB)
                .withPrefix(BannerPricesPrefix.FROM);
    }
}
