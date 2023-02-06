package ru.yandex.travel.orders.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.workflow.invoice.proto.ETrustInvoiceState;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.travel.orders.TestOrderObjects.moneyMarkup;
import static ru.yandex.travel.testing.misc.TestBaseObjects.uuid;

public class MoneyRefundTest {
    @Test
    public void pendingRefund() {
        Order order = testOrder(
                testInvoice(uuid(1), "2021-08-10", testInvoiceItem(100, 1), testInvoiceItem(101, 1)),
                testInvoice(uuid(2), "2021-08-11", testInvoiceItem(101, 1000), testInvoiceItem(102, 900)),
                testInvoice(uuid(3), "2021-08-12", testInvoiceItem(102, 100), testInvoiceItem(103, 1000)),
                testInvoice(uuid(4), "2021-08-13", testInvoiceItem(104, 1))
        );
        Map<Long, Money> targetFiscalItems = Map.of(
                101L, Money.of(100, "RUB"),
                102L, Money.of(200, "RUB"),
                103L, Money.of(300, "RUB")
        );

        MoneyRefund refund = MoneyRefund.createPendingRefund(order, targetFiscalItems, null, null, null);

        assertThat(refund.getState()).isEqualTo(MoneyRefundState.PENDING);
        assertThat(refund.getTargetFiscalItems()).isEqualTo(targetFiscalItems);

        Map<UUID, MoneyRefundContext.InvoiceRefund> invoiceRefunds = refund.getContext().getInvoicesToRefund();
        assertThat(invoiceRefunds).containsOnlyKeys(uuid(2), uuid(3));
        assertThat(invoiceRefunds.get(uuid(2)).getTargetPricesByFiscalItem())
                .isEqualTo(Map.of(101L, Money.of(99, "RUB"),
                        102L, Money.of(200, "RUB")));
        assertThat(invoiceRefunds.get(uuid(3)).getTargetPricesByFiscalItem())
                .isEqualTo(Map.of(102L, Money.of(0, "RUB"),
                        103L, Money.of(300, "RUB")));
        // the final fiscal items will be (item - rub): 100 - 1, 101 - 100, 102 - 200, 103 - 300, 104 - 1

        assertThat(invoiceRefunds).allSatisfy((fiscalItemId, invoiceRefund) ->
                assertThat(invoiceRefund.getTargetPricesByFiscalItemMarkup()).isEmpty());
    }

    @Test
    public void pendingRefund_skipAlreadyRefundedInvoiceItem() {
        Order order = testOrder(
                testInvoice(uuid(1), "2021-08-10", testInvoiceItem(100, 100)),
                testInvoice(uuid(2), "2021-08-11", testInvoiceItem(100, 0))
        );
        Map<Long, Money> targetFiscalItems = Map.of(
                100L, Money.of(50, "RUB")
        );

        MoneyRefund refund = MoneyRefund.createPendingRefund(order, targetFiscalItems, null, null, null);

        Map<UUID, MoneyRefundContext.InvoiceRefund> invoiceRefunds = refund.getContext().getInvoicesToRefund();
        assertThat(invoiceRefunds).containsOnlyKeys(uuid(1));
        assertThat(invoiceRefunds.get(uuid(1)).getTargetPricesByFiscalItem())
                .isEqualTo(Map.of(100L, Money.of(50, "RUB")));
    }

    @Test
    public void pendingRefund_skipZeroInvoiceItemRefund() {
        Order order = testOrder(
                testInvoice(uuid(1), "2021-08-10", testInvoiceItem(100, 50)),
                testInvoice(uuid(2), "2021-08-11", testInvoiceItem(100, 50))
        );
        Map<Long, Money> targetFiscalItems = Map.of(
                100L, Money.of(100, "RUB")
        );

        MoneyRefund refund = MoneyRefund.createPendingRefund(order, targetFiscalItems, null, null, null);

        Map<UUID, MoneyRefundContext.InvoiceRefund> invoiceRefunds = refund.getContext().getInvoicesToRefund();
        assertThat(invoiceRefunds).isEmpty();
    }

