package ru.yandex.market.checkout.checkouter.order;

import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.helpers.DropshipDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_EMAIL;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_NAME;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_PHONE;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.DELIVERY_ADDRESS;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.DELIVERY_ADDRESS_ONLY_LOCATION;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.DELIVERY_DATES;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.DELIVERY_PRICE;


public class OrderVisibilityTest extends AbstractWebTestBase {

    private Map<OrderVisibility, Boolean> orderVisibilityMap;

    public static Stream<Arguments> parameterizedTestData() {

        return Stream.of(
                new Object[]{null},
                new Object[]{emptyMap()},
                new Object[]{singletonMap(BUYER, Boolean.TRUE)},
                new Object[]{singletonMap(BUYER, Boolean.FALSE)},
                new Object[]{singletonMap(BUYER_EMAIL, Boolean.TRUE)},
                new Object[]{singletonMap(BUYER_EMAIL, Boolean.FALSE)},
                new Object[]{singletonMap(BUYER_NAME, Boolean.TRUE)},
                new Object[]{singletonMap(BUYER_NAME, Boolean.FALSE)},
                new Object[]{singletonMap(BUYER_PHONE, Boolean.TRUE)},
                new Object[]{singletonMap(BUYER_PHONE, Boolean.FALSE)},
                new Object[]{singletonMap(DELIVERY_ADDRESS, Boolean.TRUE)},
                new Object[]{singletonMap(DELIVERY_ADDRESS, Boolean.FALSE)},
// закомменчено, пока фукнциональность видимости DELIVERY_DATES и DELIVERY_PRICE выключена
//                new Object[]{singletonMap(DELIVERY_DATES, Boolean.TRUE)},
//                new Object[]{singletonMap(DELIVERY_DATES, Boolean.FALSE)},
//                new Object[]{singletonMap(DELIVERY_PRICE, Boolean.TRUE)},
//                new Object[]{singletonMap(DELIVERY_PRICE, Boolean.FALSE)},
                new Object[]{ImmutableMap.of(DELIVERY_ADDRESS, false, DELIVERY_ADDRESS_ONLY_LOCATION, true),
                        DELIVERY, false},
                new Object[]{ImmutableMap.of(BUYER_EMAIL, false, BUYER_PHONE, false, DELIVERY_ADDRESS, true)}
        ).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void test(Map<OrderVisibility, Boolean> orderVisibilityMap) {
        this.orderVisibilityMap = orderVisibilityMap;
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        setupOrderVisibility(parameters);

        Order order = createOrder(parameters);
        checkOrderVisibility(order);
    }

    private void setupOrderVisibility(Parameters parameters) {
        Long shopId = parameters.getOrder().getShopId();
        ShopMetaData shopMetaData = parameters.getShopMetaData().get(shopId);
        ShopMetaData tunedShopMetaData = ShopMetaDataBuilder.createCopy(shopMetaData)
                .withOrderVisibilityMap(orderVisibilityMap)
                .build();
        parameters.addShopMetaData(shopId, tunedShopMetaData);
    }

    private Order createOrder(Parameters parameters) {
        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderService.getOrder(order.getId(), ClientInfo.SYSTEM);
        return order;
    }

    private void checkOrderVisibility(Order order) {
        ClientInfo userClient = new ClientInfo(ClientRole.USER, order.getBuyer().getUid());
        ClientInfo shopClient = new ClientInfo(ClientRole.SHOP, order.getShopId());

        checkOrderVisibility(order, userClient);
        checkOrderVisibility(order, shopClient);
    }

    private void checkOrderVisibility(Order origOrder, ClientInfo clientInfo) {
        Long orderId = origOrder.getId();
        Buyer origBuyer = origOrder.getBuyer();
        Delivery origDelivery = origOrder.getDelivery();
        ClientRole clientRole = clientInfo.getRole();

        Order order = orderService.getOrder(orderId, clientInfo);
        Buyer buyer = order.getBuyer();
        Delivery delivery = order.getDelivery();

        checkBuyerVisibility(origBuyer, clientRole, buyer);
        checkDeliveryVisibility(origDelivery, clientRole, delivery);
    }

    private void checkDeliveryVisibility(Delivery origDelivery, ClientRole clientRole, Delivery delivery) {
        if (!isVisible(DELIVERY_ADDRESS, clientRole)) {
            assertNull(delivery.getBuyerAddress());
            assertNull(delivery.getOutlet());
            assertNull(delivery.getOutletCode());
            assertNull(delivery.getOutletId());
            assertNull(delivery.getPostOutlet());
            if (orderVisibilityMap.getOrDefault(DELIVERY_ADDRESS_ONLY_LOCATION, false)
                    && origDelivery.getShopAddress() != null) {
                EqualsBuilder.reflectionEquals(delivery.getShopAddress(), origDelivery.getShopAddress(),
                        "city", "street", "house", "entrance", "floor", "country", "apartment", " gps");
                assertNull(delivery.getShopAddress().getRecipientEmail());
                assertNull(delivery.getShopAddress().getPersonalEmailId());
                assertNull(delivery.getShopAddress().getRecipient());
                assertNull(delivery.getShopAddress().getPersonalFullNameId());
                assertNull(delivery.getShopAddress().getPhone());
                assertNull(delivery.getShopAddress().getPersonalPhoneId());
                assertNull(delivery.getShopAddress().getRecipientPerson());
            } else {
                assertNull(delivery.getShopAddress());
            }
        } else {
            assertNotNull(delivery);

            EqualsBuilder.reflectionEquals(delivery.getBuyerAddress(), origDelivery.getBuyerAddress(),
                    "recipient", "scheduleString", "type", "language", "addressSource", "phone", "personalPhoneId",
                    "email", "personalEmailId", "recipient", "recipientPerson", "personalFullNameId");
            EqualsBuilder.reflectionEquals(delivery.getShopAddress(), origDelivery.getShopAddress(),
                    "recipient", "scheduleString", "type", "language", "addressSource", "phone", "personalPhoneId",
                    "email", "personalEmailId", "recipient", "recipientPerson", "personalFullNameId");

            assertEquals(delivery.getOutlet(), origDelivery.getOutlet());
            assertEquals(delivery.getOutletCode(), origDelivery.getOutletCode());
            assertEquals(delivery.getOutletId(), origDelivery.getOutletId());
            assertEquals(delivery.getPostOutlet(), origDelivery.getPostOutlet());
        }

        if (!isVisible(DELIVERY_DATES, clientRole)) {
            assertNull(delivery.getDeliveryDates());
            assertNull(delivery.getValidatedDeliveryDates());
        } else {
            assertNotNull(delivery);
            assertEquals(delivery.getDeliveryDates(), origDelivery.getDeliveryDates());
            assertEquals(delivery.getValidatedDeliveryDates(), origDelivery.getValidatedDeliveryDates());
        }

        if (!isVisible(DELIVERY_PRICE, clientRole)) {
            assertNull(delivery.getPrice());
            assertNull(delivery.getBuyerPrice());
            assertNull(delivery.getVat());
        } else {
            assertNotNull(delivery);
            assertEquals(delivery.getPrice(), origDelivery.getPrice());
            assertEquals(delivery.getBuyerPrice(), origDelivery.getBuyerPrice());
            assertEquals(delivery.getVat(), origDelivery.getVat());
        }
    }

    private void checkBuyerVisibility(Buyer origBuyer, ClientRole clientRole, Buyer buyer) {
        if (!isVisible(BUYER, clientRole)) {
            assertNull(buyer);
        } else {
            assertNotNull(buyer);
            if (!isVisible(BUYER_EMAIL, clientRole)) {
                assertNull(buyer.getEmail());
                assertNull(buyer.getPersonalEmailId());
            } else {
                assertEquals(buyer.getEmail(), origBuyer.getEmail());
                assertEquals(buyer.getPersonalEmailId(), origBuyer.getPersonalEmailId());
            }
            if (!isVisible(BUYER_PHONE, clientRole)) {
                assertNull(buyer.getPhone());
                assertNull(buyer.getNormalizedPhone());
                assertNull(buyer.getPersonalPhoneId());
            } else {
                assertEquals(buyer.getNormalizedPhone(), origBuyer.getNormalizedPhone());
                assertEquals(buyer.getPersonalPhoneId(), origBuyer.getPersonalPhoneId());
            }
            if (!isVisible(BUYER_NAME, clientRole)) {
                assertNull(buyer.getFirstName());
                assertNull(buyer.getMiddleName());
                assertNull(buyer.getLastName());
                assertNull(buyer.getPersonalFullNameId());
            } else {
                assertEquals(buyer.getFirstName(), origBuyer.getFirstName());
                assertEquals(buyer.getMiddleName(), origBuyer.getMiddleName());
                assertEquals(buyer.getLastName(), origBuyer.getLastName());
                assertEquals(buyer.getPersonalFullNameId(), origBuyer.getPersonalFullNameId());
            }
        }
    }

    private boolean isVisible(OrderVisibility orderVisibility, ClientRole clientRole) {
        if (orderVisibilityMap == null || clientRole != ClientRole.SHOP) {
            return true;
        }
        return orderVisibilityMap.getOrDefault(orderVisibility, true);
    }

    @ParameterizedTest
    @MethodSource("parameterizedTestData")
    public void testDropshipDefaultVisibility(Map<OrderVisibility, Boolean> orderVisibilityMap) throws Exception {
        this.orderVisibilityMap = orderVisibilityMap;
        Parameters parameters = DropshipDeliveryHelper.getDropshipPostpaidParameters();

        this.orderVisibilityMap = ImmutableMap.of(
                BUYER, false,
                DELIVERY_ADDRESS, false,
                DELIVERY_DATES, true,
                DELIVERY_PRICE, true
        );

        Order order = createOrder(parameters);
        checkOrderVisibility(order);
    }

    @Test
    public void checkRecipientPhoneVisibilityPhoneEquals() {
        this.orderVisibilityMap = singletonMap(BUYER_PHONE, Boolean.FALSE);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        setupOrderVisibility(parameters);

        Order initialOrder = createOrder(parameters);

        ClientInfo userClient = new ClientInfo(ClientRole.USER, initialOrder.getBuyer().getUid());
        ClientInfo shopClient = new ClientInfo(ClientRole.SHOP, initialOrder.getShopId());

        Order securedUserOrder = orderService.getOrder(initialOrder.getId(), userClient);
        assertEquals(initialOrder.getDelivery().getShopAddress().getPhone(),
                securedUserOrder.getDelivery().getShopAddress().getPhone());
        assertEquals(initialOrder.getDelivery().getShopAddress().getPersonalPhoneId(),
                securedUserOrder.getDelivery().getShopAddress().getPersonalPhoneId());
        assertEquals(initialOrder.getDelivery().getBuyerAddress().getPhone(),
                securedUserOrder.getDelivery().getBuyerAddress().getPhone());
        assertEquals(initialOrder.getDelivery().getBuyerAddress().getPersonalPhoneId(),
                securedUserOrder.getDelivery().getBuyerAddress().getPersonalPhoneId());

        Order securedShopOrder = orderService.getOrder(initialOrder.getId(), shopClient);
        assertNull(securedShopOrder.getDelivery().getShopAddress().getPhone());
        assertNull(securedShopOrder.getDelivery().getShopAddress().getPersonalPhoneId());
        assertNull(securedShopOrder.getDelivery().getBuyerAddress().getPhone());
        assertNull(securedShopOrder.getDelivery().getBuyerAddress().getPersonalPhoneId());
    }

    @Test
    public void checkRecipientPhoneVisibilityPhoneDiffers() {
        this.orderVisibilityMap = singletonMap(BUYER_PHONE, Boolean.FALSE);
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        Recipient recipient = new Recipient(
                new RecipientPerson("Leo", null, "Tolstoy"), null,
                "+79111111111", "587b9ff2d61de67c32b9aa8ec0014345", "leo@ya.ru", null);
        parameters.getOrder().getDelivery().setRecipient(recipient);
        parameters.getOrder().getDelivery().getBuyerAddress().setPhone(recipient.getPhone());
        parameters.getOrder().getDelivery().getBuyerAddress().setPersonalPhoneId(recipient.getPersonalPhoneId());
        setupOrderVisibility(parameters);

        Order initialOrder = createOrder(parameters);

        ClientInfo userClient = new ClientInfo(ClientRole.USER, initialOrder.getBuyer().getUid());
        ClientInfo shopClient = new ClientInfo(ClientRole.SHOP, initialOrder.getShopId());

        Order securedUserOrder = orderService.getOrder(initialOrder.getId(), userClient);
        assertEquals(initialOrder.getDelivery().getShopAddress().getPhone(),
                securedUserOrder.getDelivery().getShopAddress().getPhone());
        assertEquals(initialOrder.getDelivery().getShopAddress().getPersonalPhoneId(),
                securedUserOrder.getDelivery().getShopAddress().getPersonalPhoneId());
        assertEquals(initialOrder.getDelivery().getBuyerAddress().getPhone(),
                securedUserOrder.getDelivery().getBuyerAddress().getPhone());
        assertEquals(initialOrder.getDelivery().getBuyerAddress().getPersonalPhoneId(),
                securedUserOrder.getDelivery().getBuyerAddress().getPersonalPhoneId());

        Order securedShopOrder = orderService.getOrder(initialOrder.getId(), shopClient);
        assertEquals(initialOrder.getDelivery().getShopAddress().getPhone(),
                securedShopOrder.getDelivery().getShopAddress().getPhone());
        assertEquals(initialOrder.getDelivery().getShopAddress().getPersonalPhoneId(),
                securedShopOrder.getDelivery().getShopAddress().getPersonalPhoneId());
        assertEquals(initialOrder.getDelivery().getBuyerAddress().getPhone(),
                securedShopOrder.getDelivery().getBuyerAddress().getPhone());
        assertEquals(initialOrder.getDelivery().getBuyerAddress().getPersonalPhoneId(),
                securedShopOrder.getDelivery().getBuyerAddress().getPersonalPhoneId());
    }
}
