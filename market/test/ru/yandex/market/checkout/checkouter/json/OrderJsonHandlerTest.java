package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;

import org.json.JSONException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class OrderJsonHandlerTest extends AbstractJsonHandlerTestBase {

    public static final String EXPECTED_STR = "{\"acceptMethod\":\"WEB_INTERFACE\"}";

    @Test
    public void serializationTest() throws IOException, ParseException {
        Order order = EntityHelper.getOrder();

        String json = write(order);
        System.out.println(json);

        checkJson(json, "$." + Names.ID, 123);
        checkJson(json, "$." + Names.Order.SHOP_ID, 234);
        checkJson(json, "$." + Names.Order.STATUS, OrderStatus.PROCESSING.name());
        checkJson(json, "$." + Names.Order.SUBSTATUS, OrderSubstatus.PENDING_CANCELLED.name());

        checkJson(json, "$." + Names.Order.CREATION_DATE, "11-11-2017 15:00:00");
        checkJson(json, "$." + Names.Order.CREATION_DATE_TS, "1510401600000");
        checkJson(json, "$." + Names.Order.UPDATE_DATE, "15-11-2017 18:00:00");
        checkJson(json, "$." + Names.Order.UPDATE_DATE_TS, "1510758000000");
        checkJson(json, "$." + Names.Order.STATUS_UPDATE_DATE, "13-11-2017 22:00:00");
        checkJson(json, "$." + Names.Order.STATUS_UPDATE_DATE_TS, "1510599600000");
        checkJson(json, "$." + Names.Order.STATUS_EXPIRY_DATE, "16-11-2017 00:00:00");
        checkJson(json, "$." + Names.Order.STATUS_EXPIRY_DATE_TS, "1510779600000");

        checkJson(json, "$." + Names.Order.CURRENCY, Currency.RUR.name());
        checkJson(json, "$." + Names.Order.BUYER_CURRENCY, Currency.USD.name());
        checkJson(json, "$." + Names.Order.EXCHANGE_RATE, 12.34);

        checkJson(json, "$." + Names.Order.ITEMS_TOTAL, 23.45);
        checkJson(json, "$." + Names.Order.BUYER_ITEMS_TOTAL, 34.56);
        checkJson(json, "$." + Names.Order.TOTAL, 45.67);
        checkJson(json, "$." + Names.Order.BUYER_TOTAL, 56.78);
        checkJson(json, "$." + Names.Order.REAL_TOTAL, 67.89);
        checkJson(json, "$." + Names.Order.FEE_TOTAL, 78.91);

        checkJson(json, "$." + Names.Order.PAYMENT_TYPE, PaymentType.PREPAID.name());
        checkJson(json, "$." + Names.Order.PAYMENT_METHOD, PaymentMethod.YANDEX.name());

        checkJson(json, "$." + Names.Order.ITEMS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Order.ITEMS, hasSize(1));
        checkJson(json, "$." + Names.Order.DELIVERY, JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + Names.Order.BUYER, JsonPathExpectationsHelper::assertValueIsMap);

        checkJson(json, "$." + Names.Order.FAKE, true);
        checkJson(json, "$." + Names.Order.CONTEXT, Context.MARKET.name());
        checkJson(json, "$." + Names.Order.NOTES, "notes");
        checkJson(json, "$." + Names.Order.SHOP_ORDER_ID, "shopOrderId");

        checkJson(json, "$." + Names.Order.DELIVERY_OPTIONS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Order.DELIVERY_OPTIONS, hasSize(1));
        checkJson(json, "$." + Names.Order.PAYMENT_OPTIONS, JsonPathExpectationsHelper::assertValueIsArray);

        checkJson(json, "$." + Names.Order.CHANGES, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Order.CHANGES, hasItem(CartChange.DELIVERY.name()));
        checkJson(json, "$." + Names.Order.PAYMENT_ID, 567);
        checkJson(json, "$." + Names.Order.BALANCE_ORDER_ID, "balanceOrderId");
        checkJson(json, "$." + Names.Order.REFUND_PLANNED, 67.89);
        checkJson(json, "$." + Names.Order.REFUND_ACTUAL, 78.91);

        checkJson(json, "$." + Names.Order.USER_GROUP, UserGroup.ABO.name());

        checkJson(json, "$." + Names.Validation.VALIDATION_ERRORS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Validation.VALIDATION_ERRORS, hasSize(1));
        checkJson(json, "$." + Names.Validation.VALIDATION_WARNINGS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Validation.VALIDATION_WARNINGS, hasSize(1));
        checkJson(json, "$." + Names.Order.NO_AUTH, true);
        checkJson(json, "$." + Names.Order.ACCEPT_METHOD, OrderAcceptMethod.WEB_INTERFACE.name());

        checkJson(json, "$." + Names.Order.SIGNATURE, "signature");
        checkJson(json, "$." + Names.Order.SHOP_NAME, "shopName");
        checkJson(json, "$." + Names.Order.DISPLAY_ORDER_ID, "shopOrderId");
        checkJson(json, "$." + Names.Order.GLOBAL, true);
        checkJson(json, "$." + Names.Order.PAYMENT, JsonPathExpectationsHelper::assertValueIsMap);
        checkJson(json, "$." + Names.Order.TAX_SYSTEM, TaxSystem.OSN.name());
        checkJson(json, "$." + Names.Order.VALID_FEATURES, JsonPathExpectationsHelper::assertValueIsArray);

        checkJson(json, "$." + Names.Order.BUYER_ITEMS_TOTAL_BEFORE_DISCOUNT, 46.9);
        checkJson(json, "$." + Names.Order.BUYER_ITEMS_TOTAL_DISCOUNT, 12.34);
        checkJson(json, "$." + Names.Order.BUYER_TOTAL_BEFORE_DISCOUNT, 69.12);
        checkJson(json, "$." + Names.Order.BUYER_TOTAL_DISCOUNT, 12.34);

        checkJson(json, "$." + Names.Order.SUBSIDY_TOTAL, 23.45);
        checkJson(json, "$." + Names.Order.TOTAL_WITH_SUBSIDY, 69.12);
        checkJson(json, "$." + Names.Order.BUYER_SUBSIDY_TOTAL, 289.37);
        checkJson(json, "$." + Names.Order.BUYER_TOTAL_WITH_SUBSIDY, 346.15);

        checkJson(json, "$.promos[0].type", PromoType.MARKET_BLUE.getCode());
        checkJson(json, "$.promos[0].buyerItemsDiscount", 12.34);
        checkJson(json, "$.promos[0].subsidy", 90.23);

        checkJson(json, "$.fulfilment", true);
    }

    @Test
    public void deserialize() throws IOException {
        Order order = read(Order.class, getClass().getResourceAsStream("order.json"));

        Assertions.assertEquals(123L, order.getId().longValue());
        Assertions.assertEquals(234L, order.getShopId().longValue());
        Assertions.assertEquals(OrderStatus.PROCESSING, order.getStatus());
        Assertions.assertEquals(OrderSubstatus.PENDING_CANCELLED, order.getSubstatus());

        Assertions.assertEquals(EntityHelper.CREATION_DATE, order.getCreationDate());
        Assertions.assertEquals(EntityHelper.UPDATE_DATE, order.getUpdateDate());
        Assertions.assertEquals(EntityHelper.STATUS_UPDATE_DATE, order.getStatusUpdateDate());
        Assertions.assertEquals(EntityHelper.STATUS_EXPIRY_DATE, order.getStatusExpiryDate());

        Assertions.assertEquals(Currency.RUR, order.getCurrency());
        Assertions.assertEquals(Currency.USD, order.getBuyerCurrency());
        Assertions.assertEquals(new BigDecimal("12.34"), order.getExchangeRate());

        Assertions.assertEquals(new BigDecimal("23.45"), order.getItemsTotal());
        Assertions.assertEquals(new BigDecimal("34.56"), order.getBuyerItemsTotal());
        Assertions.assertEquals(new BigDecimal("45.67"), order.getTotal());
        Assertions.assertEquals(new BigDecimal("56.78"), order.getBuyerTotal());
        Assertions.assertEquals(new BigDecimal("67.89"), order.getRealTotal());
        Assertions.assertEquals(new BigDecimal("78.91"), order.getFeeTotal());

        Assertions.assertEquals(PaymentType.PREPAID, order.getPaymentType());
        Assertions.assertEquals(PaymentMethod.YANDEX, order.getPaymentMethod());
        Assertions.assertEquals(TaxSystem.OSN, order.getTaxSystem());

        assertThat(order.getItems(), hasSize(1));
        Assertions.assertNotNull(order.getDelivery());
        Assertions.assertNotNull(order.getBuyer());

        Assertions.assertTrue(order.isFake());
        Assertions.assertEquals("notes", order.getNotes());
        Assertions.assertEquals("shopOrderId", order.getShopOrderId());

        assertThat(order.getDeliveryOptions(), hasSize(1));
        assertThat(order.getPaymentOptions(), hasSize(1));

        assertThat(order.getChanges(), hasItem(CartChange.DELIVERY));
        Assertions.assertEquals(UserGroup.ABO, order.getUserGroup());
        Assertions.assertEquals(567, order.getPaymentId().longValue());
        Assertions.assertEquals("balanceOrderId", order.getBalanceOrderId());

        Assertions.assertEquals(new BigDecimal("67.89"), order.getRefundPlanned());
        Assertions.assertEquals(new BigDecimal("78.91"), order.getRefundActual());
        Assertions.assertTrue(order.isNoAuth());
        Assertions.assertEquals(OrderAcceptMethod.WEB_INTERFACE, order.getAcceptMethod());
        Assertions.assertEquals(Context.MARKET, order.getContext());

        assertThat(order.getValidationErrors(), hasSize(1));
        assertThat(order.getValidationWarnings(), hasSize(1));

        Assertions.assertEquals("signature", order.getSignature());
        Assertions.assertTrue(order.isGlobal());
        Assertions.assertNotNull(order.getPayment());

        Assertions.assertEquals(new BigDecimal("23.45"), order.getPromoPrices().getSubsidyTotal());
        assertThat(order.getPromos(), hasSize(1));

        Assertions.assertTrue(order.isFulfilment());
    }

    @Test
    public void shouldSerializeAcceptMethod() throws IOException, JSONException {
        Order order = new Order();
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        String json = write(order);
        JSONAssert.assertEquals(EXPECTED_STR, json, false);
    }
}
