package ru.yandex.market.checkout.pushapi.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.shop.OrderVisibility;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaDataRwService;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.test.providers.AddressProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.PICKUP;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_EMAIL;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_NAME;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_PHONE;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.BUYER_UID;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.DELIVERY_ADDRESS;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.DELIVERY_DATES;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.DELIVERY_PRICE;
import static ru.yandex.market.checkout.checkouter.shop.OrderVisibility.IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES;
import static ru.yandex.market.checkout.pushapi.web.JsonUtil.getByPath;

abstract class AbstractOrderVisibilityTestBase extends AbstractWebTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(OrderAcceptTest.class);
    private static final String UTF8 = StandardCharsets.UTF_8.name();

    @Autowired
    private WireMockServer shopadminStubMock;
    @Autowired
    private ShopMetaDataRwService shopService;

    void checkOrderVisibility(boolean dropship, boolean showBuyer, DeliveryType deliveryType,
                              Map<OrderVisibility, Boolean> orderVisibilityMap) throws IOException {
        byte[] body = getPushApiRequestBody();
        ObjectMapper objectMapper = new ObjectMapper();

        var order = (Map<Object, Object>) objectMapper.readValue(body, Map.class);
        checkBuyerVisibility(order, "/order/buyer", dropship || !showBuyer, orderVisibilityMap);
        checkDeliveryVisibility(order, false, dropship, deliveryType, orderVisibilityMap);
    }

    void checkCartVisibility(boolean dropship, boolean showBuyer, DeliveryType deliveryType,
                             Map<OrderVisibility, Boolean> orderVisibilityMap) throws IOException {
        byte[] body = getPushApiRequestBody();
        ObjectMapper objectMapper = new ObjectMapper();

        var cart = (Map<Object, Object>) objectMapper.readValue(body, Map.class);

        checkBuyerVisibility(cart, "/cart/buyer", dropship || !showBuyer, orderVisibilityMap);
        checkDeliveryVisibility(cart, true, dropship, deliveryType, orderVisibilityMap);
    }

    private void checkBuyerVisibility(Map<Object, Object> jsonMap, String path, boolean withoutBuyer,
                                      Map<OrderVisibility, Boolean> orderVisibilityMap) {
        try {
            if (isHidden(BUYER, orderVisibilityMap) || withoutBuyer) {
                assertNull(getByPath(jsonMap, path));
            } else {
                assertNotNull(getByPath(jsonMap, path));

                if (isHidden(BUYER_UID, orderVisibilityMap)) {
                    assertNull(getByPath(jsonMap, path + "/uid"));
                } else {
                    assertNotNull(getByPath(jsonMap, path + "/uid"));
                }
                if (isHidden(BUYER_EMAIL, orderVisibilityMap)) {
                    assertNull(getByPath(jsonMap, path + "/email"));
                } else {
                    assertNotNull(getByPath(jsonMap, path + "/email"));
                }
                if (isHidden(BUYER_PHONE, orderVisibilityMap)) {
                    assertNull(getByPath(jsonMap, path + "/phone"));
                } else {
                    assertNotNull(getByPath(jsonMap, path + "/phone"));
                }
                if (isHidden(BUYER_NAME, orderVisibilityMap)) {
                    assertNull(getByPath(jsonMap, path + "/firstName"));
                    assertNull(getByPath(jsonMap, path + "/lastName"));
                } else {
                    assertNotNull(getByPath(jsonMap, path + "/firstName"));
                    assertNotNull(getByPath(jsonMap, path + "/lastName"));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private void checkDeliveryVisibility(Map<Object, Object> jsonMap, boolean deliveryForCart,
                                         boolean dropship, DeliveryType deliveryType,
                                         Map<OrderVisibility, Boolean> orderVisibilityMap) {
        try {
            String path = (deliveryForCart ? "/cart" : "/order") + "/delivery";
            if (isHidden(DELIVERY_ADDRESS, orderVisibilityMap) || dropship) {
                assertNull(getByPath(jsonMap, path + "/address"));
                assertNull(getByPath(jsonMap, path + "/outlet"));
            } else {
                if (deliveryType == DELIVERY) {
                    assertNotNull(getByPath(jsonMap, path + "/address"));
                } else {
                    assertNotNull(getByPath(jsonMap, path + "/outlet"));
                }
            }
            if (!deliveryForCart) {
                if (isHidden(DELIVERY_DATES, orderVisibilityMap)) {
                    assertNull(getByPath(jsonMap, path + "/dates"));
                } else {
                    assertNotNull(getByPath(jsonMap, path + "/dates"));
                }
                if (isHidden(DELIVERY_PRICE, orderVisibilityMap)) {
                    assertNull(getByPath(jsonMap, path + "/price"));
                    assertNull(getByPath(jsonMap, path + "/vat"));
                } else {
                    assertNotNull(getByPath(jsonMap, path + "/price"));
                    assertNotNull(getByPath(jsonMap, path + "/vat"));
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private Boolean isHidden(OrderVisibility orderVisibility, Map<OrderVisibility, Boolean> orderVisibilityMap) {
        if (orderVisibilityMap == null) {
            return false;
        }
        return !orderVisibilityMap.getOrDefault(orderVisibility, true);
    }

    boolean isShowBuyer(boolean dropship, Map<OrderVisibility, Boolean> orderVisibilityMap) {
        return ShopMetaData.getOrderVisibility(orderVisibilityMap, IGNORE_PERSONAL_DATA_HIDING_FOR_EARLY_STATUSES)
                .orElse(false) &&
                ShopMetaData.getOrderVisibility(orderVisibilityMap, BUYER).orElse(!dropship);
    }

    void prepareDelivery(Delivery delivery, DeliveryType deliveryType) {
        if (deliveryType == PICKUP) {
            delivery.setType(PICKUP);
            delivery.setOutletId(6789L);
            delivery.setOutletCode("6789");
            delivery.setOutlet(prepareOutlet());
            delivery.setShopAddress(null);
            delivery.setBuyerAddress(null);
            delivery.setAddress(null);
        } else if (deliveryType == DELIVERY) {
            if (delivery.getShopAddress() == null) {
                Address address = AddressProvider.getAddress();
                address.setType(AddressType.SHOP);
                delivery.setShopAddress(address);
            }
        }
    }

    void prepareDropshipOrder(Order order, boolean dropship) {
        if (dropship) {
            order.setRgb(Color.BLUE);
            order.setFulfilment(false);
            order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        }
    }

    void prepareClickAndCollectOrder(Order order) {
        order.setRgb(Color.BLUE);
        order.setFulfilment(false);
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
    }

    void prepareDropshipCart(Cart cart, boolean dropship) {
        if (dropship) {
            cart.setRgb(Color.BLUE);
            cart.setFulfilment(false);
            cart.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        }
    }

    void prepareClickAndCollectCart(Cart cart) {
        cart.setRgb(Color.BLUE);
        cart.setFulfilment(false);
        cart.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.SHOP);
    }

    void prepareShopMetaData(long shopId, Map<OrderVisibility, Boolean> orderVisibilityMap) {
        ShopMetaData shopMetaData = ShopMetaDataBuilder.createTestDefault()
                .withOrderVisibilityMap(orderVisibilityMap)
                .build();
        shopService.updateMeta(shopId, shopMetaData);
    }

    private ShopOutlet prepareOutlet() {
        ShopOutlet outlet = new ShopOutlet();
        outlet.setId(6789L);
        outlet.setCity("Москва");
        outlet.setStreet("ул. Льва Толстого");
        outlet.setHouse("18Б");
        return outlet;
    }

    private LoggedRequest extractPushApiRequest() {
        List<ServeEvent> serveEvents = shopadminStubMock.getAllServeEvents();
        assertThat(serveEvents, hasSize(1));
        ServeEvent event = Iterables.getOnlyElement(serveEvents);
        return event.getRequest();
    }

    private byte[] getPushApiRequestBody() {
        LoggedRequest request = extractPushApiRequest();
        LOG.debug("request.getBodyAsString()={}", request.getBodyAsString());
        return request.getBody();
    }
}
