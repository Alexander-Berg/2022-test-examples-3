package ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters;

import org.dozer.ConfigurableCustomConverter;
import ru.yandex.autotests.direct.utils.money.Currency;
import ru.yandex.autotests.direct.utils.money.Money;

import java.math.RoundingMode;

/**
 * Created by shmykov on 23.04.15.
 */
public class PriceConverter implements ConfigurableCustomConverter {

    private String currencyAbbreviation;

    @Override
    public Object convert(Object existingDestinationFieldValue, Object sourceFieldValue, Class<?> destinationClass, Class<?> sourceClass) {
        if (currencyAbbreviation != null) {
            return Money.valueOf((Float) sourceFieldValue).convert(Currency.getFor(currencyAbbreviation)).
                    setScale(0, RoundingMode.FLOOR).toString();
        }
        return null;
    }

    @Override
    public void setParameter(String parameter) {
        currencyAbbreviation = parameter;
    }
}