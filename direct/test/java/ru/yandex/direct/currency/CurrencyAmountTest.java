package ru.yandex.direct.currency;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Locale;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.currency.currencies.CurrencyYndFixed;
import ru.yandex.direct.i18n.Translator;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.i18n.I18NBundle.makeStubTranslatorFactory;

@RunWith(Parameterized.class)
public class CurrencyAmountTest {
    private static final Currency CURRENCY = CurrencyYndFixed.getInstance();

    private final Translator stubTranslator;

    private final BigDecimal amount;
    private final String expected;

    public CurrencyAmountTest(@SuppressWarnings("unused") String name, BigDecimal amount, String expected) {
        this.amount = amount;
        this.expected = expected;

        stubTranslator = makeStubTranslatorFactory().getTranslator(new Locale.Builder().setLanguageTag("ru").build());
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{"rounding up", BigDecimal.valueOf(12.3456), "12.35 у.е."},
                new Object[]{"rounding down", BigDecimal.valueOf(12.3444), "12.34 у.е."},
                new Object[]{"two digit fraction", BigDecimal.valueOf(12.0), "12.00 у.е."},
                new Object[]{"grouping", BigDecimal.valueOf(1234567.89), "1 234 567.89 у.е."}
        );
    }

    @Test
    public void test() {
        String actual = CurrencyAmount.fromMoney(Money.valueOf(amount, CURRENCY.getCode())).translate(stubTranslator);
        assertThat(actual, Matchers.is(expected));
    }

}
