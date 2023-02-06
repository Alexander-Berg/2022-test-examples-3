package ru.yandex.market.checkout.checkouter.json;

import java.util.Collections;
import java.util.EnumSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.JsonPathExpectationsHelper;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.json.helper.EntityHelper;
import ru.yandex.market.checkout.checkouter.order.CartPresetInfo;
import ru.yandex.market.checkout.checkouter.order.PresetInfo;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class MultiCartJsonHandlerTest extends AbstractJsonHandlerTestBase {

    @Test
    public void serialize() throws Exception {
        MultiCart multiCart = new MultiCart();
        multiCart.setBuyerRegionId(2L);
        multiCart.setBuyerCurrency(Currency.RUR);
        multiCart.setPaymentType(PaymentType.PREPAID);
        multiCart.setPaymentMethod(PaymentMethod.YANDEX);
        multiCart.setPromoCode("promoCode");
        multiCart.setCarts(Collections.singletonList(EntityHelper.getOrder()));
        multiCart.setPaymentOptions(EnumSet.of(PaymentMethod.YANDEX));
        multiCart.setCartFailures(Collections.singletonList(EntityHelper.getOrderFailure()));
        multiCart.setValidationErrors(Collections.singletonList(
                new ValidationResult("errorCode", ValidationResult.Severity.ERROR)
        ));
        multiCart.setValidationWarnings(Collections.singletonList(
                new ValidationResult("warningCode", ValidationResult.Severity.WARNING)
        ));
        PresetInfo presetInfo = new PresetInfo();
        presetInfo.setPresetId("sd123");
        presetInfo.setType(DeliveryType.POST);
        CartPresetInfo cartPresetInfo = new CartPresetInfo();
        cartPresetInfo.setLabel("label");
        cartPresetInfo.setDeliveryAvailable(true);
        presetInfo.setCarts(Collections.singletonList(cartPresetInfo));
        multiCart.setPresets(Collections.singletonList(presetInfo));

        String json = write(multiCart);
        System.out.println(json);

        checkJson(json, "$." + Names.Multi.BUYER_REGION_ID, 2);
        checkJson(json, "$." + Names.Multi.BUYER_CURRENCY, Currency.RUR.name());
        checkJson(json, "$." + Names.Order.PAYMENT_TYPE, PaymentType.PREPAID.name());
        checkJson(json, "$." + Names.Order.PAYMENT_METHOD, PaymentMethod.YANDEX.name());
        checkJson(json, "$." + Names.Order.PROMO_CODE, "promoCode");
        checkJson(json, "$." + Names.Multi.CARTS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Multi.CARTS, hasSize(1));

        checkJson(json, "$." + Names.Order.PAYMENT_OPTIONS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Order.PAYMENT_OPTIONS, hasSize(1));

        checkJson(json, "$." + Names.Multi.CART_FAILURES, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Multi.CART_FAILURES, hasSize(1));

        checkJson(json, "$." + Names.Validation.VALIDATION_ERRORS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Validation.VALIDATION_ERRORS, hasSize(1));

        checkJson(json, "$." + Names.Validation.VALIDATION_WARNINGS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Validation.VALIDATION_WARNINGS, hasSize(1));

        checkJson(json, "$." + Names.Multi.PRESETS, JsonPathExpectationsHelper::assertValueIsArray);
        checkJsonMatcher(json, "$." + Names.Multi.PRESETS, hasSize(1));
    }

    @Test
    public void deserialize() throws Exception {
        MultiCart multiCart = read(MultiCart.class, getClass().getResourceAsStream("multiCart.json"));

        Assertions.assertEquals(2, multiCart.getBuyerRegionId().longValue());
        Assertions.assertEquals(Currency.RUR, multiCart.getBuyerCurrency());
        Assertions.assertEquals(PaymentType.PREPAID, multiCart.getPaymentType());
        Assertions.assertEquals(PaymentMethod.YANDEX, multiCart.getPaymentMethod());
        Assertions.assertEquals("promoCode", multiCart.getPromoCode());
        Assertions.assertNotNull(multiCart.getCarts());
        assertThat(multiCart.getCarts(), hasSize(1));
        Assertions.assertNotNull(multiCart.getPaymentOptions());
        assertThat(multiCart.getPaymentOptions(), hasSize(1));
        Assertions.assertNotNull(multiCart.getCartFailures());
        assertThat(multiCart.getCartFailures(), hasSize(1));
        Assertions.assertNotNull(multiCart.getValidationErrors());
        assertThat(multiCart.getValidationErrors(), hasSize(1));
        Assertions.assertNotNull(multiCart.getValidationWarnings());
        assertThat(multiCart.getValidationWarnings(), hasSize(1));
        assertThat(multiCart.getPresets(), hasSize(1));
    }
}
