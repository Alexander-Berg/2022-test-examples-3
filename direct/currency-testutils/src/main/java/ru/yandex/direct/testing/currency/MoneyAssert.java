package ru.yandex.direct.testing.currency;

import org.assertj.core.api.AbstractAssert;

import ru.yandex.direct.currency.Money;

/**
 * Для удобной проверки значений {@link Money} в тестах
 */
public class MoneyAssert extends AbstractAssert<MoneyAssert, Money> {

    private MoneyAssert(Money actual) {
        super(actual, MoneyAssert.class);
    }

    public static MoneyAssert assertThat(Money actual) {
        return new MoneyAssert(actual);
    }

    public void isEqualTo(Money expected) {
        isNotNull();

        if (actual.bigDecimalValue().compareTo(expected.bigDecimalValue()) != 0
                || actual.getCurrencyCode() != expected.getCurrencyCode()) {
            failWithMessage("Expected money to be <%s> but was <%s>", expected, actual);
        }
    }

}
