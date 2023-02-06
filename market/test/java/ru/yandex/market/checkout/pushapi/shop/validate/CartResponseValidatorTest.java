package ru.yandex.market.checkout.pushapi.shop.validate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.delivery.AddressBuilder;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderItemBuilder;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartItem;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponseBuilder;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryBuilder;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryDatesBuilder;
import ru.yandex.market.checkout.pushapi.client.util.DateUtilBean;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.pushapi.client.entity.BuilderUtil.createDate;

public class CartResponseValidatorTest {

    private final DeliveryDatesBuilder deliveryDatesBuilder = new DeliveryDatesBuilder()
            .setFromDate(createDate("2013-06-01"))
            .setToDate(createDate("2013-06-02"));

    private DeliveryBuilder delivery1;
    private DeliveryBuilder delivery2;

    private OrderItemBuilder item1;
    private OrderItemBuilder item2;

    private CartResponseValidator validator;
    private CartResponseBuilder cartResponse;

    private DateUtilBean dateUtil = mock(DateUtilBean.class);

    public CartResponseValidatorTest() {

    }

    @BeforeEach
    public void setUp() throws Exception {
        delivery1 = new DeliveryBuilder()
                .setServiceName("1")
                .setPrice(BigDecimal.ZERO)
                .setDeliveryDates(deliveryDatesBuilder);
        delivery2 = new DeliveryBuilder()
                .setServiceName("2")
                .setPrice(BigDecimal.TEN)
                .setDeliveryDates(deliveryDatesBuilder);
        item1 = new OrderItemBuilder()
                .withFeedId(1234l)
                .withFeedCategoryId(null)
                .withOfferId("1");
        item2 = new OrderItemBuilder()
                .withFeedId(2345l)
                .withFeedCategoryId(null)
                .withOfferId("2");
        validator = new CartResponseValidator();

        cartResponse = new CartResponseBuilder();

        when(dateUtil.getToday()).thenReturn(createDate("2013-05-01"));

        validator.setDateUtil(dateUtil);

    }

    private void defaultValidate() {
        Cart cart = new Cart(
                null,
                null,
                null,
                asList(new CartItem() {{
                           setFeedId(1234L);
                           setOfferId("1");
                           setOfferName("OfferName");
                           setPrice(new BigDecimal("4567"));
                           setCount(5);
                           setDelivery(true);
                       }},
                        new CartItem() {{
                            setFeedId(2345L);
                            setOfferId("2");
                            setOfferName("OfferName");
                            setPrice(new BigDecimal("4567"));
                            setCount(5);
                            setDelivery(true);
                        }}
                )
        );
        cartResponse = cartResponse
                .setItems(item1, item2)
                .setDeliveryOptions(delivery1, delivery2);
        defaultValidate(cart, cartResponse.build());
    }

    private void defaultValidate(Cart cart, CartResponse cartResponse) {
        validator.validate(cart, cartResponse);
    }

    @Test
    public void testValidateOk() throws Exception {
        defaultValidate();
    }

    @Test
    public void testEmptyPriceOk() throws Exception {
        item1 = new OrderItemBuilder()
                .withFeedId(1234l)
                .withFeedCategoryId(null)
                .withPrice(null)
                .withOfferId("1");
        item2 = new OrderItemBuilder()
                .withFeedId(2345l)
                .withFeedCategoryId(null)
                .withPrice(null)
                .withOfferId("2");
        defaultValidate();
    }

