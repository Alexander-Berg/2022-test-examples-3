package ru.yandex.direct.core.validation.defects.params;

import java.math.BigDecimal;

import org.junit.Test;

import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class CurrencyAmountDefectParamsTest {

    @Test
    public void check_moneyPriceValue_serialization() {
        BigDecimal srcBigDecimal = BigDecimal.valueOf(22.22d);
        Money moneyValue = Money.valueOf(srcBigDecimal, CurrencyCode.RUB);
        CurrencyAmountDefectParams params = new CurrencyAmountDefectParams(moneyValue);
        String json = JsonUtils.toJson(params);
        assertThat(json).contains("\"moneyPriceValue\":22.22");
    }

}
