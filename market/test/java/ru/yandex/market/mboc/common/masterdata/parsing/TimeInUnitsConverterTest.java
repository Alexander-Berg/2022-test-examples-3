package ru.yandex.market.mboc.common.masterdata.parsing;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.masterdata.model.TimeInUnits;
import ru.yandex.market.mboc.common.masterdata.parsing.utils.TimeInUnitsConverter;

/**
 * @author dmserebr
 * @date 30/01/2019
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TimeInUnitsConverterTest {

    private TimeInUnitsConverter timeInUnitsConverter;

    @Test
    public void testParseTimeWithUnits() {
        timeInUnitsConverter = new TimeInUnitsConverter();

        Assertions.assertThat(timeInUnitsConverter.parseTimeInUnits(null)).isNull();
        Assertions.assertThat(timeInUnitsConverter.parseTimeInUnits("")).isNull();

        assertTimeInUnits("1 час", 1, TimeInUnits.TimeUnit.HOUR);
        assertTimeInUnits("2 часа", 2, TimeInUnits.TimeUnit.HOUR);
        assertTimeInUnits("5 часов", 5, TimeInUnits.TimeUnit.HOUR);
        assertTimeInUnits("12 часов", 12, TimeInUnits.TimeUnit.HOUR);

        assertTimeInUnits("1 день", 1, TimeInUnits.TimeUnit.DAY);
        assertTimeInUnits("2 дня", 2, TimeInUnits.TimeUnit.DAY);
        assertTimeInUnits("5 дней", 5, TimeInUnits.TimeUnit.DAY);
        assertTimeInUnits("12 дней", 12, TimeInUnits.TimeUnit.DAY);

        assertTimeInUnits("1 неделя", 1, TimeInUnits.TimeUnit.WEEK);
        assertTimeInUnits("2 недели", 2, TimeInUnits.TimeUnit.WEEK);
        assertTimeInUnits("5 недель", 5, TimeInUnits.TimeUnit.WEEK);
        assertTimeInUnits("12 недель", 12, TimeInUnits.TimeUnit.WEEK);

        assertTimeInUnits("1 месяц", 1, TimeInUnits.TimeUnit.MONTH);
        assertTimeInUnits("2 месяца", 2, TimeInUnits.TimeUnit.MONTH);
        assertTimeInUnits("5 месяцев", 5, TimeInUnits.TimeUnit.MONTH);
        assertTimeInUnits("12 месяцев", 12, TimeInUnits.TimeUnit.MONTH);

        assertTimeInUnits("1 год", 1, TimeInUnits.TimeUnit.YEAR);
        assertTimeInUnits("2 года", 2, TimeInUnits.TimeUnit.YEAR);
        assertTimeInUnits("5 лет", 5, TimeInUnits.TimeUnit.YEAR);
        assertTimeInUnits("12 лет", 12, TimeInUnits.TimeUnit.YEAR);

        assertTimeInUnits("0 дней", 0, TimeInUnits.TimeUnit.DAY);

        assertTimeInUnits(" 3 часа  ", 3, TimeInUnits.TimeUnit.HOUR);
        assertTimeInUnits("246\tмесяцев  ", 246, TimeInUnits.TimeUnit.MONTH);

        assertTimeInUnits(" НЕ ОГРАНИЧЕН ", 1, TimeInUnits.TimeUnit.UNLIMITED);

        Assertions.assertThat(timeInUnitsConverter.parseTimeInUnits(null)).isNull();
    }

    @Test
    public void testParseTimeWithoutUnits() {
        timeInUnitsConverter = new TimeInUnitsConverter();

        assertTimeInUnits("1", 1, TimeInUnits.TimeUnit.DAY);
        assertTimeInUnits("2", 2, TimeInUnits.TimeUnit.DAY);
        assertTimeInUnits("5", 5, TimeInUnits.TimeUnit.DAY);
        assertTimeInUnits("12", 12, TimeInUnits.TimeUnit.DAY);
        assertTimeInUnits("0", 0, TimeInUnits.TimeUnit.DAY);
    }

    @Test
    public void testParseTimeWithUnitsFailed() {
        timeInUnitsConverter = new TimeInUnitsConverter();

        Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> timeInUnitsConverter.parseTimeInUnits("12,7 месяца"))
            .withMessage("Невозможно преобразовать в единицы времени строку [12,7 месяца]");
        Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> timeInUnitsConverter.parseTimeInUnits("2.5 дня"))
            .withMessage("Невозможно преобразовать в единицы времени строку [2.5 дня]");
        Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> timeInUnitsConverter.parseTimeInUnits("-4 неделя"))
            .withMessage("Невозможно преобразовать в единицы времени строку [-4 неделя]");
        Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> timeInUnitsConverter.parseTimeInUnits("неогр."))
            .withMessage("Невозможно преобразовать в единицы времени строку [неогр.]");

        Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> timeInUnitsConverter.parseTimeInUnits("1 годы"))
            .withMessage("Неизвестная единица [годы] в строке [1 годы]");
        Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> timeInUnitsConverter.parseTimeInUnits("6 (лет)"))
            .withMessage("Неизвестная единица [(лет)] в строке [6 (лет)]");
        Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> timeInUnitsConverter.parseTimeInUnits("5 попугаев"))
            .withMessage("Неизвестная единица [попугаев] в строке [5 попугаев]");
    }

    @Test
    public void testParseTimeWithAdditionalUnits() {
        timeInUnitsConverter = new TimeInUnitsConverter(ImmutableMap.of(
            "д", TimeInUnits.TimeUnit.DAY,
            "нед.", TimeInUnits.TimeUnit.WEEK,
            "мес", TimeInUnits.TimeUnit.MONTH,
            "(years)", TimeInUnits.TimeUnit.YEAR
        ));

        assertTimeInUnits("1 д", 1, TimeInUnits.TimeUnit.DAY);
        assertTimeInUnits("12 нед.", 12, TimeInUnits.TimeUnit.WEEK);
        assertTimeInUnits("3 мес", 3, TimeInUnits.TimeUnit.MONTH);
        assertTimeInUnits("5 (years)", 5, TimeInUnits.TimeUnit.YEAR);

        Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> timeInUnitsConverter.parseTimeInUnits("1 д."))
            .withMessage("Неизвестная единица [д.] в строке [1 д.]");
        Assertions.assertThatIllegalArgumentException()
            .isThrownBy(() -> timeInUnitsConverter.parseTimeInUnits("5 years"))
            .withMessage("Неизвестная единица [years] в строке [5 years]");
    }

    private void assertTimeInUnits(String timeInUnitsStr, int time, TimeInUnits.TimeUnit unit) {
        Assertions.assertThat(
            timeInUnitsConverter.parseTimeInUnits(timeInUnitsStr)).isEqualTo(new TimeInUnits(time, unit));
    }
}
