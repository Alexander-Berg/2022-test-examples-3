package ru.yandex.direct.core.entity.banner.type.price;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.BannerWithPrice;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithPrice;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.priceGreaterThanMax;
import static ru.yandex.direct.core.entity.banner.type.price.BannerWithPriceConstants.MAX_BANNER_PRICE;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithPriceUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithPrice> {

    private static final BigDecimal WRONG_PRICE_MAX = MAX_BANNER_PRICE.add(BigDecimal.ONE);

    @Test
    public void invalidPriceForTextBanner() {
        bannerInfo = steps.bannerSteps().createActiveTextBanner();

        ModelChanges<TextBanner> modelChanges = createModelChanges(bannerInfo.getBannerId(), WRONG_PRICE_MAX);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertValidationHasError(vr,
                path(field(BannerWithPrice.BANNER_PRICE), field(BannerPrice.PRICE)),
                priceGreaterThanMax(MAX_BANNER_PRICE));
    }

    private ModelChanges<TextBanner> createModelChanges(Long bannerId, BigDecimal price) {

        BannerPrice bannerPrice = new BannerPrice()
                .withPrice(price)
                .withCurrency(BannerPricesCurrency.RUB);

        return new ModelChanges<>(bannerId, TextBanner.class)
                .process(bannerPrice, BannerWithPrice.BANNER_PRICE);
    }

    private void assertValidationHasError(ValidationResult<?, Defect> vr, Path path, Defect defect) {
        assertThat(vr, hasDefectDefinitionWith(validationError(path, defect)));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

}
