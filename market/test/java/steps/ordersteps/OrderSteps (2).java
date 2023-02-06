package steps.orderSteps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.util.Lists;
import steps.ParcelSteps;
import steps.orderSteps.itemSteps.ItemsSteps;
import steps.utils.DateUtils;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.common.report.model.DeliveryMethod;

public class OrderSteps {
    public static final long ID = 123L;
    private static final long UID = 123L;
    private static final long MUID = 123L;
    private static final long SHOP_ID = 10210930L;
    private static final Currency CURRENCY = Currency.RUR;
    private static final Currency BUYER_CURRENCY = Currency.AFN;
    private static final BigDecimal EXCHANGE_RATE = BigDecimal.valueOf(123);
    private static final BigDecimal TOTAL = BigDecimal.valueOf(123);
    private static final BigDecimal BUYER_TOTAL = BigDecimal.valueOf(123);
    private static final BigDecimal ITEMS_TOTAL = BigDecimal.valueOf(123);
    private static final BigDecimal BUYER_ITEMS_TOTAL = BigDecimal.valueOf(123);
    private static final BigDecimal FEE_TOTAL = BigDecimal.valueOf(123);
    private static final PaymentType PAYMENT_TYPE = PaymentType.POSTPAID;
    private static final PaymentMethod PAYMENT_METHOD = PaymentMethod.BANK_CARD;
    private static final OrderStatus STATUS = OrderStatus.UNPAID;
    private static final OrderSubstatus SUB_STATUS = OrderSubstatus.SHOP_FAILED;
    private static final Date DATE = DateUtils.getDate();
    private static final boolean FAKE = false;
    private static final String NOTES = "notes";
    private static final String SHOP_ORDER_ID = "123456";
    private static final UserGroup USER_GROUP = UserGroup.DEFAULT;
    private static final long PAYMENT_ID = 123L;
    private static final String BALANCE_ORDER_ID = "666";
    private static final BigDecimal REFUND_PLANNED = BigDecimal.valueOf(123);
    private static final boolean NO_AUTH = false;
    private static final OrderAcceptMethod ACCEPT_METHOD = OrderAcceptMethod.WEB_INTERFACE;
    private static final BigDecimal REAL_TOTAL = BigDecimal.valueOf(123);
    private static final boolean GLOBAL = false;
    private static final HashSet<CartChange> CART_CHANGES = new HashSet<>();
    private static final Set<PaymentMethod> PAYMENT_OPTIONS = Collections.singleton(PaymentMethod.BANK_CARD);
    private static final ArrayList<Delivery> DELIVERY_OPTIONS = new ArrayList<>();
    private static final Buyer BUYER = BuyerSteps.getBuyer();
    private static final Payment PAYMENT = PaymentSteps.getPayment();
    private static final ArrayList<ValidationResult> VALIDATION_RESULTS = new ArrayList<>();
    private static final BigDecimal REFUND_ACTUAL = BigDecimal.valueOf(123);
    private static final Context CONTEXT = Context.MARKET;
    private static final long INTERVAL_DELIVERY_ID = 123;
    private static final BigDecimal SUBSIDY_REFUND_ACTUAL = BigDecimal.valueOf(123);
    private static final BigDecimal SUBSIDY_REFUND_PLANNED = BigDecimal.valueOf(123);
    private static final String SUBSIDY_BALANCE_ORDER_ID = "123";
    private static final String SIGNATURE = "signature";
    private static final String SHOP_NAME = "shop name";
    private static final long SUBSIDY_ID = 123;
    private static final TaxSystem TAX_SYSTEM = TaxSystem.ENVD;

    private OrderSteps() {
    }

    public static Order getFilledOrder() {
        return getFilledOrder(ID);
    }

    public static Order getFilledOrder(int numberOfParcels) {
        return getFilledOrder(ID, numberOfParcels);
    }

    public static Order getFilledOrder(long orderId) {
        return getFilledOrder(orderId, 1);
    }

