package ru.yandex.travel.orders;

import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ECurrency;
import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.commons.proto.ProtoUtils;
import ru.yandex.travel.commons.proto.TPrice;

import static org.assertj.core.api.Assertions.assertThat;

public class ProtoUtilsTest {
    @Test
    public void testProtoTPriceWrapAndUnwrap() {
        Money money = Money.of(67.89, ProtoCurrencyUnit.RUB);
        TPrice wrappedMoney = ProtoUtils.toTPrice(money);

        assertThat(wrappedMoney.getAmount()).isEqualTo(6789);
        assertThat(wrappedMoney.getCurrency()).isEqualTo(ECurrency.C_RUB);
        assertThat(wrappedMoney.getPrecision()).isEqualTo(2);

        Money unwrapped = ProtoUtils.fromTPrice(wrappedMoney);

        assertThat(money).isEqualTo(unwrapped);
    }
}
