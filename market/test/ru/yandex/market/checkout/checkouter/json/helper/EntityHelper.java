package ru.yandex.market.checkout.checkouter.json.helper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.ImmutableMap;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.delivery.outlet.BreakTime;
import ru.yandex.market.checkout.checkouter.delivery.outlet.DayTimeRange;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutletPhone;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.RefundHistoryEvent;
import ru.yandex.market.checkout.checkouter.json.AddressJsonHandlerTest;
import ru.yandex.market.checkout.checkouter.json.PaymentJsonHandlerTest;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.ItemParameter;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.TariffType;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.order.UnitValue;
import ru.yandex.market.checkout.checkouter.order.UserGroup;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.OrderPromo;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentForm;
import ru.yandex.market.checkout.checkouter.pay.PaymentHistoryEventType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOption;
import ru.yandex.market.checkout.checkouter.pay.PaymentOptionHiddenReason;
import ru.yandex.market.checkout.checkouter.pay.PaymentRecord;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.pay.Refund;
import ru.yandex.market.checkout.checkouter.pay.RefundItem;
import ru.yandex.market.checkout.checkouter.pay.RefundReason;
import ru.yandex.market.checkout.checkouter.pay.RefundStatus;
import ru.yandex.market.checkout.checkouter.pay.RefundSubstatus;
import ru.yandex.market.checkout.checkouter.pay.RefundableDelivery;
import ru.yandex.market.checkout.checkouter.pay.RefundableItem;
import ru.yandex.market.checkout.checkouter.pay.TrustRefundKey;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptItem;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptStatus;
import ru.yandex.market.checkout.checkouter.receipt.ReceiptType;
import ru.yandex.market.checkout.checkouter.shop.MarketplaceFeature;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ScheduleLine;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.common.web.ServicePingResult;
import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.common.ping.ServiceInfo;
import ru.yandex.market.common.report.model.OfferPicture;

import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.blueMarketPromo;
import static ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition.marketCouponPromo;

public abstract class EntityHelper {

