package ru.yandex.direct.core.entity.autobroker.service;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.bsauction.BsCpcPrice;
import ru.yandex.direct.core.entity.autobroker.model.AutoBrokerResult;
import ru.yandex.direct.core.entity.keyword.model.Place;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

@RunWith(Parameterized.class)
public class AutoBrokerCalculatorTest {

    private static final CurrencyCode CURRENCY_CODE = CurrencyCode.YND_FIXED;

    private static final int PREMIUM1_PRICE = 780_000;
    private static final int PREMIUM2_PRICE = 710_000;
    private static final int PREMIUM3_PRICE = 610_000;
    private static final int PREMIUM4_PRICE = 510_000;
    private static final BsCpcPrice[] PREMIUM_PRICES = {
            buildBsCpcPrice(PREMIUM1_PRICE),
            buildBsCpcPrice(PREMIUM2_PRICE),
            buildBsCpcPrice(PREMIUM3_PRICE),
            buildBsCpcPrice(PREMIUM4_PRICE),
    };

    private static final int GUARANTEE1_PRICE = 110_000;

    private static final int GUARANTEE2_PRICE = 100_000;
    private static final int GUARANTEE3_PRICE = 40_000;
    private static final int GUARANTEE4_PRICE = 30_000;
    private static final BsCpcPrice[] GUARANTEE_PRICES = {
            buildBsCpcPrice(GUARANTEE1_PRICE),
            buildBsCpcPrice(GUARANTEE2_PRICE),
            buildBsCpcPrice(GUARANTEE3_PRICE),
            buildBsCpcPrice(GUARANTEE4_PRICE),
    };
    private static final int CHEAP_PREMIUM_PRICE = GUARANTEE2_PRICE;
    private static final BsCpcPrice[] PREMIUM_LESS_THAN_GUARANTEE = {
            buildBsCpcPrice(CHEAP_PREMIUM_PRICE),
            buildBsCpcPrice(CHEAP_PREMIUM_PRICE),
            buildBsCpcPrice(CHEAP_PREMIUM_PRICE),
    };

    private static BsCpcPrice buildBsCpcPrice(int priceMicro) {
        return new BsCpcPrice(buildMoney(priceMicro), buildMoney(priceMicro));
    }

    private static Money buildMoney(int priceMicro) {
        return Money.valueOfMicros(priceMicro, CURRENCY_CODE);
    }


    @Parameterized.Parameter
    public String name;

    @Parameterized.Parameter(1)
    public long moneyAmountMicro;

    @Parameterized.Parameter(2)
    public BsCpcPrice[] premium;

    @Parameterized.Parameter(3)
    public BsCpcPrice[] guarantee;

    @Parameterized.Parameter(4)
    public AuctionStrategyMode auctionStrategyMode;

    @Parameterized.Parameter(5)
    public AutoBrokerResult expectedAutoBrokerResult;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] params() {
        return new Object[][]{
                // Стратегия "наивысшая позиция"
                {"Стратегия \"наивысшая позиция\". Хватает на Гарантию-1",
                        GUARANTEE1_PRICE, PREMIUM_PRICES, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_ALL,
                        autoBrokerResult(GUARANTEE1_PRICE, Place.GUARANTEE1, 1.0)},
                {"Стратегия \"наивысшая позиция\". Хватает на Гарантию-2",
                        GUARANTEE2_PRICE, PREMIUM_PRICES, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_ALL,
                        autoBrokerResult(GUARANTEE2_PRICE, Place.GUARANTEE2, 1.0)},
                {"Стратегия \"наивысшая позиция\". Хватает на Гарантию-3",
                        GUARANTEE3_PRICE, PREMIUM_PRICES, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_ALL,
                        autoBrokerResult(GUARANTEE3_PRICE, Place.GUARANTEE3, 1.0)},
                {"Стратегия \"наивысшая позиция\". Хватает на Гарантию-4",
                        GUARANTEE4_PRICE, PREMIUM_PRICES, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_ALL,
                        autoBrokerResult(GUARANTEE4_PRICE, Place.GUARANTEE4, 1.0)},
                {"Стратегия \"наивысшая позиция\". Хватает на Спецразмещение-1",
                        PREMIUM1_PRICE, PREMIUM_PRICES, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_ALL,
                        autoBrokerResult(PREMIUM1_PRICE, Place.PREMIUM1, 1.0)},
                {"Стратегия \"наивысшая позиция\". Хватает на Спецразмещение-2",
                        PREMIUM2_PRICE, PREMIUM_PRICES, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_ALL,
                        autoBrokerResult(PREMIUM2_PRICE, Place.PREMIUM2, 1.0)},
                {"Стратегия \"наивысшая позиция\". Хватает на Спецразмещение-3",
                        PREMIUM3_PRICE, PREMIUM_PRICES, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_ALL,
                        autoBrokerResult(PREMIUM3_PRICE, Place.PREMIUM3, 1.0)},
                {"Стратегия \"наивысшая позиция\". Хватает на Спецразмещение-4",
                        PREMIUM4_PRICE, PREMIUM_PRICES, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_ALL,
                        autoBrokerResult(PREMIUM4_PRICE, Place.PREMIUM4, 1.0)},
                {"Стратегия \"наивысшая позиция\". Гарантия дороже Спецразмещения",
                        GUARANTEE1_PRICE, PREMIUM_LESS_THAN_GUARANTEE, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_ALL,
                        autoBrokerResult(CHEAP_PREMIUM_PRICE, Place.PREMIUM1, 1.0)},

                // Стратегия "Показ в Гарантии на наивысшей позиции"
                {"Стратегия \"Показ в Гарантии на наивысшей позиции\". Хватает ставки на 1-ую позицию в Гарантии",
                        800_000, PREMIUM_PRICES, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_GUARANTEE,
                        autoBrokerResult(110_000, Place.GUARANTEE1, 1.0)},
                {"Стратегия \"Показ в Гарантии на наивысшей позиции\". Хватает на 2-ую позицию в Гарантии",
                        100_000, PREMIUM_PRICES, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_GUARANTEE,
                        autoBrokerResult(30_000, Place.GUARANTEE2, 1.0)},
                {"Стратегия \"Показ в Гарантии на наивысшей позиции\". Хватает на 3-ую позицию в Гарантии",
                        50_000, PREMIUM_PRICES, GUARANTEE_PRICES,
                        AuctionStrategyMode.HIGHEST_POSITION_GUARANTEE,
                        autoBrokerResult(30_000, Place.GUARANTEE3, 1.0)},
        };
    }

    private static AutoBrokerResult autoBrokerResult(long price, Place place, double coverage) {
        return new AutoBrokerResult()
                .withBrokerPrice(Money.valueOfMicros(price, CURRENCY_CODE))
                .withBrokerPlace(place)
                .withBrokerCoverage(coverage);
    }

    @Test
    public void calculatePrices_test() throws Exception {
        Money effectiveRestMoney = Money.valueOf(BigDecimal.ZERO, CURRENCY_CODE);
        AutoBrokerResult actual = AutoBrokerCalculator.calculatePricesInternal(
                auctionStrategyMode, Money.valueOfMicros(moneyAmountMicro, CURRENCY_CODE), null, premium,
                guarantee, 1.0,
                effectiveRestMoney);
        assertThat(actual, beanDiffer(expectedAutoBrokerResult)
                .useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

}
