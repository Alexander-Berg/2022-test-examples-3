package ru.yandex.direct.core.entity.mobilecontent.util;

import org.junit.Test;

import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentExternalWorldMoney;
import ru.yandex.direct.currency.Money;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.mobilecontent.util.MobileContentUtil.getExternalWorldMoney;
import static ru.yandex.direct.core.entity.mobilecontent.util.MobileContentUtil.getMoneyValue;

public class MobileContentUtilTest {
    @Test
    public void testGetMoneyValueReturnsNullOnUnknownCurrency() {
        Money money = getMoneyValue(getExternalWorldMoney("1.123", "CAD"));
        assertThat(money).isNull();
    }

    @Test
    public void testGetMoneyValueReturnsNullOnNullCurrency() {
        //noinspection ConstantConditions
        Money money = getMoneyValue(new MobileContentExternalWorldMoney());
        assertThat(money).isNull();
    }

    @Test
    public void testGetMoneyValueReturnsNullOnNullEwm() {
        //noinspection ConstantConditions
        Money money = getMoneyValue(null);
        assertThat(money).isNull();
    }
}
