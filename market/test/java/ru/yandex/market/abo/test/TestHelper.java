package ru.yandex.market.abo.test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang.time.DateUtils;
import org.mockito.stubbing.Answer;

import ru.yandex.common.util.RandomUtils;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.cpa.order.delivery.OrderDelivery;
import ru.yandex.market.abo.cpa.order.model.CpaOrderStat;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;
import ru.yandex.market.checkout.checkouter.returns.ReturnStatus;
import ru.yandex.market.checkout.entity.Conversation;
import ru.yandex.market.checkout.entity.ConversationStatus;
import ru.yandex.market.checkout.entity.OrderInfo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static ru.yandex.market.checkout.checkouter.order.Color.RED;

/**
 * Класс для генерации часто используемых фейковых моделей: оффер, магазин, данные из фида, и тд.
 *
 * @author kukabara
 */
public class TestHelper {
    private static final ThreadLocalRandom RND = ThreadLocalRandom.current();
    private static final Date NOW = new Date();

    public static final List<Long> TEST_SHOPS = Arrays.asList(5198L, 86358L, 62666L, 211L, 3828L, 211L, 774L);

    // эти магазины есть в v_shop
    public static long generateShopId() {
        return TEST_SHOPS.get(RandomUtils.nextIntInRange(0, TEST_SHOPS.size() - 1));
    }

    public static Offer generateOffer() {
        return generateOffer(RND.nextInt(1000));
    }

    public static Offer generateOffer(long shopId) {
        Offer reportOffer = new Offer();
        reportOffer.setShopId(shopId);
        reportOffer.setName("test7 Apple iPod touch 4 32Gb");
        reportOffer.setShopOfferId("asdf");
        reportOffer.setHyperId(-1L);
        reportOffer.setUrl("http://yandex.ru");
        reportOffer.setUrlHash("906151190");
        reportOffer.setWareMd5("KQOw-jPOLvP9XEQ9ckpkKQ");
        reportOffer.setClassifierMagicId("5c1491e18fadd52ec7a96128fff4b081");
        reportOffer.setFeedId(1235L);
        reportOffer.setFeedCategoryId("category");
        reportOffer.setPriceCurrency(ru.yandex.common.util.currency.Currency.RUR);
        reportOffer.setPrice(new BigDecimal(1234d));
        reportOffer.setFeeShow("4zINsNPJIb6z_lNBs0ZO7uuFDJvAfXpNGszsU4DkhnKKz-fCWMXNIQ");
        reportOffer.setPriorityRegionId(213L);
        reportOffer.setOnStock(true);
        reportOffer.setBaseGeneration("generation");

        return reportOffer;
    }

    public static long generateId() {
        return Math.abs(RND.nextLong());
    }

    public static int generateIntId() {
        return Math.abs(RND.nextInt());
    }

    public static Order generateOrder(Long id) {
        return generateOrder(id, generateId(), generateId(), RND.nextBoolean());
    }

    public static Order generateOrder(Long id, Long shopId, Long userId, boolean fake) {
        return generateOrder(id, shopId, userId, fake, OrderStatus.DELIVERY);
    }

    public static Order generateOrder(Long id, Long shopId, Long userId, boolean fake, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setShopOrderId("ShopOrderId-" + id);
        order.setShopId(shopId);
        order.setUid(userId);
        order.setFake(fake);
        order.setBuyer(generateBuyer(userId));
        order.setItems(Collections.singletonList(generateItem()));
        order.setStatus(status);
        order.setBuyerTotal(BigDecimal.valueOf(1100));

        Delivery delivery = generateDelivery();
        order.setDelivery(delivery);
        order.setDeliveryOptions(Collections.singletonList(delivery));
        order.setCreationDate(NOW);
        order.setBuyerItemsTotal(new BigDecimal(100.5));
        order.setBuyerTotal(new BigDecimal(100.5));
        order.setNoAuth(false);
        order.setContext(Context.MARKET);
        order.setRgb(Color.GREEN);

        order.setUserGroup(UserGroup.DEFAULT);
        order.setPaymentMethod(PaymentMethod.BANK_CARD);
        order.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        order.setItemsTotal(BigDecimal.ONE);
        order.setTotal(BigDecimal.ONE);
        order.setRefundActual(BigDecimal.ONE);

        return order;
    }


