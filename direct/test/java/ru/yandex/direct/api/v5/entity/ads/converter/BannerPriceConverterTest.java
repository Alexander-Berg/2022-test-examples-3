package ru.yandex.direct.api.v5.entity.ads.converter;

import java.math.BigDecimal;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.ads.PriceCurrencyEnum;
import com.yandex.direct.api.v5.ads.PriceExtensionAddItem;
import com.yandex.direct.api.v5.ads.PriceExtensionGetItem;
import com.yandex.direct.api.v5.ads.PriceExtensionUpdateItem;
import com.yandex.direct.api.v5.ads.PriceQualifierEnum;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.assertj.core.util.BigDecimalComparator;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.common.ConverterUtils.convertToMicros;
import static ru.yandex.direct.core.testing.steps.BannerPriceSteps.defaultNewBannerPrice;

@RunWith(JUnitParamsRunner.class)
public class BannerPriceConverterTest {
    private static final ObjectFactory FACTORY = new ObjectFactory();

    private static final RecursiveComparisonConfiguration COMPARE_STRATEGY = RecursiveComparisonConfiguration.builder()
            .withComparatorForType(BigDecimalComparator.BIG_DECIMAL_COMPARATOR, BigDecimal.class).build();

    @Test
    public void convertAddRequest_allFields() {
        var expected = defaultNewBannerPrice();
        PriceExtensionAddItem item = new PriceExtensionAddItem()
                .withPrice(convertToMicros(expected.getPrice()))
                .withOldPrice(convertToMicros(expected.getPriceOld()))
                .withPriceCurrency(BannerPriceConverter.fromCore(expected.getCurrency()))
                .withPriceQualifier(BannerPriceConverter.fromCore(expected.getPrefix()));
        assertThat(BannerPriceConverter.toCore(item))
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expected);
    }

    @Test
    public void convertAddRequest_nullPrefix() {
        var expected = defaultNewBannerPrice().withPrefix(null);
        PriceExtensionAddItem item = new PriceExtensionAddItem()
                .withPrice(convertToMicros(expected.getPrice()))
                .withOldPrice(convertToMicros(expected.getPriceOld()))
                .withPriceCurrency(BannerPriceConverter.fromCore(expected.getCurrency()))
                .withPriceQualifier(PriceQualifierEnum.NONE);
        assertThat(BannerPriceConverter.toCore(item))
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expected);
    }

    @Test
    @Parameters(source = BannerPricesCurrency.class)
    public void convertAddRequest_allCurrencies(BannerPricesCurrency currency) {
        var expected = defaultNewBannerPrice().withCurrency(currency);
        PriceExtensionAddItem item = new PriceExtensionAddItem()
                .withPrice(convertToMicros(expected.getPrice()))
                .withOldPrice(convertToMicros(expected.getPriceOld()))
                .withPriceCurrency(BannerPriceConverter.fromCore(expected.getCurrency()))
                .withPriceQualifier(BannerPriceConverter.fromCore(expected.getPrefix()));
        assertThat(BannerPriceConverter.toCore(item))
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expected);
    }

    @Test
    public void convertUpdateRequest_RemovePrice_ReturnNull() {
        assertThat(BannerPriceConverter.toCore(null, new BannerPrice())).isNull();
    }

    @Test
    public void convertUpdateRequest_WasEmpty_ReturnPopulatedModel() {
        var expected = defaultNewBannerPrice();
        PriceExtensionUpdateItem item = new PriceExtensionUpdateItem()
                .withPrice(convertToMicros(expected.getPrice()))
                .withOldPrice(FACTORY.createPriceExtensionGetItemOldPrice(convertToMicros(expected.getPriceOld())))
                .withPriceCurrency(BannerPriceConverter.fromCore(expected.getCurrency()))
                .withPriceQualifier(BannerPriceConverter.fromCore(expected.getPrefix()));
        assertThat(BannerPriceConverter.toCore(item, null))
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expected);
    }

    @Test
    public void convertUpdateRequest_NewPriceOnly() {
        var expected = defaultNewBannerPrice().withPrice(BigDecimal.ZERO);
        PriceExtensionUpdateItem item = new PriceExtensionUpdateItem()
                .withPrice(convertToMicros(expected.getPrice()));
        assertThat(BannerPriceConverter.toCore(item, defaultNewBannerPrice()))
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expected);
    }

    @Test
    public void convertUpdateRequest_NewOldPriceOnly() {
        var expected = defaultNewBannerPrice().withPriceOld(BigDecimal.ZERO);
        PriceExtensionUpdateItem item = new PriceExtensionUpdateItem()
                .withOldPrice(FACTORY.createPriceExtensionGetItemOldPrice(convertToMicros(expected.getPriceOld())));
        assertThat(BannerPriceConverter.toCore(item, defaultNewBannerPrice()))
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expected);
    }

    @Test
    public void convertUpdateRequest_OldPriceRemovedOnly() {
        var expected = defaultNewBannerPrice().withPriceOld(null);
        JAXBElement<Long> oldPrice = FACTORY.createPriceExtensionGetItemOldPrice(0L);
        oldPrice.setNil(true);
        PriceExtensionUpdateItem item = new PriceExtensionUpdateItem().withOldPrice(oldPrice);
        assertThat(BannerPriceConverter.toCore(item, defaultNewBannerPrice()))
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expected);
    }

    @Test
    @Parameters(source = PriceQualifierEnum.class)
    public void convertUpdateRequest_PrefixOnly(PriceQualifierEnum qualifier) {
        var expected = defaultNewBannerPrice().withPrefix(BannerPriceConverter.toCore(qualifier));
        PriceExtensionUpdateItem item = new PriceExtensionUpdateItem()
                .withPriceQualifier(qualifier);
        assertThat(BannerPriceConverter.toCore(item, defaultNewBannerPrice()))
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expected);
    }

    @Test
    @Parameters(source = PriceCurrencyEnum.class)
    public void convertUpdateRequest_CurrencyOnly(PriceCurrencyEnum currency) {
        var expected = defaultNewBannerPrice().withCurrency(BannerPriceConverter.toCore(currency));
        PriceExtensionUpdateItem item = new PriceExtensionUpdateItem()
                .withPriceCurrency(currency);
        assertThat(BannerPriceConverter.toCore(item, defaultNewBannerPrice()))
                .usingRecursiveComparison(COMPARE_STRATEGY)
                .isEqualTo(expected);
    }

    @Test
    public void convertGetFromCore() {
        var price = defaultNewBannerPrice();
        PriceExtensionGetItem expected = new PriceExtensionGetItem()
                .withPrice(convertToMicros(price.getPrice()))
                .withOldPrice(FACTORY.createPriceExtensionGetItemOldPrice(convertToMicros(price.getPriceOld())))
                .withPriceCurrency(BannerPriceConverter.fromCore(price.getCurrency()))
                .withPriceQualifier(BannerPriceConverter.fromCore(price.getPrefix()));
        assertThat(BannerPriceConverter.fromCore(price))
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void convertGetFromCore_nullPrefix() {
        var price = defaultNewBannerPrice().withPrefix(null);
        PriceExtensionGetItem expected = new PriceExtensionGetItem()
                .withPrice(convertToMicros(price.getPrice()))
                .withOldPrice(FACTORY.createPriceExtensionGetItemOldPrice(convertToMicros(price.getPriceOld())))
                .withPriceCurrency(BannerPriceConverter.fromCore(price.getCurrency()))
                .withPriceQualifier(PriceQualifierEnum.NONE);
        assertThat(BannerPriceConverter.fromCore(price))
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    public void convertGetFromCore_nullReturnsNull() {
        assertThat(BannerPriceConverter.fromCore((BannerPrice) null)).isNull();
    }
}
