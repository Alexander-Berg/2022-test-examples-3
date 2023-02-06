package ru.yandex.market.notifier.core;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.notifier.application.AbstractServicesTestBase;
import ru.yandex.market.notifier.jobs.zk.impl.LocalOrder;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         29.03.17
 */
public class OXMHelperTest extends AbstractServicesTestBase {
    @Autowired
    private OXMHelper oxmHelper;

    @Test
    public void toXmlString() {
        Order o = new Order();
        o.setId(1L);
        o.setShopOrderId("101");
        o.setShopId(774L);
        o.setCreationDate(new Date());
        Delivery d = new Delivery();
        o.setDelivery(d);
        OrderItem item = new OrderItem(new FeedOfferId("dsflkjsdf", 32423L), new BigDecimal(1000), 1);
        o.setItems(Collections.singletonList(item));
        Buyer b = new Buyer();
        b.setFirstName("Kate");
        b.setEmail("kukabara@yandex-team.ru");
        o.setBuyer(b);
        LocalOrder lo = new LocalOrder(o);
        lo.setRefundAmount(new BigDecimal(150));
        lo.setRefundCurrency(Currency.RUR);
        lo.setRefundReason("Возврат за товар");
        lo.setConversation(false);
        assertNotNull(oxmHelper.toXmlString(lo, MarketOXMAliases.MARKETPLACE_OXM_ALIASES));
    }

    @Test
    public void toXmlWithBadCharString() {
        Order o = new Order();
        o.setId(1L);
        o.setShopOrderId("101");
        o.setShopId(774L);
        o.setCreationDate(new Date());
        Delivery d = new Delivery();
        o.setDelivery(d);
        OrderItem item = new OrderItem(new FeedOfferId("dsflkjsdf", 32423L), new BigDecimal(1000), 1);
        o.setItems(Collections.singletonList(item));
        Buyer b = new Buyer();
        b.setFirstName("Kate");
        b.setEmail("kukabara@yandex-team.ru");
        o.setBuyer(b);
        LocalOrder lo = new LocalOrder(o);
        lo.setRefundAmount(new BigDecimal(150));
        lo.setRefundCurrency(Currency.RUR);
        lo.setRefundReason("Возврат за товар"+(char)0x1d);
        lo.setConversation(false);
        assertNotNull(oxmHelper.toXmlString(lo, MarketOXMAliases.MARKETPLACE_OXM_ALIASES));
    }

}
