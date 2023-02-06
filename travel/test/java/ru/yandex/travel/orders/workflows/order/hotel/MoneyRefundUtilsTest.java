package ru.yandex.travel.orders.workflows.order.hotel;

import org.junit.Test;

import ru.yandex.travel.orders.entities.MoneyMarkup;
import ru.yandex.travel.orders.workflow.invoice.proto.TMoneyMarkup;
import ru.yandex.travel.orders.workflow.order.proto.TStartMoneyOnlyRefund;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.commons.proto.ProtoUtils.toTPrice;
import static ru.yandex.travel.orders.TestOrderObjects.moneyMarkup;
import static ru.yandex.travel.testing.misc.TestBaseObjects.rub;

public class MoneyRefundUtilsTest {
    @Test
    public void fromTMoneyMarkup() {
        TStartMoneyOnlyRefund event = TStartMoneyOnlyRefund.newBuilder().build();

        assertThat(event.hasNewInvoiceAmountMarkup()).isFalse();
        MoneyMarkup markup = MoneyRefundUtils.fromTMoneyMarkup(event.getNewInvoiceAmountMarkup());
        assertThat(markup).isNull();

        event = event.toBuilder()
                // convenient but not quite a proper way to pass the value
                .setNewInvoiceAmountMarkup(event.getNewInvoiceAmountMarkup())
                .build();
        assertThat(event.hasNewInvoiceAmountMarkup()).isTrue();
        markup = MoneyRefundUtils.fromTMoneyMarkup(event.getNewInvoiceAmountMarkup());
        assertThat(markup).isNull();

        event = event.toBuilder()
                .setNewInvoiceAmountMarkup(TMoneyMarkup.newBuilder()
                        .setCard(toTPrice(rub(100)))
                        .setYandexAccount(toTPrice(rub(10)))
                        .build())
                .build();
        assertThat(event.hasNewInvoiceAmountMarkup()).isTrue();
        markup = MoneyRefundUtils.fromTMoneyMarkup(event.getNewInvoiceAmountMarkup());
        assertThat(markup).isEqualTo(moneyMarkup(100, 10));
    }
}