    public static final String DEFAULT_CASHBACK_PROMO_KEY = "1";
    public static final Date CHECKPOINT_DATE = new Date(111111111111L);
    public static final Date DATE = new Date(2222222222222000L);
    public static final Date CREATED_AT = new Date(111111111111000L);
    public static final Date UPDATED_AT = new Date(222222222222000L);
    public static final Date STATUS_UPDATED_AT = new Date(333333333333000L);
    public static final Currency CURRENCY = Currency.USD;
    public static final BigDecimal TOTAL_AMOUNT = new BigDecimal("3.45");
    public static final Date CREATION_DATE = Date.from(LocalDateTime.of(2017, Month.NOVEMBER, 11, 12, 0)
            .toInstant(ZoneOffset.UTC));
    public static final Date UPDATE_DATE = Date.from(LocalDateTime.of(2017, Month.NOVEMBER, 15, 15, 0)
            .toInstant(ZoneOffset.UTC));
    public static final Date STATUS_UPDATE_DATE = Date.from(LocalDateTime.of(2017, Month.NOVEMBER, 13, 19, 0)
            .toInstant(ZoneOffset.UTC));
    public static final Date STATUS_EXPIRY_DATE = Date.from(LocalDateTime.of(2017, Month.NOVEMBER, 15, 21, 0)
            .toInstant(ZoneOffset.UTC));
    public static final PaymentForm PAYMENT_FORM = new PaymentForm(ImmutableMap.of("token", "token"));
    public static final PrepayType PREPAY_TYPE = PrepayType.YANDEX_MARKET;
    public static final String FAIL_DESCRIPTION = "failDescription";
    public static final String BALANCE_PAY_METHOD_TYPE = "BALANCE_PAY_METHOD_TYPE";
    public static final String CARD_NUMBER = "12345678****2256";
    public static final Date SHIPMENT_DATE = new Date(111111080400000L);
    public static final int ID = 123;
    public static final String NAME = "name";
    public static final int REGION_ID = 456;
    public static final String CITY = "city";
    public static final String STREET = "street";
    public static final String KM = "km";
    public static final String HOUSE = "HOUSE";
    public static final String BUILDING = "building";
    public static final String ESTATE = "estate";
    public static final String BLOCK = "block";
    public static final String GPS = "gps";
    public static final String PERSONAL_GPS_ID = "personalGpsId";
    public static final String PERSONAL_ADDRESS_ID = "personalAddressId";
    public static final String NOTES = "notes";
    public static final int WIDTH = 12;
    public static final int HEIGHT = 34;
    public static final String URL = "//avatars.mds.yandex" +
            ".net/get-marketpictesting/478213/market_cpEGvnjlGcUKC7OdzSrryw/50x50";
    public static final int CONTAINER_HEIGHT = 56;
    public static final int CONTAINER_WIDTH = 78;
    public static final String ID_STRING = "id";
    public static final long UID = 123L;
    public static final long MUID = 123412341234L;
    public static final String UUID = "111111111111";
    public static final String IP = "127.0.0.1";
    public static final long DELIVERY_REGION_ID = 213L;
    @Deprecated // будет удалено в MARKETCHECKOUT-27942
    public static final String LAST_NAME = "lastName";
    @Deprecated // будет удалено в MARKETCHECKOUT-27942
    public static final String FIRST_NAME = "firstName";
    @Deprecated // будет удалено в MARKETCHECKOUT-27942
    public static final String MIDDLE_NAME = "middleName";
    public static final String PERSONAL_FULL_NAME_ID = "149c57f6efdbe393aa5878c1b4d22006";
    @Deprecated // будет удалено в MARKETCHECKOUT-27094
    public static final String PHONE = "+74952234562";
    public static final String PERSONAL_PHONE_ID = "0123456789abcdef0123456789abcdef";
    @Deprecated // будет удалено в MARKETCHECKOUT-27942
    public static final String EMAIL = "asd@gmail.com";
    public static final String PERSONAL_EMAIL_ID = "51e7897da4fa5ec326206b1908fbc43d";
    public static final boolean DONT_CALL = true;
    public static final boolean ASSESSOR = false;
    public static final String BIND_KEY = "bind.key";
    public static final boolean BEEN_CALLED = true;
    public static final long UNREAD_IMPORTANT_EVENTS = 123L;
    public static final Date UPDATE_DATE2 = new Date(111111111111000L);
    public static final Date EXPIRY_DATE2 = new Date(222222222222000L);
    public static final String YANDEX_UID = "yandexUid";
    public static final String SKU = "12345";
    public static final long MSKU = 12345L;
    public static final long VENDOR_ID = 10545982L;
    public static final String USER_AGENT = "the_user_agent";

    public static ServiceInfo getServiceInfo() {
        return new ServiceInfo("name", "description");
    }

    public static ServicePingResult getServicePingResult() {
        return new ServicePingResult(getServiceInfo(), getCheckResult());
    }

    private static CheckResult getCheckResult() {
        return new CheckResult(CheckResult.Level.OK, "message");
    }


    public static ServicePingResult getServicePingResultCrit() {
        return new ServicePingResult(getServiceInfo(), getCheckResultCrit());
    }

    private static CheckResult getCheckResultCrit() {
        return new CheckResult(CheckResult.Level.CRITICAL, "fuckup");
    }

    public static ShopOutletPhone getShopOutletPhone() {
        return new ShopOutletPhone("+7", "495", "2234562", "albatros");
    }

    public static DayTimeRange getDayTimeRange() {
        return new DayTimeRange(0, "08:00", 5, "16:00",
                Arrays.asList(new BreakTime("18:00", "19:00"),
                        new BreakTime("19:30", "20:00")));
    }