    public static Order getFilledOrder(long orderId, int numberOfParcels) {
        Order order = new Order();

        order.setId(orderId);
        order.setUid(UID);
        order.setMuid(MUID);
        order.setShopId(SHOP_ID);
        order.setCurrency(CURRENCY);
        order.setBuyerCurrency(BUYER_CURRENCY);
        order.setExchangeRate(EXCHANGE_RATE);
        order.setTotal(TOTAL);
        order.setBuyerTotal(BUYER_TOTAL);
        order.setItemsTotal(ITEMS_TOTAL);
        order.setBuyerItemsTotal(BUYER_ITEMS_TOTAL);
        order.setFeeTotal(FEE_TOTAL);
        order.setPaymentType(PAYMENT_TYPE);
        order.setPaymentMethod(PAYMENT_METHOD);
        order.setStatus(STATUS);
        order.setSubstatus(SUB_STATUS);
        order.setCreationDate(DATE);
        order.setUpdateDate(DATE);
        order.setStatusUpdateDate(DATE);
        order.setStatusExpiryDate(DATE);
        order.setFake(FAKE);
        order.setNotes(NOTES);
        order.setShopOrderId(SHOP_ORDER_ID);
        order.setUserGroup(USER_GROUP);
        order.setPaymentId(PAYMENT_ID);
        order.setBalanceOrderId(BALANCE_ORDER_ID);
        order.setRefundPlanned(REFUND_PLANNED);
        order.setNoAuth(NO_AUTH);
        order.setAcceptMethod(ACCEPT_METHOD);
        order.setRealTotal(REAL_TOTAL);
        order.setGlobal(GLOBAL);
        order.setItems(ItemsSteps.getOrderItemsList());
        order.setChanges(CART_CHANGES);
        order.setPaymentOptions(PAYMENT_OPTIONS);
        order.setDelivery(DeliverySteps.getDelivery(numberOfParcels));
        order.setDeliveryOptions(DELIVERY_OPTIONS);
        order.setBuyer(BUYER);
        order.setPayment(PAYMENT);
        order.setValidationErrors(VALIDATION_RESULTS);
        order.setValidationWarnings(VALIDATION_RESULTS);
        order.setRefundActual(REFUND_ACTUAL);
        order.setDeliveryCurrency(CURRENCY);
        order.setContext(CONTEXT);
        order.setInternalDeliveryId(INTERVAL_DELIVERY_ID);
        order.setSubsidyRefundActual(SUBSIDY_REFUND_ACTUAL);
        order.setSubsidyRefundPlanned(SUBSIDY_REFUND_PLANNED);
        order.setSubsidyBalanceOrderId(SUBSIDY_BALANCE_ORDER_ID);
        order.setSignature(SIGNATURE);
        order.setChanges(Collections.emptySet());
        order.setShopName(SHOP_NAME);
        order.setSubsidyId(SUBSIDY_ID);
        order.setDeliveryMethods(DeliveryMethodsSteps.getDeliveryMethodsMap());
        order.setTaxSystem(TAX_SYSTEM);

        return order;
    }

    public static Order getNotFakeOrder() {
        Order order = getFilledOrder();

        Parcel parcel = new Parcel();
        parcel.setTracks(Lists.newArrayList(getTrack(
            order.getId(),
            123L,
            order.getDelivery().getDeliveryServiceId()
        )));

        order.getDelivery().setParcels(Lists.newArrayList(parcel));
        order.getDelivery().setDeliveryServiceId(123L); //any delivery serviceId
        order.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);