    @Test
    public void pendingRefund_moneyMarkup() {
        // all items have total value of 1000 RUB with 500 roubles included as yandex plus points
        Order order = testOrder(
                // item params: total price + used plus points (included in the total)
                testInvoice(uuid(1), "2021-08-10", testInvoiceItem(100, 1000, 500), testInvoiceItem(101, 1, 0)),
                testInvoice(uuid(2), "2021-08-11", testInvoiceItem(101, 999, 500), testInvoiceItem(102, 900, 450)),
                testInvoice(uuid(3), "2021-08-12", testInvoiceItem(102, 100, 50), testInvoiceItem(103, 1000, 500)),
                testInvoice(uuid(4), "2021-08-13", testInvoiceItem(104, 1000, 500))
        );
        // refunding items: 101 for 900 "rub" (), 102 for 800 "rub' and 103 for 700 "rub"
        Map<Long, Money> targetFiscalItems = Map.of(
                101L, Money.of(100, "RUB"),
                102L, Money.of(200, "RUB"),
                103L, Money.of(300, "RUB")
        );
        Map<Long, MoneyMarkup> targetFiscalItemsMarkup = Map.of(
                101L, moneyMarkup(80, 20),  // refunding 420 roubles + 480 points
                102L, moneyMarkup(200, 0),  // refunding 300 roubles + 500 points
                103L, moneyMarkup(0, 300)   // refunding 500 roubles + 200 points
        );

        MoneyRefund refund = MoneyRefund.createPendingRefund(order, targetFiscalItems,
                targetFiscalItemsMarkup, null, null);

        assertThat(refund.getState()).isEqualTo(MoneyRefundState.PENDING);
        assertThat(refund.getTargetFiscalItems()).isEqualTo(targetFiscalItems);

        Map<UUID, MoneyRefundContext.InvoiceRefund> invoiceRefunds = refund.getContext().getInvoicesToRefund();
        assertThat(invoiceRefunds).containsOnlyKeys(uuid(2), uuid(3));
        assertThat(invoiceRefunds.get(uuid(2)).getTargetPricesByFiscalItem())
                .isEqualTo(Map.of(101L, Money.of(99, "RUB"), 102L, Money.of(200, "RUB")));
        assertThat(invoiceRefunds.get(uuid(2)).getTargetPricesByFiscalItemMarkup())
                .isEqualTo(Map.of(101L, moneyMarkup(79, 20), 102L, moneyMarkup(200, 0)));
        assertThat(invoiceRefunds.get(uuid(3)).getTargetPricesByFiscalItem())
                .isEqualTo(Map.of(102L, Money.of(0, "RUB"), 103L, Money.of(300, "RUB")));
        assertThat(invoiceRefunds.get(uuid(3)).getTargetPricesByFiscalItemMarkup())
                .isEqualTo(Map.of(102L, moneyMarkup(0, 0), 103L, moneyMarkup(0, 300)));
    }

    private Order testOrder(Invoice... invoices) {
        Order order = new HotelOrder();
        order.setCurrency(ProtoCurrencyUnit.RUB);
        order.setInvoices(List.of(invoices));
        for (Invoice invoice : invoices) {
            invoice.setOrder(order);
        }
        return order;
    }

    private Invoice testInvoice(UUID id, String date, InvoiceItem... items) {
        TrustInvoice invoice = new TrustInvoice();
        invoice.setId(id);
        invoice.setCreatedAt(LocalDate.parse(date).atStartOfDay(ZoneId.of("UTC")).toInstant());
        invoice.setState(ETrustInvoiceState.IS_CLEARED);
        invoice.setInvoiceItems(List.of(items));
        for (InvoiceItem item : items) {
            item.setInvoice(invoice);
        }
        return invoice;
    }

    private InvoiceItem testInvoiceItem(long fiscalItemId, int price) {
        return testInvoiceItem(fiscalItemId, price, 0);
    }

    private InvoiceItem testInvoiceItem(long fiscalItemId, int price, int plusPoints) {
        InvoiceItem item = new InvoiceItem();
        item.setFiscalItemId(fiscalItemId);
        item.setPrice(BigDecimal.valueOf(price));
        item.setYandexPlusWithdraw(BigDecimal.valueOf(plusPoints));
        return item;
    }
}