    public static TrackCheckpoint createTrackCheckpoint() {
        TrackCheckpoint trackCheckpoint = new TrackCheckpoint(
                456L,
                "country",
                "city",
                "location",
                "message",
                CheckpointStatus.DELIVERED,
                "zipCode",
                CHECKPOINT_DATE,
                123
        );
        trackCheckpoint.setId(123L);
        trackCheckpoint.setTranslatedCountry("страна");
        trackCheckpoint.setTranslatedCity("город");
        trackCheckpoint.setTranslatedLocation("местоположение");
        trackCheckpoint.setTranslatedMessage("сообщение");
        return trackCheckpoint;
    }

    public static ScheduleLine getScheduleLine() {
        return new ScheduleLine(0, 600, 1200);
    }

    public static RefundItem getRefundItem() {
        RefundItem refundItem = new RefundItem();
        refundItem.setItemId(111L);
        refundItem.setFeedId(123L);
        refundItem.setOfferId("456");
        refundItem.setCount(1);
        refundItem.setQuantity(BigDecimal.valueOf(1.1));
        refundItem.setDeliveryService(true);
        return refundItem;
    }

    public static Track getTrack() {
        Track track = new Track();
        track.setId(123L);
        track.setTrackCode("code");
        track.setDeliveryServiceId(123L);
        track.setTrackerId(456L);
        track.setStatus(TrackStatus.STARTED);
        track.setCheckpoints(Collections.singletonList(createTrackCheckpoint()));
        track.setCreationDate(DATE);
        return track;
    }

    public static ParcelItem getShipmentItem() {
        return new ParcelItem(123L, 234);
    }

    public static UnitValue createUnitValue() {
        UnitValue unitValue = new UnitValue();
        unitValue.setUnitId("unitId");
        unitValue.setValues(Arrays.asList("a", "b", "c"));
        unitValue.setShopValues(Arrays.asList("d", "e", "f"));
        unitValue.setDefaultUnit(true);
        return unitValue;
    }

    public static Refund getRefund() {
        Refund refund = new Refund();
        refund.setId(123L);
        refund.setOrderId(456L);
        refund.setPaymentId(789L);
        refund.setTrustRefundKey(new TrustRefundKey("trustRefundId"));
        refund.setHasReceipt(true);
        refund.setCurrency(Currency.RUR);
        refund.setAmount(new BigDecimal("12.34"));
        refund.setOrderRemainder(new BigDecimal("56.78"));
        refund.setComment("comment");
        refund.setStatus(RefundStatus.SUCCESS);
        refund.setSubstatus(RefundSubstatus.REFUND_FAILED);
        refund.setCreatedBy(987L);
        refund.setCreatedByRole(ClientRole.SHOP);
        refund.setShopManagerId(654L);
        refund.setCreationDate(new Date(111111111111111L));
        refund.setUpdateDate(new Date(222222222222222L));
        refund.setStatusUpdateDate(new Date(3333333333333333L));
        refund.setStatusExpiryDate(new Date(4444444444444444L));
        refund.setReason(RefundReason.ORDER_CHANGED);
        refund.setFake(true);
        return refund;
    }

    public static Receipt getReceipt() {
        Receipt receipt = new Receipt();

        receipt.setId(123L);
        receipt.setType(ReceiptType.INCOME);
        receipt.setPaymentId(456L);
        receipt.setRefundId(789L);
        receipt.setStatus(ReceiptStatus.NEW);

        receipt.setCreatedAt(CREATED_AT.toInstant());
        receipt.setUpdatedAt(UPDATED_AT.toInstant());
        receipt.setStatusUpdatedAt(STATUS_UPDATED_AT.toInstant());

        receipt.setItems(Collections.singletonList(getReceiptItem()));
        return receipt;
    }

    public static ReceiptItem getReceiptItem() {
        ReceiptItem item = new ReceiptItem();
        item.setOrderId(1L);
        item.setReceiptId(123L);
        item.setItemId(2L);
        item.setItemServiceId(4L);
        item.setDeliveryId(3L);
        item.setItemTitle("top item");
        item.setCount(666);
        item.setPrice(new BigDecimal("322"));
        item.setAmount(new BigDecimal("123456"));
        return item;
    }

