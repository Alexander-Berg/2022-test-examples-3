package ru.yandex.market.checkout.pushapi.error.validate;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressBuilder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderBuilder;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemBuilder;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryBuilder;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OrderValidatorTest {

    private OrderValidator validator = new OrderValidator();

    private OrderItemBuilder offerItem = new OrderItemBuilder()
        .withFeedId(2345l)
        .withOfferId("2345");
    private OrderItemBuilder offerItem1 = new OrderItemBuilder()
        .withFeedId(3456l)
        .withOfferId("3456");

    private DeliveryBuilder delivery = new DeliveryBuilder()
        .setRegionId(213l)
        .setAddress(new AddressBuilder());

    private OrderBuilder orderBuilder = new OrderBuilder()
        .withId(1234l)
        .withCurrency(Currency.RUR)
        .withPaymentType(PaymentType.POSTPAID)
        .withPaymentMethod(PaymentMethod.CASH_ON_DELIVERY)
        .withItems(offerItem, offerItem1)
        .withDelivery(delivery);

    private Order order = orderBuilder.build();

    private AddressValidator addressValidator;

    @Before
    public void setUp() throws Exception {
        addressValidator = mock(AddressValidator.class);
        validator.setAddressValidator(addressValidator);
    }



    @Test
    public void testOk() throws Exception {
        validator.validate(order);
    }

    @Test
    public void testNonNullAddressIsValidatedWithAddressValidator() throws Exception {
        final Address address = new Address();
        address.setBlock("blaaargh");
        order.getDelivery().setAddress(address);
        validator.validate(order);
        verify(addressValidator).validate(address);
    }

    @Test(expected = ValidationException.class)
    public void testOrderIsNull() throws Exception {
        validator.validate(null);
    }

    // order params

    @Test(expected = ValidationException.class)
    public void testOrderIdIsZero() throws Exception {
        order.setId(0l);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testOrderIdIsNegative() throws Exception {
        order.setId(-1l);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testCurrencyIsNull() throws Exception {
        order.setCurrency(null);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testPaymentMethodIsNullWhenPaymentTypeIsPostpaid() throws Exception {
        order.setPaymentType(PaymentType.POSTPAID);
        order.setPaymentMethod(null);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testItemsListIsNull() throws Exception {
        order.setItems(null);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testItemsListIsEmpty() throws Exception {
        order.setItems(Collections.<OrderItem>emptyList());
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testDeliveryIsNull() throws Exception {
        order.setDelivery(null);
        validator.validate(order);
    }

    // order items

    @Test(expected = ValidationException.class)
    public void testItemFeedIdIsNull() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withFeedId(null), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemFeedIdIsZero() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withFeedId(0l), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemFeedIdIsNegative() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withFeedId(-1l), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemOfferIdIsNull() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withOfferId(null), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemOfferIdIsEmpty() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withOfferId(""), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemFeedCategoryIdIsNull() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withFeedCategoryId(null), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemFeedCategoryIdIsEmpty() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withFeedCategoryId(""), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemOfferNameIsNull() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withOfferName(null), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemOfferNameIsEmpty() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withOfferName(""), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemPriceIsNull() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withPrice(null), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemPriceIsZero() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withPrice(new BigDecimal(0)), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemPriceIsNegative() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withPrice(new BigDecimal(-1)), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemCountIsZero() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withCount(0), offerItem1)
                .build()
        );
    }

    @Test(expected = ValidationException.class)
    public void testItemCountIsNegative() throws Exception {
        validator.validate(
            orderBuilder
                .withItems(offerItem.withCount(-1), offerItem1)
                .build()
        );
    }

    // order delivery

    @Test(expected = ValidationException.class)
    public void testDeliveryTypeIsNull() throws Exception {
        order.getDelivery().setType(null);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testDeliveryPriceIsNull() throws Exception {
        order.getDelivery().setPrice(null);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testDeliveryPriceIsNegative() throws Exception {
        order.getDelivery().setPrice(new BigDecimal(-1));
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testDeliveryServiceIdIsNull() throws Exception {
        order.getDelivery().setServiceName(null);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testDeliveryDatesIsNull() throws Exception {
        order.getDelivery().setDeliveryDates(null);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testToDateInDeliveryDatesIsNull() throws Exception {
        order.getDelivery().getDeliveryDates().setToDate(null);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testFromDateInDeliveryDatesIsNull() throws Exception {
        order.getDelivery().getDeliveryDates().setFromDate(null);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testDeliveryRegionIdIsNull() throws Exception {
        order.getDelivery().setRegionId(null);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testDeliveryAddressIsNull() throws Exception {
        order.getDelivery().setAddress(null);
        validator.validate(order);
    }

    @Test(expected = ValidationException.class)
    public void testDeliveryOutletIdsIsNotNull() throws Exception {
        order.getDelivery().setOutletIds(Collections.<Long>emptyList());
        validator.validate(order);
    }

    @Test
    public void validatesNormallyIfPaymentTypeIsNull() throws Exception {
        validator.validate(
            orderBuilder
                .withPaymentType(null)
                .build()
        );
    }
}
