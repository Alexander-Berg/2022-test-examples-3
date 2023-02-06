package ru.yandex.market.core.order;

import java.util.List;
import java.util.Map;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.core.order.model.MbiOrder;
import ru.yandex.market.core.order.model.MbiOrderItemPromo;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.market.billing.dbschema.market_billing.MarketBilling.MARKET_BILLING;
import static ru.yandex.market.test.order.OrderTestData.order;
import static ru.yandex.market.test.order.OrderTestData.orderItem;
import static ru.yandex.market.test.order.OrderTestData.orderItemPromo;
import static ru.yandex.market.test.order.OrderTestData.orderPromo;

public class DbOrderDaoStoreItemPromosTest extends FunctionalTest {

    private static final Long ORDER_ID = 934239237L;

    private static final Long ITEM_ID_1 = 329423048L;
    private static final Long ITEM_ID_2 = 329423049L;

    private static final String MARKET_PROMO_ID_1 = "wienfeifnerf";
    private static final String MARKET_PROMO_ID_2 = "owiefjmwefnwe";

    @Autowired
    private DbOrderDao orderDao;

    @Autowired
    private DSLContext dslContext;

    @AfterEach
    public void afterEach() {
        dslContext.deleteFrom(MARKET_BILLING.CPA_ORDER)
                .where(MARKET_BILLING.CPA_ORDER.ORDER_ID.eq(ORDER_ID))
                .execute();

        dslContext.deleteFrom(MARKET_BILLING.CPA_ORDER_PROMO)
                .where(MARKET_BILLING.CPA_ORDER_PROMO.ORDER_ID.eq(ORDER_ID))
                .execute();

        dslContext.deleteFrom(MARKET_BILLING.CPA_ORDER_ITEM)
                .where(MARKET_BILLING.CPA_ORDER_ITEM.ORDER_ID.eq(ORDER_ID))
                .execute();

        dslContext.deleteFrom(MARKET_BILLING.CPA_ORDER_ITEM_PROMO)
                .where(MARKET_BILLING.CPA_ORDER_ITEM_PROMO.ORDER_ID.eq(ORDER_ID));
    }

    @Test
    public void storeOneItemPromoWithMarketPromoId() {
        MbiOrderItemPromo itemPromo = orderItemPromo(PromoType.MARKET_BLUE, MARKET_PROMO_ID_1);

        MbiOrder order = order(ORDER_ID,
                List.of(orderPromo(ORDER_ID, PromoType.MARKET_BLUE, MARKET_PROMO_ID_1)),
                List.of(orderItem(ORDER_ID, ITEM_ID_1, List.of(itemPromo))));

        orderDao.storeOrder(order);
        checkOneItemPromo(ITEM_ID_1, PromoType.MARKET_BLUE, MARKET_PROMO_ID_1);
    }

    @Test
    public void storeOneItemPromoWithNullMarketPromoId() {
        MbiOrderItemPromo itemPromo = orderItemPromo(PromoType.MARKET_BLUE, null);

        MbiOrder order = order(ORDER_ID,
                List.of(orderPromo(ORDER_ID, PromoType.MARKET_BLUE, null)),
                List.of(orderItem(ORDER_ID, ITEM_ID_1, List.of(itemPromo))));

        orderDao.storeOrder(order);
        checkOneItemPromo(ITEM_ID_1, PromoType.MARKET_BLUE, null);
    }

    @Test
    public void storeOneItemPromoWithNullMarketPromoIdInOrderAndEmptyInItem() {
        MbiOrderItemPromo itemPromo = orderItemPromo(PromoType.MARKET_BLUE, "");

        MbiOrder order = order(ORDER_ID,
                List.of(orderPromo(ORDER_ID, PromoType.MARKET_BLUE, null)),
                List.of(orderItem(ORDER_ID, ITEM_ID_1, List.of(itemPromo))));

        orderDao.storeOrder(order);
        checkOneItemPromo(ITEM_ID_1, PromoType.MARKET_BLUE, null);
    }

    @Test
    public void storeSeveralItemsWithPromos() {
        MbiOrderItemPromo item1Promo1 = orderItemPromo(PromoType.CASHBACK, MARKET_PROMO_ID_1);
        MbiOrderItemPromo item1Promo2 = orderItemPromo(PromoType.MARKET_BLUE, "");
        MbiOrderItemPromo item2Promo1 = orderItemPromo(PromoType.DIRECT_DISCOUNT, MARKET_PROMO_ID_2);
        MbiOrderItemPromo item2Promo2 = orderItemPromo(PromoType.MARKET_BLUE, null);

        MbiOrder order = order(ORDER_ID,
                List.of(
                        orderPromo(ORDER_ID, PromoType.CASHBACK, MARKET_PROMO_ID_1),
                        orderPromo(ORDER_ID, PromoType.DIRECT_DISCOUNT, MARKET_PROMO_ID_2),
                        orderPromo(ORDER_ID, PromoType.MARKET_BLUE, null)
                ),
                List.of(
                        orderItem(ORDER_ID, ITEM_ID_1, List.of(item1Promo1, item1Promo2)),
                        orderItem(ORDER_ID, ITEM_ID_2, List.of(item2Promo1, item2Promo2))
                ));

        orderDao.storeOrder(order);

        Map<Long, List<MbiOrderItemPromo>> actualPromos =
                orderDao.getOrderItemPromos(List.of(ITEM_ID_1, ITEM_ID_2),
                        PromoType.CASHBACK, PromoType.DIRECT_DISCOUNT, PromoType.MARKET_BLUE);

        assertThat(actualPromos).hasSize(2);

        assertThat(actualPromos.get(ITEM_ID_1)).hasSize(2);
        assertThat(actualPromos.get(ITEM_ID_2)).hasSize(2);

        Map<PromoType, MbiOrderItemPromo> actualItem1Promos =
                listToMap(actualPromos.get(ITEM_ID_1), MbiOrderItemPromo::getPromoType);
        assertThat(actualItem1Promos.get(PromoType.CASHBACK).getMarketPromoId()).isEqualTo(MARKET_PROMO_ID_1);
        assertThat(actualItem1Promos.get(PromoType.MARKET_BLUE).getMarketPromoId()).isNull();

        Map<PromoType, MbiOrderItemPromo> actualItem2Promos =
                listToMap(actualPromos.get(ITEM_ID_2), MbiOrderItemPromo::getPromoType);
        assertThat(actualItem2Promos.get(PromoType.DIRECT_DISCOUNT).getMarketPromoId()).isEqualTo(MARKET_PROMO_ID_2);
        assertThat(actualItem2Promos.get(PromoType.MARKET_BLUE).getMarketPromoId()).isNull();
    }

    private void checkOneItemPromo(Long itemId, PromoType promoType, String marketPromoId) {
        Map<Long, List<MbiOrderItemPromo>> actualPromos =
                orderDao.getOrderItemPromos(List.of(itemId), promoType);
        assertThat(actualPromos).hasSize(1);
        assertThat(actualPromos.get(itemId)).hasSize(1);

        MbiOrderItemPromo actualPromo = actualPromos.get(itemId).get(0);
        assertThat(actualPromo.getPromoType()).isEqualTo(promoType);
        assertThat(actualPromo.getMarketPromoId()).isEqualTo(marketPromoId);
    }
}