    public static Payment getPayment() {
        Payment payment = new Payment();
        payment.setId(PaymentJsonHandlerTest.ID);
        payment.setOrderId(PaymentJsonHandlerTest.ORDER_ID);
        payment.setBasketId(PaymentJsonHandlerTest.BASKET_ID);
        payment.setFake(PaymentJsonHandlerTest.FAKE);
        payment.setStatus(PaymentJsonHandlerTest.STATUS);
        payment.setSubstatus(PaymentJsonHandlerTest.SUBSTATUS);
        payment.setFailReason(PaymentJsonHandlerTest.FAIL_REASON);
        payment.setUid(PaymentJsonHandlerTest.UID);
        payment.setCurrency(CURRENCY);
        payment.setTotalAmount(TOTAL_AMOUNT);
        payment.setCreationDate(CREATION_DATE);
        payment.setUpdateDate(UPDATE_DATE);
        payment.setStatusUpdateDate(STATUS_UPDATE_DATE);
        payment.setStatusExpiryDate(STATUS_EXPIRY_DATE);
        payment.setPaymentForm(PAYMENT_FORM);
        payment.setPrepayType(PREPAY_TYPE);
        payment.setFailDescription(FAIL_DESCRIPTION);
        payment.setCardNumber(CARD_NUMBER);
        payment.setBalancePayMethodType(BALANCE_PAY_METHOD_TYPE);
        return payment;
    }


    public static AddressImpl getAddress() {
        AddressImpl address = new AddressImpl();
        address.setCountry(AddressJsonHandlerTest.COUNTRY);
        address.setPostcode(AddressJsonHandlerTest.POSTCODE);
        address.setCity(AddressJsonHandlerTest.CITY);
        address.setDistrict(AddressJsonHandlerTest.DISTRICT);
        address.setSubway(AddressJsonHandlerTest.SUBWAY);
        address.setStreet(AddressJsonHandlerTest.STREET);
        address.setHouse(AddressJsonHandlerTest.HOUSE);
        address.setBlock(AddressJsonHandlerTest.BLOCK);
        address.setEntrance(AddressJsonHandlerTest.ENTRANCE);
        address.setEntryPhone(AddressJsonHandlerTest.ENTRY_PHONE);
        address.setFloor(AddressJsonHandlerTest.FLOOR);
        address.setApartment(AddressJsonHandlerTest.APARTMENT);
        address.setPersonalAddressId(AddressJsonHandlerTest.PERSONAL_ADDRESS_ID);
        address.setRecipient(AddressJsonHandlerTest.RECIPIENT);
        address.setPersonalFullNameId(AddressJsonHandlerTest.PERSONAL_FULL_NAME_ID);
        address.setPhone(AddressJsonHandlerTest.PHONE);
        address.setPersonalPhoneId(AddressJsonHandlerTest.PERSONAL_PHONE_ID);
        address.setRecipientEmail(AddressJsonHandlerTest.RECIPIENT_EMAIL);
        address.setPersonalEmailId(AddressJsonHandlerTest.PERSONAL_EMAIL_ID);
        address.setAddressSource(AddressJsonHandlerTest.ADDRESS_SOURCE);
        address.setLanguage(AddressJsonHandlerTest.LANGUAGE);
        address.setGps(AddressJsonHandlerTest.GPS);
        address.setPersonalGpsId(AddressJsonHandlerTest.PERSONAL_GPS_ID);
        return address;
    }

    public static ShopOutlet getShopOutlet() {
        ShopOutlet shopOutlet = new ShopOutlet();
        shopOutlet.setId((long) ID);
        shopOutlet.setName(NAME);
        shopOutlet.setRegionId(REGION_ID);
        shopOutlet.setCity(CITY);
        shopOutlet.setStreet(STREET);
        shopOutlet.setKm(KM);
        shopOutlet.setHouse(HOUSE);
        shopOutlet.setBuilding(BUILDING);
        shopOutlet.setEstate(ESTATE);
        shopOutlet.setBlock(BLOCK);
        shopOutlet.setGps(GPS);
        shopOutlet.setPersonalGpsId(PERSONAL_GPS_ID);
        shopOutlet.setPersonalAddressId(PERSONAL_ADDRESS_ID);
        shopOutlet.setNotes(NOTES);
        shopOutlet.setPhones(Collections.singletonList(getShopOutletPhone()));
        return shopOutlet;
    }

