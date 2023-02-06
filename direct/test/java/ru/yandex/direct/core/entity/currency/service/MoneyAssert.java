package ru.yandex.direct.core.entity.currency.service;

import java.math.BigDecimal;

import org.assertj.core.api.AbstractAssert;

import ru.yandex.direct.currency.CurrencyCode;
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

    @SuppressWarnings("UnusedReturnValue")
    MoneyAssert hasCurrency(CurrencyCode code) {
        isNotNull();

        if (actual.getCurrencyCode() != code) {
            failWithMessage("Expected money currency code to be <%s> but was <%s>", code, actual.getCurrencyCode());
        }

        return this;
    }

    MoneyAssert hasAmount(BigDecimal amount) {
        isNotNull();

        if (actual.bigDecimalValue().compareTo(amount) != 0) {
            failWithMessage("Expected money amount to be <%s> but was <%s>", amount, actual.bigDecimalValue());
        }

        return this;
    }

}
