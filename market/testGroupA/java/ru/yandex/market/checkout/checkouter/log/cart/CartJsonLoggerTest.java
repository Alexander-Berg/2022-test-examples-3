package ru.yandex.market.checkout.checkouter.log.cart;

import java.io.IOException;
import java.math.BigDecimal;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.carter.InMemoryAppender;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.log.Loggers;
import ru.yandex.market.checkout.checkouter.log.cart.serialization.CartJsonLoggerObjectMapperFactory;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.json.JsonTest;
import ru.yandex.market.request.trace.RequestContextHolder;

public class CartJsonLoggerTest {

    public static final long ORDER_ID = 111111L;
    public static final long SHOP_ID = 222222L;
    public static final long UID = 123123L;
    public static final long REGION_ID = 345L;
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(CartJsonLoggerTest.class);
    private static final Logger logger = (Logger) LoggerFactory.getLogger(Loggers.CART_DIFF_JSON_LOG);
    private static final String MARKET_REQUEST_ID = "TEST_MARKET_REQUEST_ID";
    private InMemoryAppender inMemoryAppender;
    private Level oldLevel;

    @BeforeEach
    public void setUp() {
        RequestContextHolder.createContext(MARKET_REQUEST_ID);
        inMemoryAppender = new InMemoryAppender();
        logger.addAppender(inMemoryAppender);
        inMemoryAppender.clear();
        inMemoryAppender.start();
        oldLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    public void tearDown() {
        logger.detachAppender(inMemoryAppender);
        logger.setLevel(oldLevel);
    }

    @Test
    public void shouldWriteCartDiffLog() {
        String message = writeJson();

        LOG.debug(message);

        JsonTest.checkJson(message, "$.logType", LogType.CART_ACCEPT.name());
        JsonTest.checkJson(message, "$.event", "ITEM_DELIVERY");
        JsonTest.checkJson(message, "$.marketRequestId", MARKET_REQUEST_ID);

        JsonTest.checkJson(message, "$.additionalLoggingInfo.actualBuyerPrice", 345.67);
        JsonTest.checkJson(message, "$.additionalLoggingInfo.originalBuyerCurrency", Currency.RUR.name());
        JsonTest.checkJson(message, "$.additionalLoggingInfo.actualCartItemCount", 456);

        JsonTest.checkJson(message, "$.cart.buyer.uid", "123123");
        JsonTest.checkJson(message, "$.cart.id", String.valueOf(ORDER_ID));
        JsonTest.checkJson(message, "$.cart.shopId", String.valueOf(SHOP_ID));
        JsonTest.checkJson(message, "$.cart.buyerCurrency", Currency.RUR.name());
        JsonTest.checkJson(message, "$.cart.context", Context.MARKET.name());
        JsonTest.checkJson(message, "$.cart.global", true);
        JsonTest.checkJson(message, "$.cart.rgb", Color.BLUE.name());
        JsonTest.checkJson(message, "$.cart.fulfilment", true);
        JsonTest.checkJson(message, "$.cart.acceptMethod", OrderAcceptMethod.PUSH_API.name());

        JsonTest.checkJson(message, "$.cart.items[0].feedOfferId.id", "offerId");
        JsonTest.checkJson(message, "$.cart.items[0].feedOfferId.feedId", 555555);
        JsonTest.checkJson(message, "$.cart.items[0].offerName", "offerName");
        JsonTest.checkJson(message, "$.cart.items[0].buyerPrice", 123.45);
        JsonTest.checkJson(message, "$.cart.items[0].count", 123);
        JsonTest.checkJson(message, "$.cart.items[0].warehouseId", 147);

        JsonTest.checkJson(message, "$.item.offerId", "offerId");
        JsonTest.checkJson(message, "$.item.feedId", 555555);
        JsonTest.checkJson(message, "$.item.feedOfferId.id", "offerId");
        JsonTest.checkJson(message, "$.item.feedOfferId.feedId", 555555);
        JsonTest.checkJson(message, "$.item.offerName", "offerName");
        JsonTest.checkJson(message, "$.item.buyerPrice", 123.45);
        JsonTest.checkJson(message, "$.item.count", 123);
        JsonTest.checkJson(message, "$.item.shopSku", "shopSku");
        JsonTest.checkJson(message, "$.item.msku", 112358);

        JsonTest.checkJson(message, "$.cart.delivery.shopAddress.country", "Россия");
        JsonTest.checkJson(message, "$.cart.delivery.shopAddress.postcode", "123456");
        JsonTest.checkJson(message, "$.cart.delivery.shopAddress.city", "Москва");
        JsonTest.checkJson(message, "$.cart.delivery.shopAddress.street", "Льва Толстого");
        JsonTest.checkJson(message, "$.cart.delivery.shopAddress.building", "16");
        JsonTest.checkJson(message, "$.cart.delivery.regionId", 345);
        JsonTest.checkJson(message, "$.cart.delivery.outletId", 678901);
    }

    @Test
    public void shouldReadWrittenJson() throws IOException {
        OrderItem item = buildItem();
        Order order = buildOrder(item);
        AdditionalLoggingInfo info = buildAdditionalLoggingInfo();

        String json = writeJson();

        DiffLog diffLog = CartJsonLoggerObjectMapperFactory.getObjectMapper()
                .readValue(json, DiffLog.class);

        Assertions.assertEquals(order.getBuyer().getUid(), diffLog.getCart().getBuyer().getUid());
        Assertions.assertEquals(order.getId(), diffLog.getCart().getId());
        Assertions.assertEquals(order.getShopId(), diffLog.getCart().getShopId());
        Assertions.assertEquals(order.getBuyerCurrency(), diffLog.getCart().getBuyerCurrency());
        Assertions.assertEquals(order.getAcceptMethod(), diffLog.getCart().getAcceptMethod());

        OrderItem orderItem = Iterables.get(order.getItems(), 0);
        OrderItem diffLogCartItem = Iterables.get(diffLog.getCart().getItems(), 0);
        compareItem(orderItem, diffLogCartItem);
        compareItem(item, diffLog.getItem());

        Delivery orderDelivery = order.getDelivery();
        Delivery diffLogDelivery = diffLog.getCart().getDelivery();

        Assertions.assertEquals(orderDelivery.getRegionId(), diffLogDelivery.getRegionId());
        Assertions.assertEquals(orderDelivery.getOutletId(), diffLogDelivery.getOutletId());

        Address orderAddress = orderDelivery.getShopAddress();
        Address diffLogAddress = diffLogDelivery.getShopAddress();

        Assertions.assertEquals(orderAddress.getCountry(), diffLogAddress.getCountry());
        Assertions.assertEquals(orderAddress.getPostcode(), diffLogAddress.getPostcode());
        Assertions.assertEquals(orderAddress.getCity(), diffLogAddress.getCity());
        Assertions.assertEquals(orderAddress.getDistrict(), diffLogAddress.getDistrict());
        Assertions.assertEquals(orderAddress.getStreet(), diffLogAddress.getStreet());
        Assertions.assertEquals(orderAddress.getBuilding(), diffLogAddress.getBuilding());

        Assertions.assertEquals(info.getActualBuyerPrice(), diffLog.getAdditionalLoggingInfo().getActualBuyerPrice());
        Assertions.assertEquals(info.getOriginalBuyerCurrency(),
                diffLog.getAdditionalLoggingInfo().getOriginalBuyerCurrency());
        Assertions.assertEquals(info.getActualCartItemCount(),
                diffLog.getAdditionalLoggingInfo().getActualCartItemCount());

        Assertions.assertEquals(MARKET_REQUEST_ID, diffLog.getMarketRequestId());
    }

    @Test
    public void shouldNotFailOnUnknownField() throws IOException {
        String json = "{\"newField\":\"newValue\"}";

        DiffLog difflog = CartJsonLoggerObjectMapperFactory.getObjectMapper()
                .readValue(json, DiffLog.class);
    }

    @Test
    public void shouldNotFailOnUnknownEnumValue() throws IOException {
        String json = "{\"logType\":\"NEW_VALUE\"}";

        DiffLog diffLog = CartJsonLoggerObjectMapperFactory.getObjectMapper()
                .readValue(json, DiffLog.class);

        Assertions.assertEquals(LogType.UNKNOWN, diffLog.getLogType());
    }

    public void compareItem(OrderItem orderItem, OrderItem diffLogItem) {
        Assertions.assertEquals(orderItem.getFeedOfferId(), diffLogItem.getFeedOfferId());
        Assertions.assertEquals(orderItem.getOfferName(), diffLogItem.getOfferName());
        Assertions.assertEquals(orderItem.getBuyerPrice(), diffLogItem.getBuyerPrice());
        Assertions.assertEquals(orderItem.getCount(), diffLogItem.getCount());
        Assertions.assertEquals(orderItem.getAtSupplierWarehouse(), diffLogItem.getAtSupplierWarehouse());
        Assertions.assertEquals(orderItem.getMsku(), diffLogItem.getMsku());
        Assertions.assertEquals(orderItem.getShopSku(), diffLogItem.getShopSku());
    }

    @Test
    public void shouldNotSerializeSnapshotFilters() {
        Order order = OrderProvider.getBlueOrder();

        CartJsonLogger.log(
                LogType.CART_DIFF,
                CartLoggingEvent.ITEM_DELIVERY,
                order,
                Iterables.get(order.getItems(), 0),
                null
        );
    }

    public AdditionalLoggingInfo buildAdditionalLoggingInfo() {
        AdditionalLoggingInfo info = new AdditionalLoggingInfo();
        info.setActualBuyerPrice(new BigDecimal("345.67"));
        info.setOriginalBuyerCurrency(Currency.RUR);
        info.setActualCartItemCount(456);
        return info;
    }

    public OrderItem buildItem() {
        OrderItem item = new OrderItem();
        item.setOfferId("offerId");
        item.setFeedId(555555L);
        item.setWareMd5("wareMd5");
        item.setOfferName("offerName");
        item.setBuyerPrice(new BigDecimal("123.45"));
        item.setCount(123);
        item.setWarehouseId(147);
        item.setAtSupplierWarehouse(true);
        item.setShopSku("shopSku");
        item.setMsku(112358L);
        return item;
    }

    public Order buildOrder(OrderItem item) {
        Buyer buyer = new Buyer();
        buyer.setUid(UID);

        AddressImpl shopAddress = new AddressImpl();
        shopAddress.setCountry("Россия");
        shopAddress.setCity("Москва");
        shopAddress.setStreet("Льва Толстого");
        shopAddress.setBuilding("16");
        shopAddress.setPostcode("123456");

        Delivery delivery = new Delivery(REGION_ID);
        delivery.setShopAddress(shopAddress);
        delivery.setOutletId(678901L);

        Order order = new Order();
        order.setId(ORDER_ID);
        order.setShopId(SHOP_ID);
        order.setBuyer(buyer);
        order.setDelivery(delivery);
        order.setBuyerCurrency(Currency.RUR);
        order.setContext(Context.MARKET);
        order.setGlobal(true);
        order.setRgb(Color.BLUE);
        order.setFulfilment(true);
        order.setAcceptMethod(OrderAcceptMethod.PUSH_API);

        order.addItem(item);
        return order;
    }

    private String writeJson() {
        OrderItem item = buildItem();
        Order order = buildOrder(item);
        AdditionalLoggingInfo info = buildAdditionalLoggingInfo();

        return writeJson(item, order, info);
    }

    private String writeJson(OrderItem item, Order order, AdditionalLoggingInfo info) {
        CartJsonLogger.log(LogType.CART_ACCEPT, CartLoggingEvent.ITEM_DELIVERY, order, item, info);

        ILoggingEvent loggingEvent = Iterables.getOnlyElement(inMemoryAppender.getRaw());
        return loggingEvent.getFormattedMessage();
    }

}