    public static Parcel getOrderShipment() {
        Parcel orderShipment = new Parcel();
        orderShipment.setId(123L);
        orderShipment.setShipmentId(345L);
        orderShipment.setWeight(567L);
        orderShipment.setWidth(109L);
        orderShipment.setHeight(789L);
        orderShipment.setDepth(901L);
        orderShipment.setStatus(ParcelStatus.NEW);
        orderShipment.setLabelURL("labelUrl");
        orderShipment.setTracks(Collections.singletonList(getTrack()));
        orderShipment.setParcelItems(Collections.singletonList(getShipmentItem()));
        orderShipment.setFromDate(LocalDate.of(2018, 1, 1));
        orderShipment.setToDate(LocalDate.of(2018, 1, 2));
        orderShipment.setTariffType(TariffType.REGISTERED);
        orderShipment.setRoute(new TextNode("test"));
        orderShipment.setCombinatorRouteId(new UUID(0, 0));
        return orderShipment;
    }

    public static OfferPicture getOfferPicture() {
        OfferPicture offerPicture = new OfferPicture(URL);
        offerPicture.setContainerWidth(CONTAINER_WIDTH);
        offerPicture.setContainerHeight(CONTAINER_HEIGHT);
        offerPicture.setWidth(WIDTH);
        offerPicture.setHeight(HEIGHT);
        return offerPicture;
    }

    public static ItemParameter getItemParameter() {
        ItemParameter itemParameter = new ItemParameter();
        itemParameter.setType("type");
        itemParameter.setSubType("subType");
        itemParameter.setName("name");
        itemParameter.setValue("value");
        itemParameter.setUnit("unit");
        itemParameter.setCode("code");
        itemParameter.setUnits(Collections.singletonList(createUnitValue()));
        itemParameter.setSpecifiedForOffer(true);
        return itemParameter;
    }

    public static void setOfferItem(OfferItem orderItem) {
        orderItem.setFeedId(234L);
        orderItem.setOfferId("345");
        orderItem.setWareMd5("456");
        orderItem.setFeedCategoryId("123");
        orderItem.setCategoryId(567);
        orderItem.setOfferName("offerName");
        orderItem.setPrice(new BigDecimal("67.89"));
        orderItem.setCount(789);
        orderItem.setDelivery(true);
        orderItem.setPromoKey("promoKey");
        orderItem.setErrors(Collections.singleton(new ValidationResult(
                "code", ValidationResult.Severity.ERROR
        )));
    }

    public static OrderItem getOrderItem() {
        return getOrderItem(false);
    }

    public static OrderItem getOrderItem(Boolean isPickupPromocode) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(123L);
        setOfferItem(orderItem);
        orderItem.setModelId(987L);
        orderItem.setDescription("description");
        orderItem.setPictures(Collections.singletonList(getOfferPicture()));
        orderItem.setBuyerPrice(new BigDecimal("87.6"));
        orderItem.setQuantPrice(new BigDecimal("87.6"));
        orderItem.setFee(new BigDecimal("7.65"));
        orderItem.setFeeInt(765);
        orderItem.setFeeSum(new BigDecimal("6.54"));
        orderItem.setPp(1000);
        orderItem.setShowUid("showUid");
        orderItem.setCartShowUid("cartShowUid");
        orderItem.setCartShowInfo("cartShowInfo");
        orderItem.setRealShowUid("realShowUid");
        orderItem.setShowInfo("showInfo");
        orderItem.setShopUrl("shopUrl");
        orderItem.setKind2Parameters(Collections.singletonList(getItemParameter()));
        orderItem.setVat(VatType.VAT_18);
        orderItem.getPrices().setSubsidy(new BigDecimal("5.43"));
        orderItem.getPrices().setBuyerDiscount(new BigDecimal("4.32"));
        orderItem.getPrices().setBuyerSubsidy(new BigDecimal("2.10"));
        orderItem.getPrices().setBuyerPriceBeforeDiscount(new BigDecimal("91.92"));

