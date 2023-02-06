package ru.yandex.direct.core.entity.bids.validation;

import java.math.BigDecimal;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.validation.defects.params.CurrencyAmountDefectParams;
import ru.yandex.direct.currency.Currency;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectId;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.currency.CurrencyCode.EUR;
import static ru.yandex.direct.currency.CurrencyCode.RUB;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

@RunWith(Parameterized.class)
public class PriceValidatorTest {

    @Parameterized.Parameters(name = "валюта \"{0}\", тип: \"{1}\" цена: \"{2}\", дефект: \"{3}\", ограничение: \"{4}\"")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{

                {RUB, AdGroupType.BASE, BigDecimal.valueOf(0.3), null, null},

                {RUB, AdGroupType.BASE, BigDecimal.valueOf(0),
                        BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                        RUB.getCurrency().getMinPrice()},

                {RUB, AdGroupType.BASE, BigDecimal.valueOf(0.29),
                        BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                        RUB.getCurrency().getMinPrice()},

                {RUB, AdGroupType.BASE, BigDecimal.valueOf(25001),
                        BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX,
                        RUB.getCurrency().getMaxPrice()},

                {RUB, AdGroupType.MOBILE_CONTENT, BigDecimal.valueOf(0.29),
                        BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                        RUB.getCurrency().getMinPrice()},

                {RUB, AdGroupType.MOBILE_CONTENT, BigDecimal.valueOf(25001),
                        BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX,
                        RUB.getCurrency().getMaxPrice()},

                {RUB, AdGroupType.CPM_BANNER, BigDecimal.valueOf(4.9),
                        BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_GREATER_THAN_MIN,
                        RUB.getCurrency().getMinCpmPrice()},

                {RUB, AdGroupType.CPM_BANNER, BigDecimal.valueOf(3001),
                        BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_SMALLER_THAN_MAX,
                        RUB.getCurrency().getMaxCpmPrice()},

                {RUB, AdGroupType.PERFORMANCE, BigDecimal.valueOf(0.29),
                        BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                        RUB.getCurrency().getMinCpcCpaPerformance()},

                {RUB, AdGroupType.PERFORMANCE, BigDecimal.valueOf(25000.1),
                        BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX,
                        RUB.getCurrency().getMaxPrice()},

                {RUB, AdGroupType.CPM_GEOPRODUCT, BigDecimal.valueOf(4.9),
                        BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_GREATER_THAN_MIN,
                        RUB.getCurrency().getMinCpmPrice()},

                {RUB, AdGroupType.CPM_GEOPRODUCT, BigDecimal.valueOf(10.9),
                        null, null},

                {RUB, AdGroupType.CPM_GEOPRODUCT, BigDecimal.valueOf(3001),
                        BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_SMALLER_THAN_MAX,
                        RUB.getCurrency().getMaxCpmPrice()},

                {RUB, AdGroupType.CPM_VIDEO, BigDecimal.valueOf(4.9),
                        BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_GREATER_THAN_MIN,
                        RUB.getCurrency().getMinCpmPrice()},

                {RUB, AdGroupType.CPM_VIDEO, BigDecimal.valueOf(3001),
                        BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_SMALLER_THAN_MAX,
                        RUB.getCurrency().getMaxCpmPrice()},

                {RUB, AdGroupType.CPM_INDOOR, BigDecimal.valueOf(4.9),
                        BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_GREATER_THAN_MIN,
                        RUB.getCurrency().getMinCpmPrice()},

                {RUB, AdGroupType.CPM_INDOOR, BigDecimal.valueOf(3001),
                        BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_SMALLER_THAN_MAX,
                        RUB.getCurrency().getMaxCpmPrice()},

                {RUB, AdGroupType.CPM_OUTDOOR, BigDecimal.valueOf(4.9),
                        BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_GREATER_THAN_MIN,
                        RUB.getCurrency().getMinCpmPrice()},

                {RUB, AdGroupType.CPM_OUTDOOR, BigDecimal.valueOf(3001),
                        BidsDefects.CurrencyAmountDefects.CPM_PRICE_IS_NOT_SMALLER_THAN_MAX,
                        RUB.getCurrency().getMaxCpmPrice()},

                {EUR, AdGroupType.BASE, BigDecimal.valueOf(0),
                        BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_GREATER_THAN_MIN,
                        EUR.getCurrency().getMinPrice()},

                {EUR, AdGroupType.BASE, BigDecimal.valueOf(501),
                        BidsDefects.CurrencyAmountDefects.SEARCH_PRICE_IS_NOT_SMALLER_THAN_MAX,
                        EUR.getCurrency().getMaxPrice()}

        });
    }

    @Parameterized.Parameter(0)
    public CurrencyCode currencyCode;

    @Parameterized.Parameter(1)
    public AdGroupType type;

    @Parameterized.Parameter(2)
    public BigDecimal price;

    @Parameterized.Parameter(3)
    public DefectId<CurrencyAmountDefectParams> expectedDefectId;

    @Parameterized.Parameter(4)
    public BigDecimal limitPrice;

    @Test
    public void autoBudgetPriorityIsAccepted_validValue() {
        Currency currency = currencyCode.getCurrency();

        if (expectedDefectId != null) {
            Defect expectedDefect = new Defect<>(expectedDefectId,
                    new CurrencyAmountDefectParams(Money.valueOf(limitPrice, currency.getCode())));

            assertThat(new PriceValidator(currency, type).apply(price),
                    hasDefectDefinitionWith(validationError(path(), expectedDefect)));
        } else {
            assertThat(new PriceValidator(currency, type).apply(price),
                    hasNoDefectsDefinitions());
        }
    }
}
