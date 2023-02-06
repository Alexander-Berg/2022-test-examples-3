package ru.yandex.market.checkout.checkouter.json;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.MARKET_COUPON;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoType.MARKET_PROMOCODE;

public class ItemPromoJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serializeItemPromos() throws Exception {
        OrderItem orderHistoryEvent = EntityHelper.getOrderItem();

        String json = write(orderHistoryEvent);
        System.out.println(json);

        checkJson(json, "$." + Names.OrderItem.PROMOS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.OrderItem.PROMOS, hasSize(2));
        checkJsonMatcher(json, "$." + Names.OrderItem.PROMOS + "[*].type", containsInAnyOrder("CASHBACK",
                "MARKET_COUPON"));
        checkJsonMatcher(json, "$." + Names.OrderItem.PROMOS + "[*].cashbackAccrualAmount", hasItem(151));
        checkJsonMatcher(json, "$." + Names.OrderItem.PROMOS + "[*].marketCashbackPercent", hasItem(5));
        checkJsonMatcher(json, "$." + Names.OrderItem.PROMOS + "[*].partnerCashbackPercent", hasItem(95));
        checkJsonMatcher(json, "$." + Names.OrderItem.PROMOS + "[*].partnerId", hasItem(123));
        checkJsonMatcher(json, "$." + Names.OrderItem.PROMOS + "[*].isPickupPromocode", hasItem(false));
    }

    @Test
    public void deserializeItemPromos() throws Exception {
        String json = "{\"id\": 123, \"promos\": [{\"type\": \"CASHBACK\", \"bundleReturnRestrict\": false, " +
                "\"buyerDiscount\": 0, \"subsidy\": 0, \"buyerSubsidy\": 0, \"cashbackAccrualAmount\": 151, " +
                "\"marketCashbackPercent\": 5, \"partnerCashbackPercent\": 95, \"partnerId\": 123}, " +
                "{\"type\": \"MARKET_COUPON\", \"bundleReturnRestrict\": false, \"buyerDiscount\": 5.43, \"subsidy\":" +
                " 4.32, \"buyerSubsidy\": 3.21}]}";
        OrderItem orderItem = read(OrderItem.class, json);

        assertThat(orderItem.getPromos(), hasSize(2));

        ItemPromo cashbackPromo = orderItem.getPromos().stream()
                .filter(itemPromo -> itemPromo.getType().equals(PromoType.CASHBACK))
                .findFirst().get();
        ItemPromo marketCoupon = orderItem.getPromos().stream()
                .filter(itemPromo -> itemPromo.getType().equals(MARKET_COUPON))
                .findFirst().get();

        assertThat(cashbackPromo.getCashbackAccrualAmount(), equalTo(BigDecimal.valueOf(151L)));
        assertThat(cashbackPromo.getMarketCashbackPercent(), equalTo(BigDecimal.valueOf(5L)));
        assertThat(cashbackPromo.getPartnerCashbackPercent(), equalTo(BigDecimal.valueOf(95L)));
        assertThat(cashbackPromo.getPartnerId(), equalTo(123L));
        assertThat(marketCoupon.getCashbackAccrualAmount(), nullValue());
    }

    @Test
    public void deserializeItemPromosAndCheckIsPickupPromo() throws Exception {
        String json = "{\"id\": 123, \"promos\": [{\"type\": \"CASHBACK\", \"bundleReturnRestrict\": false, " +
                "\"buyerDiscount\": 0, \"subsidy\": 0, \"buyerSubsidy\": 0, \"cashbackAccrualAmount\": 151, " +
                "\"marketCashbackPercent\": 5, \"partnerCashbackPercent\": 95, \"partnerId\": 123}, " +
                "{\"type\": \"MARKET_PROMOCODE\", \"bundleReturnRestrict\": false, \"buyerDiscount\": 5.43, " +
                "\"subsidy\": 4.32, \"buyerSubsidy\": 3.21,  \"isPickupPromocode\": true}]}";
        OrderItem orderItem = read(OrderItem.class, json);

        assertThat(orderItem.getPromos(), hasSize(2));

        ItemPromo marketCoupon = orderItem.getPromos().stream()
                .filter(itemPromo -> itemPromo.getType().equals(MARKET_PROMOCODE))
                .findFirst().get();

        assertThat(marketCoupon.getIsPickupPromocode(), equalTo(true));
    }
}