        orderItem.setPromos(makePromos(isPickupPromocode));
        orderItem.setLoyaltyProgramPartner(true);
        orderItem.setChanges(Collections.singleton(ItemChange.COUNT));
        orderItem.setSku(SKU);
        orderItem.setMsku(MSKU);
        orderItem.setShopSku("shopSku");
        orderItem.setSupplierId(111L);
        orderItem.setVendorId(VENDOR_ID);
        return orderItem;
    }

    private static Set<ItemPromo> makePromos(Boolean isPickupPromocode) {
        final Set<ItemPromo> promos = new HashSet<>();
        promos.add(new ItemPromo(marketCouponPromo(isPickupPromocode),
                new BigDecimal("5.43"), new BigDecimal("4.32"), new BigDecimal("3.21")));
        promos.add(ItemPromo.cashbackPromo(BigDecimal.valueOf(151L), DEFAULT_CASHBACK_PROMO_KEY,
                123L, new BigDecimal("5"), new BigDecimal("95")));
        return promos;
    }

    public static Delivery getDelivery() {
        Delivery delivery = new Delivery();
        delivery.setId("id");
        delivery.setShopDeliveryId("shopDeliveryId");
        delivery.setHash("hash");
        delivery.setDeliveryOptionId("deliveryOptionId");
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setServiceName("serviceName");
        delivery.setPrice(new BigDecimal("12.34"));
        delivery.setBuyerPrice(new BigDecimal("34.56"));
        delivery.setDeliveryDates(new DeliveryDates(0, 2));
        delivery.setValidatedDeliveryDates(new DeliveryDates(0, 2));
        delivery.setRawDeliveryIntervals(new RawDeliveryIntervalsCollection());
        delivery.setRegionId(2L);
        delivery.setAddress(getAddress());
        delivery.setBuyerAddress(getAddress());
        delivery.setShopAddress(getAddress());
        delivery.setOutletId(123L);
        delivery.setOutletCode("outletCode");
        delivery.setOutlet(getShopOutlet());
        delivery.setOutlets(Arrays.asList(getShopOutlet()));
        delivery.setPaymentOptions(EnumSet.of(PaymentMethod.YANDEX));
        delivery.setPaymentRecords(Collections.singleton(new PaymentRecord(PaymentMethod.YANDEX)));
        delivery.setHiddenPaymentOptions(
                Arrays.asList(new PaymentOption(PaymentMethod.CASH_ON_DELIVERY, PaymentOptionHiddenReason.MULTICART))
        );
        delivery.setValidationErrors(
                Collections.singletonList(new ValidationResult("code", ValidationResult.Severity.ERROR))
        );
        delivery.setValidFeatures(EnumSet.of(MarketplaceFeature.PLAINCPA));
        delivery.setDeliveryServiceId(99L);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setParcels(Collections.singletonList(getOrderShipment()));
        delivery.setVat(VatType.VAT_18);
        delivery.setUserReceived(true);
        delivery.setTryingAvailable(true);
        return delivery;
    }

    public static Buyer getBuyer() {
        Buyer buyer = new Buyer();
        buyer.setId(ID_STRING);
        buyer.setUid(UID);
        buyer.setMuid(MUID);
        buyer.setUuid(UUID);
        buyer.setYandexUid(YANDEX_UID);
        buyer.setIp(IP);
        buyer.setRegionId(DELIVERY_REGION_ID);
        buyer.setLastName(LAST_NAME);
        buyer.setFirstName(FIRST_NAME);
        buyer.setMiddleName(MIDDLE_NAME);
        buyer.setPersonalFullNameId(PERSONAL_FULL_NAME_ID);
        buyer.setPhone(PHONE);
        buyer.setPersonalPhoneId(PERSONAL_PHONE_ID);
        buyer.setEmail(EMAIL);
        buyer.setPersonalEmailId(PERSONAL_EMAIL_ID);
        buyer.setDontCall(DONT_CALL);
        buyer.setAssessor(ASSESSOR);
        buyer.setBindKey(BIND_KEY);
        buyer.setBeenCalled(BEEN_CALLED);
        buyer.setUnreadImportantEvents(UNREAD_IMPORTANT_EVENTS);
        buyer.setUserAgent(USER_AGENT);
        return buyer;
    }

    public static Order getOrder() {
        Order order = getOrderNoErrors();

        order.setValidationErrors(
                Collections.singletonList(new ValidationResult("code", ValidationResult.Severity.ERROR))
        );
        order.setValidationWarnings(
                Collections.singletonList(new ValidationResult("code", ValidationResult.Severity.WARNING))
        );
        return order;
    }

    public static Order getOrderNoErrors() {
        Order order = new Order();
        order.setId(123L);
        order.setShopId(234L);
        order.setStatus(OrderStatus.PROCESSING);
        order.setSubstatus(OrderSubstatus.PENDING_CANCELLED);

        order.setCreationDate(CREATION_DATE);
        order.setUpdateDate(UPDATE_DATE);
        order.setStatusUpdateDate(STATUS_UPDATE_DATE);
        order.setSubstatusUpdateDate(STATUS_UPDATE_DATE);
        order.setStatusExpiryDate(STATUS_EXPIRY_DATE);

        order.setCurrency(Currency.RUR);
        order.setBuyerCurrency(Currency.USD);
        order.setExchangeRate(new BigDecimal("12.34"));

        order.setItemsTotal(new BigDecimal("23.45"));
        order.setBuyerItemsTotal(new BigDecimal("34.56"));
        order.setTotal(new BigDecimal("45.67"));
        order.setBuyerTotal(new BigDecimal("56.78"));
        order.setRealTotal(new BigDecimal("67.89"));
        order.setFeeTotal(new BigDecimal("78.91"));

        order.getPromoPrices().setBuyerItemsTotalBeforeDiscount(new BigDecimal("46.9"));
        order.getPromoPrices().setBuyerItemsTotalDiscount(new BigDecimal("12.34"));
        order.getPromoPrices().setBuyerTotalBeforeDiscount(new BigDecimal("69.12"));
        order.getPromoPrices().setBuyerTotalDiscount(new BigDecimal("12.34"));

        order.setPaymentType(PaymentType.PREPAID);
        order.setPaymentMethod(PaymentMethod.YANDEX);

        order.setItems(Collections.singletonList(getOrderItem(false)));
        order.setDelivery(getDelivery());
        order.setBuyer(getBuyer());

        order.setFake(true);
        order.setContext(Context.MARKET);
        order.setNotes("notes");
        order.setShopOrderId("shopOrderId");

        order.setDeliveryOptions(Collections.singletonList(getDelivery()));
        order.setPaymentOptions(Collections.singleton(PaymentMethod.YANDEX));

        order.setChanges(EnumSet.of(CartChange.DELIVERY));
        order.setPaymentId(567L);
        order.setBalanceOrderId("balanceOrderId");
        order.setRefundPlanned(new BigDecimal("67.89"));
        order.setRefundActual(new BigDecimal("78.91"));

        order.setUserGroup(UserGroup.ABO);

        order.setNoAuth(true);
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);

        order.setSignature("signature");
        order.setShopName("shopName");
        order.setGlobal(true);
        order.setPayment(getPayment());
        order.setTaxSystem(TaxSystem.OSN);
        order.addMarketplaceFeature(MarketplaceFeature.PLAINCPA);

        OrderPromo orderPromo = new OrderPromo(blueMarketPromo("some promo"));
        orderPromo.setBuyerItemsDiscount(new BigDecimal("12.34"));
        orderPromo.setSubsidy(new BigDecimal("90.23"));
        order.addPromo(orderPromo);

        order.getPromoPrices().setSubsidyTotal(new BigDecimal("23.45"));

        order.setFulfilment(true);
        order.setPaymentSystem("mastercard");
        return order;
    }

    public static OrderFailure getOrderFailure() {
        return new OrderFailure(
                getOrder(),
                OrderFailure.Code.OUT_OF_DATE,
                OrderFailure.Reason.SHOP_IS_TRICKY,
                "errorDetails",
                "errorDevDetails"
        );
    }

    public static RefundHistoryEvent getRefundHistoryEvent() {
        RefundHistoryEvent event = new RefundHistoryEvent();

        event.setId(123L);
        event.setType(PaymentHistoryEventType.CREATE);
        event.setAuthor(new ClientInfo(ClientRole.USER, 345L));
        event.setStatus(RefundStatus.SUCCESS);
        event.setSubstatus(RefundSubstatus.REFUND_EXPIRED);
        event.setUpdateDate(UPDATE_DATE2);
        event.setExpiryDate(EXPIRY_DATE2);
        event.setRefund(getRefund());
        return event;
    }

    public static OrderHistoryEvent getOrderHistoryEvent() {
        OrderHistoryEvent orderHistoryEvent = new OrderHistoryEvent();
        orderHistoryEvent.setId(123L);
        orderHistoryEvent.setType(HistoryEventType.ORDER_DELIVERY_UPDATED);
        orderHistoryEvent.setAuthor(new ClientInfo(ClientRole.SHOP, 242102L));
        orderHistoryEvent.setFromDate(CREATION_DATE);
        orderHistoryEvent.setToDate(UPDATE_DATE);
        orderHistoryEvent.setTranDate(STATUS_EXPIRY_DATE);
        orderHistoryEvent.setHost("host");
        orderHistoryEvent.setRefundActual(new BigDecimal("12.34"));
        orderHistoryEvent.setRefundPlanned(new BigDecimal("34.56"));
        orderHistoryEvent.setRefundId(345L);
        orderHistoryEvent.setOrderAfter(getOrder());
        orderHistoryEvent.setOrderBefore(getOrder());
        orderHistoryEvent.setRefundEvent(getRefundHistoryEvent());
        orderHistoryEvent.setReceipt(getReceipt());
        orderHistoryEvent.setReadByUser(true);
        return orderHistoryEvent;
    }

    public static RefundableDelivery getRefundableDelivery() {
        RefundableDelivery refundableDelivery = new RefundableDelivery();
        refundableDelivery.setType(DeliveryType.DELIVERY);
        refundableDelivery.setServiceName("serviceName");
        refundableDelivery.setPrice(new BigDecimal("12.34"));
        refundableDelivery.setBuyerPrice(new BigDecimal("34.56"));
        refundableDelivery.setRefundable(true);
        return refundableDelivery;
    }

    public static RefundableItem getRefundableItem() {
        RefundableItem refundableItem = new RefundableItem();
        refundableItem.setFeedId(123L);
        refundableItem.setOfferId("345");
        refundableItem.setWareMd5("wareMd5");
        refundableItem.setFeedGroupIdHash("feedGroupIdHash");
        refundableItem.setFeedCategoryId("567");
        refundableItem.setCategoryId(789);
        refundableItem.setOfferName("offerName");
        refundableItem.setPrice(new BigDecimal("9.87"));
        refundableItem.setCount(765);
        refundableItem.setModelId(654L);
        refundableItem.setDescription("description");
        refundableItem.setPictures(Collections.singletonList(getOfferPicture()));
        refundableItem.setBuyerPrice(new BigDecimal("4.32"));
        refundableItem.setRefundableCount(321);
        refundableItem.setQuantity(BigDecimal.valueOf(765));
        refundableItem.setQuantPrice(new BigDecimal("4.32"));
        refundableItem.setRefundableQuantity(BigDecimal.valueOf(321));

        return refundableItem;
    }
}
