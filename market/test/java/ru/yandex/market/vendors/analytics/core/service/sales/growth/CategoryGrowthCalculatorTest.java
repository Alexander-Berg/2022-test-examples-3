package ru.yandex.market.vendors.analytics.core.service.sales.growth;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.vendors.analytics.core.model.common.MoneyCountPair;
import ru.yandex.market.vendors.analytics.core.model.sales.growth.Growth;
import ru.yandex.market.vendors.analytics.core.model.sales.growth.SalesChangeInfo;
import ru.yandex.market.vendors.analytics.core.model.sales.growth.category.CategoryGrowthExtendedInfo;
import ru.yandex.market.vendors.analytics.core.model.sales.growth.category.GrowthWaterfallInfo;
import ru.yandex.market.vendors.analytics.core.model.sales.growth.model.ModelSalesChange;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author antipov93.
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
public class CategoryGrowthCalculatorTest {

    private static final double EPSILON = 0.01;

    private CategoryGrowthCalculator categoryGrowthCalculator = new CategoryGrowthCalculator();

    private static Stream<Arguments> calcWaterfallGrowthArguments() {
        return Stream.of(
                Arguments.of(100L, 10L, 108L, 12L, true, -8.0, 16.0),
                Arguments.of(0L, 0L, 100L, 10L, false, 0.0, 0.0),
                Arguments.of(100L, 10L, 0L, 0L, false, 0.0, 0.0)
        );
    }

    @ParameterizedTest
    @MethodSource("calcWaterfallGrowthArguments")
    @DisplayName("Расчёт вклада изменений объёма продаж и средней цены на рост категории")
    void calcWaterfallGrowth(long baseMoney, long baseCount, long comparingMoney, long comparingCount,
                             boolean present, double priceContribution, double volumeContribution) {
        var baseSales = MoneyCountPair.builder()
                .money(baseMoney)
                .count(baseCount)
                .build();
        var comparingSales = MoneyCountPair.builder()
                .money(comparingMoney)
                .count(comparingCount)
                .build();
        var info = new SalesChangeInfo(baseSales, comparingSales);

        var expected = GrowthWaterfallInfo.builder()
                .moneyBasePeriod(baseMoney)
                .countBasePeriod(baseCount)
                .moneyComparingPeriod(comparingMoney)
                .countComparingPeriod(comparingCount)
                .priceContribution(priceContribution)
                .volumeContribution(volumeContribution)
                .build();

        Optional<GrowthWaterfallInfo> actualOpt = categoryGrowthCalculator.calcWaterfallGrowth(info);
        assertEquals(present, actualOpt.isPresent());
        if (present) {
            assertEquals(expected, actualOpt.get());
        }
    }

    private static Stream<Arguments> calcCategoryGrowthExtendedArguments() {
        return Stream.of(
                Arguments.of(
                        List.of(
                                buildModelSalesChange(1, 1, 10000, 100, 21000, 200),
                                buildModelSalesChange(2, 1, 7000, 5, 6000, 3),
                                buildModelSalesChange(3, 1, 15000, 60, 12000, 50),
                                buildModelSalesChange(4, 2, 180000, 1000, 200000, 1250),
                                buildModelSalesChange(5, 2, 30000, 600, 28000, 700),
                                buildModelSalesChange(6, 3, 1200, 3, 1300, 4)
                        ),
                        true,
                        10.32, 24.83, -11.62, -9.29, 0.83, -3.37
                ),
                Arguments.of(
                        Collections.emptyList(),
                        false,
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0
                ),
                Arguments.of(
                        List.of(buildModelSalesChange(1, 1, 100, 100, 0, 0)),
                        false,
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0
                ),
                Arguments.of(
                        List.of(buildModelSalesChange(1, 1, 0, 0, 100, 100)),
                        false,
                        0.0, 0.0, 0.0, 0.0, 0.0, 0.0
                ),
                Arguments.of(
                        List.of(
                                buildModelSalesChange(1, 1, 100, 100, 0, 0),
                                buildModelSalesChange(2, 2, 0, 0, 300, 150)
                        ),
                        true,
                        200.0, 50.0, 100.0, 0.0, 100.0, 0.0
                )
        );
    }

    @ParameterizedTest
    @MethodSource("calcCategoryGrowthExtendedArguments")
    @DisplayName("Расчёт дополнительной информации по росту категории")
    void calcCategoryGrowthExtendedInfo(List<ModelSalesChange> modelSalesChanges, boolean present,
                                        double moneyDiff, double countDiff, double priceDiff,
                                        double modelPriceDiff, double brandTransition, double modelTransition
    ) {
        Optional<CategoryGrowthExtendedInfo> actualOpt = categoryGrowthCalculator
                .calcGrowthExtendedInfo(modelSalesChanges);
        assertEquals(present, actualOpt.isPresent());
        if (!present) {
            return;
        }

        CategoryGrowthExtendedInfo actual = actualOpt.get();
        Growth actualGrowth = actual.getPercentGrowth();
        assertEqualsDouble(moneyDiff, actualGrowth.getMoneyDiff());
        assertEqualsDouble(countDiff, actualGrowth.getCountDiff());
        assertEqualsDouble(priceDiff, actualGrowth.getPriceDiff());

        assertEqualsDouble(modelPriceDiff, actual.getModelPriceDiff());
        assertEqualsDouble(brandTransition, actual.getBrandTransition());
        assertEqualsDouble(modelTransition, actual.getModelTransition());
    }

    public static ModelSalesChange buildModelSalesChange(
            long modelId, long brandId, long baseMoney, long baseCount, long comparingMoney, long comparingCount
    ) {
        var baseSales = MoneyCountPair.builder()
                .money(baseMoney)
                .count(baseCount)
                .build();
        var comparingSales = MoneyCountPair.builder()
                .money(comparingMoney)
                .count(comparingCount)
                .build();

        var salesChange = new SalesChangeInfo(baseSales, comparingSales);
        return ModelSalesChange.builder()
                .modelId(modelId)
                .brandId(brandId)
                .salesChangeInfo(salesChange)
                .build();
    }

    private static void assertEqualsDouble(double first, double second) {
        assertEquals(first, second, EPSILON);
    }
}
