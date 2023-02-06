package ru.yandex.market.checkout.pushapi.shop.validate;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.checkout.checkouter.delivery.AddressBuilder;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderItemBuilder;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.entity.*;
import ru.yandex.market.checkout.pushapi.client.util.DateUtilBean;

import java.math.BigDecimal;
import java.util.Collections;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.pushapi.client.entity.BuilderUtil.createDate;

public class CartResponseValidatorTest {

    private final DeliveryDatesBuilder deliveryDatesBuilder = new DeliveryDatesBuilder()
        .setFromDate(createDate("2013-06-01"))
        .setToDate(createDate("2013-06-02"));

    private final DeliveryBuilder delivery1 = new DeliveryBuilder()
        .setDeliveryDates(deliveryDatesBuilder);
    private final DeliveryBuilder delivery2 = new DeliveryBuilder()
        .setDeliveryDates(deliveryDatesBuilder);

    private OrderItemBuilder item1 = new OrderItemBuilder()
        .withFeedId(1234l)
        .withOfferId("1");
    private OrderItemBuilder item2 = new OrderItemBuilder()
        .withFeedId(2345l)
        .withOfferId("2");

    private CartResponseValidator validator = new CartResponseValidator();
    private CartResponseBuilder cartResponse = new CartResponseBuilder()
        .setItems(item1, item2)
        .setDeliveryOptions(delivery1, delivery2);

    private Cart cart = new Cart(
        null,
        null,
        asList(item1.build(), item2.build())
    );

    private DateUtilBean dateUtil = mock(DateUtilBean.class);

    @Before
    public void setUp() throws Exception {
        when(dateUtil.getToday()).thenReturn(createDate("2013-05-01"));

        validator.setDateUtil(dateUtil);
    }

    private void defaultValidate() {
        validator.validate(cart, cartResponse.build());
    }

    @Test
    public void testValidateOk() throws Exception {
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testShouldNotBeNull() throws Exception {
        final CartResponse cartResponse = null;
        validator.validate(cart, cartResponse);
    }

    @Test(expected = ValidationException.class)
    public void testValidateNullItems() throws Exception {
        cartResponse.setItems(null);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNullDeliveryOptions() throws Exception {
        cartResponse.setDeliveryOptions(null);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testEmptyItems() throws Exception {
        cartResponse.setItems();
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testEmptyDeliveryOptions() throws Exception {
        cartResponse.setDeliveryOptions();
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNullItemFeedId() throws Exception {
        item2.withFeedId(null);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNullItemOfferId() throws Exception {
        item2.withOfferId(null);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNullItemOfferName() throws Exception {
        item2.withOfferName(null);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNullItemPrice() throws Exception {
        item2.withPrice(null);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNotNullFeedCategoryId() throws Exception {
        item2.withFeedCategoryId("Камеры");
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNotNullCategoryId() throws Exception {
        item2.withCategoryId(1234);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
     public void testZeroItemFeedId() throws Exception {
        item2.withFeedId(0l);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNegativeItemFeedId() throws Exception {
        item2.withFeedId(-1l);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testEmptyItemOfferId() throws Exception {
        item2.withOfferId("");
        defaultValidate();
    }
    
    @Test(expected = ValidationException.class)
    public void testEmptyItemOfferName() throws Exception {
        item2.withOfferName("");
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testZeroItemPrice() throws Exception {
        item2.withPrice(BigDecimal.ZERO);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNegativeItemPrice() throws Exception {
        item2.withPrice(new BigDecimal(-1));
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNegativeItemCount() throws Exception {
        item2.withCount(-1);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNullDeliveryType() throws Exception {
        delivery2.setType(null);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNullDeliveryServiceId() throws Exception {
        delivery2.setServiceName(null);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNullDeliveryPrice() throws Exception {
        delivery2.setPrice(null);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testDeliveryDatesShouldNotBeNull() throws Exception {
        delivery2.setDeliveryDates(null);
        defaultValidate();
    }

    @Test
    public void testToDateInDeliveryDatesCanBeNull() throws Exception {
        delivery2.setDeliveryDates(new DeliveryDatesBuilder().setToDate(null));
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testFromDateInDeliveryDatesShouldNotBeNull() throws Exception {
        delivery2.setDeliveryDates(deliveryDatesBuilder.setFromDate(null));
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNullDeliveryOutletsWhenTypeIsPickup() throws Exception {
        delivery2.setType(DeliveryType.PICKUP);
        delivery2.setOutletIds(null);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testEmptyDeliveryOutletsWhenTypeIsPickup() throws Exception {
        delivery2.setType(DeliveryType.PICKUP);
        delivery2.setOutletIds(Collections.<Long>emptyList());
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNotNullDeliveryOutletsWhenTypeIsDelivery() throws Exception {
        delivery2.setType(DeliveryType.DELIVERY);
        delivery2.setOutletIds(Collections.<Long>emptyList());
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNotNullDeliveryAddress() throws Exception {
        delivery2.setAddress(new AddressBuilder());
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNotNullDeliveryRegionId() throws Exception {
        delivery2.setRegionId(123l);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNegativeDeliveryPrice() throws Exception {
        delivery2.setPrice(new BigDecimal(-1));
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testZeroDeliveryOutletId() throws Exception {
        delivery2.setOutletIds(asList(1l, 0l));
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNegativeDeliveryOutletId() throws Exception {
        delivery2.setOutletIds(asList(1l, -1l));
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNullPaymentMethods() throws Exception {
        cartResponse.setPaymentMethods(null);
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testEmptyPaymentMethods() throws Exception {
        cartResponse.setPaymentMethods();
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void testNullPaymentMethod() throws Exception {
        cartResponse.setPaymentMethods(null, PaymentMethod.YANDEX_MONEY);
        defaultValidate();
    }

    @Test
    public void offerIdIsNotUnique() throws Exception {
        cartResponse.setItems(
            new OrderItemBuilder()
                .withFeedId(1234l)
                .withOfferId("1234"),
            new OrderItemBuilder()
                .withFeedId(1234l)
                .withOfferId("1235")
        );
        defaultValidate();
    }

    @Test(expected = ValidationException.class)
    public void deliveryIdShouldNotBeLongerThanMaximumLength() throws Exception {
        delivery1.setId(
            "1234567890abcdefghij" + "1234567890abcdefghij" + "1234567890abcdefghij"
            + "1234567890abcdefghij" + "1234567890abcdefghij" + "1234567890abcdefghij"
        );

        defaultValidate();
    }
}
