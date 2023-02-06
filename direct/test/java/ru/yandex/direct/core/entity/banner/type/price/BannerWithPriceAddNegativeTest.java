package ru.yandex.direct.core.entity.banner.type.price;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.BannerWithPrice;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.math.BigDecimal.ZERO;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.currencyMissing;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.priceGreaterThanMax;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.priceGreaterThanOld;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.priceLessThanMin;
import static ru.yandex.direct.core.entity.banner.type.price.BannerWithPriceConstants.MAX_BANNER_PRICE;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithPriceAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final BigDecimal WRONG_PRICE_MAX = MAX_BANNER_PRICE.add(BigDecimal.ONE);
    private static final BigDecimal WRONG_PRICE_MIN = BigDecimal.ZERO.subtract(BigDecimal.ONE);

    @Test
    public void invalidPriceForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        BannerPrice bannerPrice = new BannerPrice()
                .withPrice(WRONG_PRICE_MAX)
                .withCurrency(BannerPricesCurrency.RUB);

        TextBanner banner = createTextBanner(bannerPrice);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertValidationHasError(vr,
                path(field(BannerWithPrice.BANNER_PRICE), field(BannerPrice.PRICE)),
                priceGreaterThanMax(MAX_BANNER_PRICE));
    }

    @Test
    public void invalidPriceOldForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        BannerPrice bannerPrice = new BannerPrice()
                .withPrice(BigDecimal.TEN)
                .withPriceOld(WRONG_PRICE_MIN)
                .withCurrency(BannerPricesCurrency.RUB);

        TextBanner banner = createTextBanner(bannerPrice);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertValidationHasError(vr,
                path(field(BannerWithPrice.BANNER_PRICE), field(BannerPrice.PRICE_OLD)),
                priceLessThanMin(ZERO));
    }

    @Test
    public void invalidPriceCurrencyForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        BannerPrice bannerPrice = new BannerPrice()
                .withPrice(BigDecimal.TEN)
                .withPriceOld(BigDecimal.TEN)
                .withCurrency(null);

        TextBanner banner = createTextBanner(bannerPrice);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertValidationHasError(vr,
                path(field(BannerWithPrice.BANNER_PRICE), field(BannerPrice.CURRENCY)),
                currencyMissing());
    }

    @Test
    public void priceGreaterThanOldPriceForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        BannerPrice bannerPrice = new BannerPrice()
                .withPrice(BigDecimal.TEN)
                .withPriceOld(BigDecimal.ONE)
                .withCurrency(BannerPricesCurrency.RUB);

        TextBanner banner = createTextBanner(bannerPrice);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertValidationHasError(vr,
                path(field(BannerWithPrice.BANNER_PRICE)),
                priceGreaterThanOld());
    }

    private void assertValidationHasError(ValidationResult<?, Defect> vr, Path path, Defect defect) {
        assertThat(vr, hasDefectDefinitionWith(validationError(path, defect)));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    private TextBanner createTextBanner(BannerPrice bannerPrice) {
        return clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withBannerPrice(bannerPrice);
    }

}