    @Test
    public void testShouldNotBeNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            Cart cart = new Cart(
                    null,
                    null,
                    null,
                    asList(new CartItem() {{
                               setFeedId(1234L);
                               setOfferId("1");
                               setOfferName("OfferName");
                               setPrice(new BigDecimal("4567"));
                               setCount(5);
                               setDelivery(true);
                           }},
                            new CartItem() {{
                                setFeedId(2345L);
                                setOfferId("2");
                                setOfferName("OfferName");
                                setPrice(new BigDecimal("4567"));
                                setCount(5);
                                setDelivery(true);
                            }}
                    )
            );
            final CartResponse cartResponse = null;
            validator.validate(cart, cartResponse);
        });
    }

    @Test
    public void testNullItemFeedId() {
        Assertions.assertThrows(ValidationException.class, () -> {
            item2 = item2.withFeedId(null);
            defaultValidate();
        });
    }

    @Test
    public void testNullItemOfferId() {
        Assertions.assertThrows(ValidationException.class, () -> {
            item2 = item2.withOfferId(null);
            defaultValidate();
        });
    }

    @Test
    public void testNotNullFeedCategoryId() {
        Assertions.assertThrows(ValidationException.class, () -> {
            item2 = item2.withFeedCategoryId("Камеры");
            defaultValidate();
        });
    }

    @Test
    public void testNotNullCategoryId() {
        Assertions.assertThrows(ValidationException.class, () -> {
            item2 = item2.withCategoryId(1234);
            defaultValidate();
        });
    }

    @Test
    public void testZeroItemFeedId() {
        Assertions.assertThrows(ValidationException.class, () -> {
            item2 = item2.withFeedId(0L);
            defaultValidate();
        });
    }

    @Test
    public void testNegativeItemFeedId() {
        Assertions.assertThrows(ValidationException.class, () -> {
            item2 = item2.withFeedId(-1L);
            defaultValidate();
        });
    }

    @Test
    public void testEmptyItemOfferId() {
        Assertions.assertThrows(ValidationException.class, () -> {
            item2 = item2.withOfferId("");
            defaultValidate();
        });
    }

    @Test
    public void testZeroItemPrice() {
        Assertions.assertThrows(ValidationException.class, () -> {
            item2 = item2.withPrice(BigDecimal.ZERO);
            defaultValidate();
        });
    }

    @Test
    public void testNegativeItemPrice() {
        Assertions.assertThrows(ValidationException.class, () -> {
            item2 = item2.withPrice(new BigDecimal(-1));
            defaultValidate();
        });
    }

    @Test
    public void testNegativeItemCount() {
        Assertions.assertThrows(ValidationException.class, () -> {
            item2 = item2.withCount(-1);
            defaultValidate();
        });
    }

    @Test
    public void testNullDeliveryType() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setType(null);
            defaultValidate();
        });
    }

    @Test
    public void testNullDeliveryServiceId() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setServiceName(null);
            defaultValidate();
        });
    }

    @Test
    public void testNullDeliveryPrice() {
        Assertions.assertDoesNotThrow(() -> {
            delivery2 = delivery2.setPrice(null);
            defaultValidate();
        });
    }

    @Test
    public void testDeliveryDatesShouldNotBeNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setDeliveryDates((DeliveryDates) null);
            defaultValidate();
        });
    }

    @Test
    public void testToDateInDeliveryDatesCanBeNull() throws Exception {
        delivery2 = delivery2.setDeliveryDates(new DeliveryDatesBuilder().setToDate(null));
        defaultValidate();
    }

    @Test
    public void testFromDateInDeliveryDatesShouldNotBeNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setDeliveryDates(deliveryDatesBuilder.setFromDate(null));
            defaultValidate();
        });
    }

    @Test
    public void testNullDeliveryOutletsWhenTypeIsPickup() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setType(DeliveryType.PICKUP);
            delivery2 = delivery2.setOutletIds(null);
            defaultValidate();
        });
    }

    @Test
    public void testEmptyDeliveryOutletsWhenTypeIsPickup() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setType(DeliveryType.PICKUP);
            delivery2 = delivery2.setOutletIds(Collections.emptySet());
            defaultValidate();
        });
    }

    @Test
    public void testNullDeliveryOutletIdWhenTypeIsPickup() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setType(DeliveryType.PICKUP);
            delivery2 = delivery2.setOutletIds(Collections.singleton(null));
            defaultValidate();
        });
    }

    @Test
    public void testNegativeDeliveryOutletIdWhenTypeIsPickup() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setType(DeliveryType.PICKUP);
            delivery2 = delivery2.setOutletIds(Collections.singleton(-1L));
            defaultValidate();
        });
    }

    @Test
    public void testNotNullDeliveryOutletsWhenTypeIsDelivery() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setType(DeliveryType.DELIVERY);
            delivery2 = delivery2.setOutletIds(Collections.emptySet());
            defaultValidate();
        });
    }

    @Test
    public void testNotNullDeliveryAddress() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setShopAddress(new AddressBuilder());
            defaultValidate();
        });
    }

    @Test
    public void testNotNullDeliveryRegionId() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setRegionId(123l);
            defaultValidate();
        });
    }

    @Test
    public void testNegativeDeliveryPrice() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setPrice(new BigDecimal(-1));
            defaultValidate();
        });
    }

    @Test
    public void testZeroDeliveryOutletId() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setOutletIds(Set.of(1l, 0l));
            defaultValidate();
        });
    }

    @Test
    public void testNegativeDeliveryOutletId() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery2 = delivery2.setOutletIds(Set.of(1l, -1l));
            defaultValidate();
        });
    }

    @Test
    public void testNullPaymentMethods() throws Exception {
        cartResponse = cartResponse.setPaymentMethods((PaymentMethod[]) null);
        defaultValidate();
    }

    @Test
    public void testEmptyPaymentMethods() throws Exception {
        cartResponse = cartResponse.setPaymentMethods();
        defaultValidate();
    }

    @Test
    public void testNullPaymentMethod() {
        Assertions.assertThrows(ValidationException.class, () -> {
            cartResponse = cartResponse.setPaymentMethods(null, PaymentMethod.YANDEX_MONEY);
            defaultValidate();
        });
    }

    @Test
    public void offerIdIsNotUnique() throws Exception {
        cartResponse = cartResponse.setItems(
                new OrderItemBuilder()
                        .withFeedId(1234l)
                        .withOfferId("1234"),
                new OrderItemBuilder()
                        .withFeedId(1234l)
                        .withOfferId("1235")
        );
        defaultValidate();
    }

    @Test
    public void deliveryIdShouldNotBeLongerThanMaximumLength() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery1 = delivery1.setShopDeliveryId(
                    "1234567890abcdefghij" + "1234567890abcdefghij" + "1234567890abcdefghij"
                            + "1234567890abcdefghij" + "1234567890abcdefghij" + "1234567890abcdefghij"
            );

            defaultValidate();
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //     TAX DATA TESTS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Test
    public void onlyTaxSystem() {
        Assertions.assertThrows(ValidationException.class, () -> {
            cartResponse = cartResponse.setTaxSystem(TaxSystem.OSN);
            defaultValidate();
        });
    }

    @Test
    public void onlyItemVat() {
        Assertions.assertThrows(ValidationException.class, () -> {
            item1 = item1.withVat(VatType.VAT_10);
            defaultValidate();
        });
    }

    @Test
    public void onlyDeliveryVat() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery1 = delivery1.setVat(VatType.VAT_18);
            defaultValidate();
        });
    }

    private void fillTaxDataFully() {
        cartResponse = cartResponse.setTaxSystem(TaxSystem.OSN);
        item1 = item1.withVat(VatType.VAT_10);
        item2 = item2.withVat(VatType.VAT_18);
        delivery1 = delivery1.setVat(VatType.VAT_0);
        delivery2 = delivery2.setVat(VatType.NO_VAT);
    }

    @Test
    public void withoutItemVat() {
        Assertions.assertThrows(ValidationException.class, () -> {
            fillTaxDataFully();
            item1 = item1.withVat(null);
            defaultValidate();
        });
    }

    @Test
    public void withoutDeliveryVat() {
        Assertions.assertThrows(ValidationException.class, () -> {
            fillTaxDataFully();
            delivery2 = delivery2.setVat(null);
            defaultValidate();
        });
    }

    @Test
    public void withoutFreeDeliveryVatIsOk() throws Exception {
        fillTaxDataFully();
        delivery2 = delivery2.setPrice(BigDecimal.ZERO).setVat(null);
        defaultValidate();
    }

    @Test
    public void withoutTaxSystem() {
        Assertions.assertThrows(ValidationException.class, () -> {
            fillTaxDataFully();
            cartResponse = cartResponse.setTaxSystem(null);
            defaultValidate();
        });
    }

    @Test
    public void completeTaxData() throws Exception {
        fillTaxDataFully();
        defaultValidate();
    }

    @Test
    public void invalidTaxData() {
        Assertions.assertThrows(ValidationException.class, () -> {
            fillTaxDataFully();
            cartResponse = cartResponse.setTaxSystem(TaxSystem.USN);
            defaultValidate();
        });
    }

    @Test
    public void mismatchingDeliveryVatIsOk() throws Exception {
        cartResponse = cartResponse.setTaxSystem(TaxSystem.USN);
        item1 = item1.withVat(VatType.NO_VAT);
        item2 = item2.withVat(VatType.NO_VAT);
        delivery1 = delivery1.setVat(VatType.VAT_18);
        delivery2 = delivery2.setVat(VatType.VAT_10);
        defaultValidate();
    }
}