    public static Delivery generateDelivery() {
        Delivery delivery = new Delivery(DeliveryType.DELIVERY, BigDecimal.valueOf(300), "service",
                new DeliveryDates(DateUtils.addDays(NOW, 2), DateUtils.addDays(NOW, 3)),
                new AddressImpl(), 213L, null);

        AddressImpl buyerAddress = new AddressImpl();
        buyerAddress.setCity("Москва");
        buyerAddress.setStreet("ул. Льва Толстого");
        buyerAddress.setBuilding("д.16");
        delivery.setBuyerAddress(buyerAddress);
        delivery.setAddress(buyerAddress);

        delivery.setPaymentOptions(new HashSet<>(
                Arrays.asList(PaymentMethod.YANDEX, PaymentMethod.CASH_ON_DELIVERY))
        );
        delivery.setParcels(List.of(generateParcel()));
        delivery.setPrice(new BigDecimal(RND.nextInt(1000)));
        return delivery;
    }

    public static Parcel generateParcel() {
        Parcel parcel = new Parcel();
        parcel.setShipmentDate(LocalDate.now());
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.now());
        parcel.setWeight(100L);
        parcel.addTrack(new Track("trackCode", 145L));
        return parcel;
    }

    public static OrderHistoryEvent generateEvent(Order order, Date fromDate,
                                                  OrderStatus statusBefore, OrderStatus statusAfter) {
        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.ORDER_STATUS_UPDATED);
        if (statusBefore != null) {
            event.setOrderBefore(order);
            event.getOrderBefore().setStatus(statusBefore);
            event.getOrderBefore().setSubstatus(statusBefore != OrderStatus.CANCELLED ? null : OrderSubstatus.USER_REFUSED_PRODUCT);
        }
        event.setOrderAfter(order);
        event.getOrderAfter().setStatus(statusAfter);
        event.getOrderAfter().setSubstatus(statusAfter != OrderStatus.CANCELLED ? null : OrderSubstatus.USER_REFUSED_PRODUCT);
        event.setFromDate(fromDate);
        ClientInfo systemClientInfo = new ClientInfo(ClientRole.SYSTEM, 1L);
        ClientInfo shopClientInfo = new ClientInfo(ClientRole.SHOP_USER, order.getShopId());
        event.setAuthor(statusBefore != null ? shopClientInfo : systemClientInfo);
        return event;
    }

    private static Buyer generateBuyer(Long userId) {
        Buyer b = new Buyer();
        b.setUid(userId);
        b.setFirstName("Имя");
        b.setLastName("Фамилия");
        b.setPhone("+7-915-123-4567");
        return b;
    }

    public static OrderItem generateItem() {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(RND.nextLong(10_000L));
        orderItem.setCount(1);
        orderItem.setDelivery(true);
        BigDecimal price = BigDecimal.valueOf(RND.nextInt(1000, 10_000));
        orderItem.setBuyerPrice(price);
        orderItem.setPrice(price);
        orderItem.setModelId(RND.nextLong(10_000L));
        orderItem.setCategoryId(RND.nextInt(10_000));
        orderItem.setOfferName("offer_name_" + RND.nextInt(10));
        orderItem.setOfferId("offer_id_" + RND.nextInt(10_000));
        orderItem.setFeedId(RND.nextLong(1000));
        orderItem.setShopSku("shop_sku_" + RND.nextInt(10_000));
        orderItem.setSupplierId(RND.nextLong(10_000));
        return orderItem;
    }

    public static Conversation generateConv(ConversationStatus status) {
        return generateConv(generateIntId(), generateIntId(), generateId(), status);
    }

    private static Conversation generateConv(long id, long shopId, long userId, ConversationStatus status) {
        Conversation conv = new Conversation();
        conv.setId(id);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(generateId());
        orderInfo.setUid(userId);
        orderInfo.setShopId(shopId);
        conv.setOrder(orderInfo);
        conv.setLastStatus(status);
        return conv;
    }

    public static CpaOrderStat generateCpaOrderStat(long orderId, long shopId, boolean noAuth, LocalDateTime processing, Color color) {
        CpaOrderStat cpaOrderStat = new CpaOrderStat();
        cpaOrderStat.setOrderId(orderId);
        cpaOrderStat.setUserId(RND.nextInt(100));
        cpaOrderStat.setShopId(shopId);
        cpaOrderStat.setCreationDate(NOW);
        cpaOrderStat.setDeliveryType(DeliveryType.DELIVERY);
        Optional.ofNullable(processing).map(DateUtil::asDate).ifPresent(cpaOrderStat::setProcessing);
        cpaOrderStat.setRgb(color);
        return cpaOrderStat;
    }

    public static CpaOrderStat generateCpaOrderStat(long orderId, long shopId, Color color, Date creationDate, OrderStatus orderStatus, DeliveryType deliveryType) {
        CpaOrderStat cpaOrderStat = new CpaOrderStat();
        cpaOrderStat.setOrderId(orderId);
        cpaOrderStat.setUserId(RND.nextInt(100));
        cpaOrderStat.setShopId(shopId);
        cpaOrderStat.setCreationDate(creationDate);
        cpaOrderStat.setStatus(orderStatus);
        cpaOrderStat.setDeliveryType(deliveryType);
        cpaOrderStat.setProcessing(NOW);
        cpaOrderStat.setRgb(color);
        return cpaOrderStat;
    }

    public static OrderDelivery generateCpaOrderDelivery(long orderId, Date byOrder, DeliveryPartnerType deliveryPartnerType) {
        OrderDelivery orderDelivery = new OrderDelivery();
        orderDelivery.setOrderId(orderId);
        orderDelivery.setByOrder(byOrder);
        orderDelivery.setDeliveryPartnerType(deliveryPartnerType);
        return orderDelivery;
    }

    public static List<CpaOrderStat> generateCpaOrderStatList(long shopId) {
        List<CpaOrderStat> cpaOrderStatList = new ArrayList<>();

        CpaOrderStat cpaOrderStat1 = generateCpaOrderStat(1, shopId, true, null, RED);
        cpaOrderStatList.add(cpaOrderStat1);

        CpaOrderStat cpaOrderStat2 = generateCpaOrderStat(2, shopId, true, null, RED);
        cpaOrderStatList.add(cpaOrderStat2);

        CpaOrderStat cpaOrderStat3 = generateCpaOrderStat(3, shopId, false, null, RED);
        cpaOrderStat3.setCreationDate(Date.from(LocalDateTime.now().minusDays(2).atZone(ZoneId.systemDefault()).toInstant()));
        cpaOrderStatList.add(cpaOrderStat3);

        CpaOrderStat cpaOrderStat4 = generateCpaOrderStat(4, shopId, false, null, RED);
        cpaOrderStatList.add(cpaOrderStat4);

        return cpaOrderStatList;
    }

    @SuppressWarnings("unchecked")
    public static <T> Answer<T> identityAnswer() {
        return invocation -> (T) invocation.getArguments()[0];
    }

    public static Return generateReturn(long orderId, ReturnStatus returnStatus, ReturnReasonType reasonType) {
        Return orderReturn = new Return();
        orderReturn.setOrderId(orderId);
        orderReturn.setCreationDate(new Date());
        orderReturn.setUpdatedDate(new Date());
        orderReturn.setStatus(returnStatus);
        orderReturn.setItems(List.of(generateReturnItem(reasonType)));
        return orderReturn;
    }

    public static ReturnItem generateReturnItem(ReturnReasonType reasonType) {
        ReturnItem returnItem = new ReturnItem(1L, 1, false, new BigDecimal(100));
        returnItem.setReasonType(reasonType);
        return returnItem;
    }

    public static void mockExecutorService(ExecutorService... mocks) {
        for (var mock : mocks) {
            doAnswer(invocation -> {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            }).when(mock).execute(any());
        }
    }
}
