package ru.yandex.market.checkout.checkouter.json;

import java.util.Collections;
import java.util.EnumSet;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNull.notNullValue;

public class MultiOrderJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setBuyerRegionId(2L);
        multiOrder.setBuyerCurrency(Currency.RUR);
        multiOrder.setPaymentType(PaymentType.PREPAID);
        multiOrder.setPaymentMethod(PaymentMethod.YANDEX);
        multiOrder.setPromoCode("promoCode");
        multiOrder.setOrders(Collections.singletonList(EntityHelper.getOrder()));
        multiOrder.setPaymentOptions(EnumSet.of(PaymentMethod.YANDEX));
        multiOrder.setOrderFailures(Collections.singletonList(EntityHelper.getOrderFailure()));
        multiOrder.setValidationErrors(Collections.singletonList(new ValidationResult("errorCode",
                ValidationResult.Severity.ERROR)));
        multiOrder.setValidationWarnings(Collections.singletonList(new ValidationResult("warningCode",
                ValidationResult.Severity.WARNING)));
        multiOrder.setBuyer(EntityHelper.getBuyer());

        String json = write(multiOrder);

        checkJson(json, Names.Multi.BUYER_REGION_ID, 2);
        checkJson(json, Names.Multi.BUYER_CURRENCY, Currency.RUR.name());
        checkJson(json, Names.Order.PAYMENT_TYPE, PaymentType.PREPAID.name());
        checkJson(json, Names.Order.PAYMENT_METHOD, PaymentMethod.YANDEX.name());
        checkJson(json, Names.Order.PROMO_CODE, "promoCode");
        checkJson(json, Names.Multi.ORDERS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, Names.Multi.ORDERS, hasSize(1));
        checkJson(json, Names.Order.PAYMENT_OPTIONS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, Names.Order.PAYMENT_OPTIONS, hasSize(1));
        checkJson(json, Names.Multi.ORDER_FAILURES, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, Names.Multi.ORDER_FAILURES, hasSize(1));
        checkJson(json, Names.Validation.VALIDATION_ERRORS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, Names.Validation.VALIDATION_ERRORS, hasSize(1));
        checkJson(json, Names.Validation.VALIDATION_WARNINGS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, Names.Validation.VALIDATION_WARNINGS, hasSize(1));
        checkJson(json, Names.Order.BUYER, JsonPathExpectationsHelper::assertValueIsMap);
        checkJsonMatcher(json, Names.Order.BUYER, notNullValue());

        // Упадет, если будут дубликаты полей. например "paymentType"
        new ObjectMapper()
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .readTree(json);
    }

    @Test
    public void serializeCheckedOut() throws Exception {
        MultiOrder multiOrder = new MultiOrder();
        multiOrder.setBuyerRegionId(2L);
        multiOrder.setBuyerCurrency(Currency.RUR);
        multiOrder.setPaymentType(PaymentType.PREPAID);
        multiOrder.setPaymentMethod(PaymentMethod.YANDEX);
        multiOrder.setPromoCode("promoCode");
        multiOrder.setOrders(Collections.singletonList(EntityHelper.getOrderNoErrors()));
        multiOrder.setPaymentOptions(EnumSet.of(PaymentMethod.YANDEX));
        multiOrder.setBuyer(EntityHelper.getBuyer());

        String json = write(multiOrder);

        checkJson(json, Names.Multi.BUYER_REGION_ID, 2);
        checkJson(json, Names.Multi.BUYER_CURRENCY, Currency.RUR.name());
        checkJson(json, Names.Order.PAYMENT_TYPE, PaymentType.PREPAID.name());
        checkJson(json, Names.Order.PAYMENT_METHOD, PaymentMethod.YANDEX.name());
        checkJson(json, Names.Order.PROMO_CODE, "promoCode");
        checkJson(json, Names.Multi.ORDERS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, Names.Multi.ORDERS, hasSize(1));
        checkJson(json, Names.Order.PAYMENT_OPTIONS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, Names.Order.PAYMENT_OPTIONS, hasSize(1));
        checkJson(json, Names.Order.BUYER, JsonPathExpectationsHelper::assertValueIsMap);
        checkJsonMatcher(json, Names.Order.BUYER, notNullValue());
        checkJson(json, Names.Multi.CHECKED_OUT, true);
    }

    @Test
    public void deserialize() throws Exception {
        MultiOrder multiOrder = read(MultiOrder.class, getClass().getResourceAsStream("multiOrder.json"));

        Assertions.assertEquals(2, multiOrder.getBuyerRegionId().longValue());
        Assertions.assertEquals(Currency.RUR, multiOrder.getBuyerCurrency());
        Assertions.assertEquals(PaymentType.PREPAID, multiOrder.getPaymentType());
        Assertions.assertEquals(PaymentMethod.YANDEX, multiOrder.getPaymentMethod());
        assertThat(multiOrder.getCarts(), notNullValue());
        assertThat(multiOrder.getCarts(), hasSize(1));
        assertThat(multiOrder.getPaymentOptions(), hasItem(PaymentMethod.YANDEX));
        assertThat(multiOrder.getCartFailures(), notNullValue());
        assertThat(multiOrder.getCartFailures(), hasSize(1));
        assertThat(multiOrder.getValidationErrors(), notNullValue());
        assertThat(multiOrder.getValidationErrors(), hasSize(1));
        assertThat(multiOrder.getValidationWarnings(), notNullValue());
        assertThat(multiOrder.getValidationWarnings(), hasSize(1));
        assertThat(multiOrder.getCarts().get(0).getPaymentSystem(), is("mastercard"));
        Assertions.assertEquals("promoCode", multiOrder.getPromoCode());
        Assertions.assertNotNull(multiOrder.getBuyer());
    }
}
