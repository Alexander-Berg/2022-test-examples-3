package ru.yandex.direct.grid.processing.service.goal;

import java.math.BigDecimal;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.grid.core.entity.model.campaign.GdiGoalCostPerAction;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class GoalDataConverterAdditionalTest {

    private static final CurrencyCode CURRENCY_CODE = CurrencyCode.RUB;
    private static final BigDecimal COST_BOUNDARY = CURRENCY_CODE.getCurrency()
                                                    .getAutobudgetPayForConversionAvgCpaWarningIncreased();
    private static final BigDecimal COST_DEVIATION = new BigDecimal(randomCost());

    @Parameterized.Parameter()
    public BigDecimal goalCostValue;

    @Parameterized.Parameter(1)
    public Boolean hasPriceLimitation;

    @Parameterized.Parameter(2)
    public BigDecimal expectedCost;

    @Parameterized.Parameter(3)
    public String description;

    @Parameterized.Parameters(name = "{3}")
    public static Collection testData() {
        Object[][] data = new Object[][]{
                {
                        COST_BOUNDARY,
                        true,
                        COST_BOUNDARY,
                        "Цена равна ценовому ограничению и не должна быть ограничена, ограничение цены: ON"
                },
                {
                        COST_BOUNDARY,
                        false,
                        COST_BOUNDARY,
                        "Цена равна ценовому ограничению и не должна быть ограничена, ограничение цены: OFF"
                },
                {
                        COST_BOUNDARY.add(new BigDecimal(1)),
                        true,
                        COST_BOUNDARY,
                        "Цена чуть больше ценового ограничения и должна быть ограничена, ограничение цены: ON"
                },
                {
                        COST_BOUNDARY.subtract(new BigDecimal(1)),
                        true,
                        COST_BOUNDARY.subtract(new BigDecimal(1)),
                        "Цена чуть меньше ценового ограничения и не должна быть ограничена, ограничение цены: ON"
                },
                {
                        COST_BOUNDARY.add(COST_DEVIATION),
                        true,
                        COST_BOUNDARY,
                        "Цена больше ценового ограничения и должна быть ограничена, ограничение цены: ON"
                },
                {
                        COST_BOUNDARY.add(COST_DEVIATION),
                        false,
                        COST_BOUNDARY.add(COST_DEVIATION),
                        "Цена больше ценового ограничения и не должна быть ограничена, ограничение цены: OFF"
                },
                {
                        COST_BOUNDARY.subtract(COST_DEVIATION),
                        true,
                        COST_BOUNDARY.subtract(COST_DEVIATION),
                        "Цена меньше ценового ограничения и не должна быть ограничена, ограничение цены: ON"
                },
                {
                        COST_BOUNDARY.subtract(COST_DEVIATION),
                        false,
                        COST_BOUNDARY.subtract(COST_DEVIATION),
                        "Цена меньше ценового ограничения и не должна быть ограничена, ограничение цены: OFF"
                }
        };
        return asList(data);
    }


    @Test
    public void toGdRecommendedGoalCostPerAction_limitationOfCostByMaximum() {
        GdiGoalCostPerAction goalCost = new GdiGoalCostPerAction()
                .withGoalId(RandomNumberUtils.nextPositiveLong())
                .withCostPerAction(goalCostValue);

        BigDecimal actualCost = GoalDataConverter.toGdRecommendedGoalCostPerAction(
                goalCost,
                CURRENCY_CODE,
                hasPriceLimitation).getCostPerAction();

        assertThat(actualCost).isEqualTo(expectedCost);
    }

    private static Integer randomCost() {
        return (RandomNumberUtils.nextPositiveInteger(20) + 1) * 1000;
    }
}