        return order;
    }

    public static Order getOrderWithParcelBoxes() {
        Order order = getFilledOrder();
        order.getDelivery().getParcels().forEach(parcel -> parcel.setBoxes(getBoxes()));
        return order;
    }

    private static List<ParcelBox> getBoxes() {
        List<ParcelBox> boxes = new ArrayList<>();
        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setWeight(100L);
        parcelBox.setHeight(100L);
        parcelBox.setDepth(100L);
        parcelBox.setWidth(100L);
        parcelBox.setFulfilmentId("EXTFFID");
        boxes.add(parcelBox);
        return boxes;
    }

    public static Order getRedSingleOrder() {
        Order order = getFilledOrder();

        order.setGlobal(true);
        order.setRgb(Color.RED);
        order.setStatus(OrderStatus.PROCESSING);

        return order;
    }

    public static Order getRedMultipleOrder() {
        Order order = getFilledOrder();

        Parcel parcel = ParcelSteps.getParcel();
        parcel.setTracks(DeliverySteps.TrackSteps.getTracksList());
        parcel.setId(33L);

        order.getDelivery().addParcel(parcel);

        order.setRgb(Color.RED);
        order.setStatus(OrderStatus.PROCESSING);

        return order;
    }

    public static Order getRedMultipleOrderWithOneParcelWithoutTracks() {
        Order order = getFilledOrder();

        Parcel parcel = ParcelSteps.getParcel();
        parcel.setId(33L);

        order.getDelivery().addParcel(parcel);

        order.setRgb(Color.RED);
        order.setStatus(OrderStatus.PROCESSING);

        return order;
    }

    public static void setOrderDimension(Order order) {
        order.getDelivery().getParcels().get(0).setWidth(10L);
        order.getDelivery().getParcels().get(0).setHeight(10L);
        order.getDelivery().getParcels().get(0).setDepth(10L);
    }

    public static void setOrderWeight(Order order) {
        order.getDelivery().getParcels().get(0).setWeight(1L);
    }

    public static void setItemsAtSupplierWarehouse(Order order) {
        order.getItems()
            .forEach(item -> item.setAtSupplierWarehouse(true));
    }

    public static void addParcelsRoute(Order order) {
        order.getDelivery()
            .getParcels()
            .forEach(ParcelSteps::addRoute);
    }

    private static Track getTrack(long orderId, long deliveryId, long deliveryServiceId) {
        Track track = new Track();
        track.setId(1234L);
        track.setOrderId(orderId);
        track.setDeliveryId(deliveryId);
        track.setTrackCode("123code");
        track.setDeliveryServiceId(deliveryServiceId);
        track.setTrackerId(1233L);
        return track;
    }

    public static Order getDSOrder() {
        Track track = new Track();
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        track.setDeliveryServiceId(123L);
        track.setTrackCode("track-code");

        Parcel parcel = new Parcel();
        parcel.setId(12L);
        parcel.addTrack(track);

        Delivery delivery = new Delivery();
        delivery.addParcel(parcel);
        delivery.setType(DeliveryType.DELIVERY);

        Order order = new Order();
        order.setId(1L);
        order.setDelivery(delivery);

        return order;
    }

    public static Order getPostOrderWithTrackType(DeliveryServiceType type) {
        Track track = new Track();
        track.setDeliveryServiceType(type);
        track.setDeliveryServiceId(123L);
        track.setTrackCode("track-code");

        Parcel parcel = new Parcel();
        parcel.setId(12L);
        parcel.addTrack(track);

        Delivery delivery = new Delivery();
        delivery.addParcel(parcel);
        delivery.setType(DeliveryType.POST);

        Order order = new Order();
        order.setId(1L);
        order.setDelivery(delivery);

        return order;
    }

    public static Order setShipmentId(Order order) {
        order.getDelivery().getParcels().get(0).setShipmentId(777L);
        return order;
    }

    public static Order getOrderWithParcelBoxes(long orderId) {
        return getOrderWithParcelBoxes(orderId, null);
    }

    public static Order getOrderWithParcelBoxes(long orderId, Color rgb) {
        Order filledOrder = getFilledOrder(orderId);
        filledOrder.setRgb(rgb);

        Delivery delivery = filledOrder.getDelivery();

        LinkedList<Parcel> parcels = new LinkedList<>();
        Parcel parcel = new Parcel();
        parcel.setId(12L);
        LinkedList<ParcelBox> boxes = new LinkedList<>();

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(1L);
        parcelBox.setFulfilmentId("1");

        ParcelBox parcelBox2 = new ParcelBox();
        parcelBox2.setId(2L);
        parcelBox2.setFulfilmentId("1");

        boxes.add(parcelBox);
        boxes.add(parcelBox2);

        parcel.setBoxes(boxes);

        Track track = new Track();
        track.setDeliveryServiceId(123L);
        track.setTrackCode("track-code");
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        parcel.setTracks(List.of(track));

        parcels.add(parcel);
        delivery.setParcels(parcels);

        return filledOrder;
    }

    public static Order createFulfilmentOrder() {
        return createFulfilmentOrder(OrderStatus.PROCESSING);
    }

    public static Order createFulfilmentOrder(OrderStatus orderStatus) {
        Order order = getFilledOrder();
        order.setId(1L);
        order.setRgb(Color.BLUE);
        order.setFulfilment(true);
        order.setStatus(orderStatus);

        Delivery delivery = new Delivery();
        delivery.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        delivery.setType(DeliveryType.DELIVERY);

        LinkedList<Parcel> parcels = new LinkedList<>();
        Parcel parcel = new Parcel();
        parcel.setId(12L);
        LinkedList<ParcelBox> boxes = new LinkedList<>();

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(1L);
        parcelBox.setFulfilmentId("1");

        boxes.add(parcelBox);

        parcel.setBoxes(boxes);

        ArrayList<Track> tracks = new ArrayList<>();
        Track track = new Track();
        track.setDeliveryServiceId(123L);
        track.setTrackCode("track-code");
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);
        tracks.add(track);
        parcel.setTracks(tracks);

        parcels.add(parcel);
        delivery.setParcels(parcels);
        order.setDelivery(delivery);

        clearTracks(order);
        OrderSteps.setOrderWeight(order);
        OrderSteps.setOrderDimension(order);

        return order;
    }

    private static class DeliveryMethodsSteps {
        private static final Long KEY = 123L;
        private static final String SERVICE_ID = "123";
        private static final boolean MARKET_BRANDED = true;
        private static final DeliveryMethod DELIVERY_METHOD = new DeliveryMethod(SERVICE_ID, MARKET_BRANDED);

        static Map<Long, DeliveryMethod> getDeliveryMethodsMap() {
            Map<Long, DeliveryMethod> deliveryMethodMap = new HashMap<>();
            deliveryMethodMap.put(KEY, DELIVERY_METHOD);
            return deliveryMethodMap;
        }
    }

    public static void clearTracks(Order order) {
        order.getDelivery()
            .getParcels()
            .stream()
            .findFirst()
            .ifPresent(p -> p.setTracks(null));
    }
}
