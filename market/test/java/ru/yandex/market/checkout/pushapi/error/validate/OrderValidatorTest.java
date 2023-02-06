package ru.yandex.market.checkout.pushapi.error.validate;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressBuilder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderBuilder;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItemBuilder;
import ru.yandex.market.checkout.checkouter.order.certificate.ExternalCertificate;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryBuilder;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

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
            .setAddress(new AddressBuilder())
            .setShopAddress(new AddressBuilder());

    private OrderBuilder orderBuilder = new OrderBuilder()
            .withId(1234l)
            .withCurrency(Currency.RUR)
            .withPaymentMethod(PaymentMethod.CASH_ON_DELIVERY)
            .withItems(offerItem, offerItem1)
            .withDelivery(delivery);

    private Order order = orderBuilder.build();

    private AddressValidator addressValidator;

    @BeforeEach
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
        final Address address = new AddressBuilder().withBlock("blaaargh").build();
        order.getDelivery().setShopAddress(address);
        validator.validate(order);
        verify(addressValidator).validate(address);
    }

    @Test
    public void testOrderIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(null);
        });
    }

    // order params

    // order params
    @Test
    public void testOrderIdIsZero() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.setId(0l);
            validator.validate(order);
        });
    }

    @Test
    public void testOrderIdIsNegative() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.setId(-1l);
            validator.validate(order);
        });
    }

    @Test
    public void testCurrencyIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.setCurrency(null);
            validator.validate(order);
        });
    }

    @Test
    public void testCurrencyIsNullAndItemCurrencyNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            OrderItemBuilder offerItem = new OrderItemBuilder()
                    .withFeedId(2345l)
                    .withSupplierCurrency(Currency.RUR)
                    .withOfferId("2345");

            OrderItemBuilder offerItem1 = new OrderItemBuilder()
                    .withFeedId(3456l)
                    .withSupplierCurrency(null)
                    .withOfferId("3456");

            OrderBuilder orderBuilder = new OrderBuilder()
                    .withId(1234l)
                    .withCurrency(Currency.RUR)
                    .withPaymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                    .withItems(offerItem, offerItem1)
                    .withCurrency(null)
                    .withDelivery(delivery);

            Order order = orderBuilder.build();

            validator.validate(order);
        });
    }

    @Test
    public void testCurrencyIsNullAndItemCurrencies() throws Exception {

        OrderItemBuilder offerItem = new OrderItemBuilder()
                .withFeedId(2345l)
                .withSupplierCurrency(Currency.RUR)
                .withOfferId("2345");

        OrderItemBuilder offerItem1 = new OrderItemBuilder()
                .withFeedId(3456l)
                .withSupplierCurrency(Currency.USD)
                .withOfferId("3456");

        OrderBuilder orderBuilder = new OrderBuilder()
                .withId(1234l)
                .withCurrency(Currency.RUR)
                .withPaymentMethod(PaymentMethod.CASH_ON_DELIVERY)
                .withItems(offerItem, offerItem1)
                .withCurrency(null)
                .withDelivery(delivery);

        Order order = orderBuilder.build();

        validator.validate(order);
    }

    @Test
    public void testPaymentMethodIsNullWhenPaymentTypeIsPostpaid() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.setPaymentType(PaymentType.POSTPAID);
            order.setPaymentMethod(null);
            validator.validate(order);
        });
    }

    @Test
    public void testItemsListIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.setItems(null);
            validator.validate(order);
        });
    }

    @Test
    public void testItemsListIsEmpty() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.setItems(Collections.<OrderItem>emptyList());
            validator.validate(order);
        });
    }

    @Test
    public void testDeliveryIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.setDelivery(null);
            validator.validate(order);
        });
    }

    // order items

    // order items
    @Test
    public void testItemFeedIdIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(
                    orderBuilder
                            .withItems(offerItem.withFeedId(null), offerItem1)
                            .build()
            );
        });
    }

    @Test
    public void testItemFeedIdIsZero() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(
                    orderBuilder
                            .withItems(offerItem.withFeedId(0l), offerItem1)
                            .build()
            );
        });
    }

    @Test
    public void testItemFeedIdIsNegative() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(
                    orderBuilder
                            .withItems(offerItem.withFeedId(-1l), offerItem1)
                            .build()
            );
        });
    }

    @Test
    public void testItemOfferIdIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(
                    orderBuilder
                            .withItems(offerItem.withOfferId(null), offerItem1)
                            .build()
            );
        });
    }

    @Test
    public void testItemOfferIdIsEmpty() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(
                    orderBuilder
                            .withItems(offerItem.withOfferId(""), offerItem1)
                            .build()
            );
        });
    }

    @Test()
    public void testItemFeedCategoryIdIsEmptyBlue() throws Exception {
        validator.validate(
                orderBuilder
                        .withItems(offerItem.withFeedCategoryId(""), offerItem1)
                        .withRgb(Color.BLUE)
                        .build()
        );
    }

    @Test()
    public void testItemFeedCategoryIdIsEmptBlueWithoutFulfilment() throws Exception {
        validator.validate(
                orderBuilder
                        .withItems(offerItem.withFeedCategoryId(""), offerItem1)
                        .withRgb(Color.BLUE)
                        .withFulfilment(false)
                        .build()
        );
    }

    @Test()
    public void testItemFeedCategoryIdIsEmptRedFulfilment() throws Exception {
        validator.validate(
                orderBuilder
                        .withItems(offerItem.withFeedCategoryId(""), offerItem1)
                        .withRgb(Color.RED)
                        .withFulfilment(true)
                        .build()
        );
    }


    @Test
    public void testItemOfferNameIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(
                    orderBuilder
                            .withItems(offerItem.withOfferName(null), offerItem1)
                            .build()
            );
        });
    }

    @Test
    public void testItemOfferNameIsEmpty() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(
                    orderBuilder
                            .withItems(offerItem.withOfferName(""), offerItem1)
                            .build()
            );
        });
    }

    @Test
    public void testItemPriceIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(
                    orderBuilder
                            .withItems(offerItem.withPrice(null), offerItem1)
                            .build()
            );
        });
    }

    @Test
    public void testItemPriceIsZero() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(
                    orderBuilder
                            .withItems(offerItem.withPrice(new BigDecimal(0)), offerItem1)
                            .build()
            );
        });
    }

    @Test
    public void testItemPriceIsZeroWithCertificate() throws Exception {
        Order order = orderBuilder
                .withItems(offerItem.withPrice(new BigDecimal(0)), offerItem1)
                .build();
        order.setExternalCertificate(new ExternalCertificate());
        validator.validate(order);
    }

    @Test
    public void testItemPriceIsNegative() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(
                    orderBuilder
                            .withItems(offerItem.withPrice(new BigDecimal(-1)), offerItem1)
                            .build()
            );
        });
    }

    // order delivery

    // order delivery
    @Test
    public void testDeliveryTypeIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.getDelivery().setType(null);
            validator.validate(order);
        });
    }

    @Test
    public void testDeliveryPriceIsNull() {
        Assertions.assertDoesNotThrow(() -> {
            order.getDelivery().setPrice(null);
            validator.validate(order);
        });
    }

    @Test
    public void testDeliveryPriceIsNegative() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.getDelivery().setPrice(new BigDecimal(-1));
            validator.validate(order);
        });
    }

    @Test
    public void testDeliveryServiceIdIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.getDelivery().setServiceName(null);
            validator.validate(order);
        });
    }

    @Test
    public void testDeliveryDatesIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.getDelivery().setDeliveryDates(null);
            validator.validate(order);
        });
    }

    @Test
    public void testToDateInDeliveryDatesIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.getDelivery().getDeliveryDates().setToDate(null);
            validator.validate(order);
        });
    }

    @Test
    public void testFromDateInDeliveryDatesIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.getDelivery().getDeliveryDates().setFromDate(null);
            validator.validate(order);
        });
    }

    @Test
    public void testDeliveryRegionIdIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.getDelivery().setRegionId(null);
            validator.validate(order);
        });
    }

    @Test
    public void testDeliveryAddressIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.getDelivery().setShopAddress(null);
            validator.validate(order);
        });
    }

    @Test
    public void testDeliveryOutletIdsIsNotNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            order.getDelivery().setOutletIds(Collections.emptySet());
            validator.validate(order);
        });
    }

    @Test
    public void validatesNormallyIfPaymentTypeIsNull() throws Exception {
        Order order = orderBuilder.build();
        order.setPaymentType(null);
        validator.validate(order);
    }
}
