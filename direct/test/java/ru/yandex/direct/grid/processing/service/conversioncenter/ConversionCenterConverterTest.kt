package ru.yandex.direct.grid.processing.service.conversioncenter

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.core.entity.conversionsource.model.ConversionActionValue.Fixed
import ru.yandex.direct.core.entity.conversionsource.model.MetrikaGoalSelection
import ru.yandex.direct.currency.CurrencyCode
import ru.yandex.direct.currency.Money
import ru.yandex.direct.grid.processing.model.goal.GdConversionActionValueFixed
import ru.yandex.direct.grid.processing.model.goal.GdConversionActionValueType
import ru.yandex.direct.grid.processing.model.goal.GdMetrikaGoalSelection
import java.math.BigDecimal

private const val COUNTER_ID = 2323422L
private const val GOAL_ID = 53422002L

class ConversionCenterConverterTest {
    lateinit var conversionCenterConverter: ConversionCenterConverter

    @Before
    fun before() {
        conversionCenterConverter = ConversionCenterConverter(mock(), mock(), mock())
    }

    @Test
    fun fromGdMetrikaGoalSelection_NullValue() {
        val selection = GdMetrikaGoalSelection()
            .withMetrikaCounterId(COUNTER_ID)
            .withGoalId(GOAL_ID)
            .withValue(null)
            .withIsSelected(false)
        val result = conversionCenterConverter.fromGdMetrikaGoalSelection(listOf(selection))
        assertThat(result).contains(MetrikaGoalSelection(COUNTER_ID, GOAL_ID, null, false))
    }

    @Test
    fun fromGdMetrikaGoalSelection_FixedValue() {
        val selection = GdMetrikaGoalSelection()
            .withMetrikaCounterId(COUNTER_ID)
            .withGoalId(GOAL_ID)
            .withValue(
                GdConversionActionValueFixed()
                    .withType(GdConversionActionValueType.FIXED)
                    .withCost(BigDecimal.valueOf(33))
                    .withCurrency(CurrencyCode.RUB)
            )
            .withIsSelected(true)
        val result = conversionCenterConverter.fromGdMetrikaGoalSelection(listOf(selection))
        assertThat(result).contains(
            MetrikaGoalSelection(COUNTER_ID, GOAL_ID, Fixed(Money.valueOf("33", CurrencyCode.RUB)), true)
        )
    }
}
