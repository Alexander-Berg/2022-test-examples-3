package ru.yandex.market.checkout.util;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.AddressLanguage;
import ru.yandex.market.checkout.checkouter.delivery.AddressSource;
import ru.yandex.market.checkout.checkouter.delivery.AddressType;
import ru.yandex.market.checkout.checkouter.delivery.BusinessRecipient;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.FreeDeliveryInfo;
import ru.yandex.market.checkout.checkouter.delivery.LiftType;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalAndDate;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletDeliveryTimeInterval;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletPurpose;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.delivery.parcel.CancellationRequestStatus;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBoxItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.DeliveryDeadlineStatus;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.ItemPrices;
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.TariffType;
import ru.yandex.market.checkout.checkouter.order.TaxSystem;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.order.promo.ItemPromo;
import ru.yandex.market.checkout.checkouter.order.promo.PromoDefinition;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.order.promo.ReportPromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.pushapi.client.entity.DispatchType;
import ru.yandex.market.checkout.pushapi.client.entity.Region;
import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrderItem;
import ru.yandex.market.checkout.pushapi.shop.entity.StubExtraContext;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

/**
 * Вспомогательные методы для потроения полного заказа для использования в тестах
 */
public class ShopOrderTestData {

    public static ShopOrder get() {
        ShopOrder shopOrder = new ShopOrder();
        shopOrder.setBusinessId(123456L);
        shopOrder.setDeliveryWithRegion(buildDelivery());
        shopOrder.setId(9876543L);
        shopOrder.setCurrency(Currency.RUR);
        shopOrder.setItems(buildOrderItems());
        shopOrder.setPaymentType(PaymentType.POSTPAID);
        shopOrder.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
        shopOrder.setTaxSystem(TaxSystem.ENVD);
        shopOrder.setFake(false);
        shopOrder.setFulfilment(false);
        shopOrder.setBuyer(BuyerProvider.getSberIdBuyer());
        shopOrder.setNotes("shop_order_notes_value");
        shopOrder.setRgb(Color.BLUE);
        shopOrder.setPreorder(false);
        shopOrder.setStatus(OrderStatus.DELIVERED);
        shopOrder.setSubstatus(OrderSubstatus.DELIVERED_USER_RECEIVED);
        shopOrder.setCreationDate(getDate("15-04-2022 16:21:33"));
        shopOrder.setItemsTotal(new BigDecimal("2"));
        shopOrder.setTotal(new BigDecimal("2"));
        shopOrder.setSubsidy(new BigDecimal("1"));
        shopOrder.setTotalWithSubsidy(new BigDecimal("3"));
        shopOrder.setContext(Context.PRODUCTION_TESTING);
        shopOrder.setStubContext(new StubExtraContext());
        shopOrder.setElectronicAcceptanceCertificateCode("901-401");
        return shopOrder;
    }

    private static DeliveryWithRegion buildDelivery() {
        DeliveryWithRegion delivery = new DeliveryWithRegion();
        delivery.setRegion(buildRegion());
        delivery.setDispatchType(DispatchType.BUYER);
        delivery.setCourier(buildCourier());
        delivery.setId("183456");
        delivery.setHash("vujQrQNMAdOzZmnKZ6gFMPXWOueGz50j1iwnxBdVNl4D6jFBborYfMqavs/jjvh5nihb6o0vLHc=");
        delivery.setShopDeliveryId("54321");
        delivery.setDeliveryOptionId("123");
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setServiceName("courier-delivery-service");
        delivery.setPrice(new BigDecimal("8913"));
        delivery.setBuyerPrice(new BigDecimal("8813"));
        delivery.setSupplierPrice(new BigDecimal("8703"));
        delivery.setSupplierDiscount(new BigDecimal("8657"));
        delivery.setLiftPrice(new BigDecimal("8208"));
        delivery.setDeliveryDates(buildDeliveryDates());
        delivery.setValidatedDeliveryDates(buildDeliveryDates());
        delivery.setRealDeliveryDate(getDate("16-04-2022 00:00:00"));
        delivery.setRawDeliveryIntervals(buildDeliveryIntervals());
        delivery.setFreeDeliveryInfo(new FreeDeliveryInfo(new BigDecimal("193"), new BigDecimal("905")));
        delivery.setRegionId(213L);
        delivery.setAddress(buildAddress());
        delivery.setBuyerAddress(buildAddress());
        delivery.setShopAddress(buildAddress());
        delivery.setOnDemandOutletId("on_demand_outlet_id");
        delivery.setOutletStoragePeriod(10);
        delivery.setOutletStorageLimitDate(LocalDate.of(2022, 4, 17));
        delivery.setOutletPurpose(OutletPurpose.PICKUP);
        delivery.setOutletIds(Set.of(2L, 3L, 4L));
        delivery.setPostCodes(List.of(101010L, 202020L));
        delivery.setOutletCodes(Set.of("outlet_code_1", "outlet_code_2"));
        delivery.setOutlets(List.of(buildOutlet()));
        delivery.setOutletTimeIntervals(List.of(
                new OutletDeliveryTimeInterval(492L,
                        LocalTime.of(10, 20),
                        LocalTime.of(10, 50)))
        );
        delivery.setPaymentOptions(Set.of(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY));
        delivery.setDeliveryServiceId(18344L);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setParcels(buildParcels());
        delivery.setVat(VatType.VAT_10);
        delivery.setBalanceOrderId("balance_id_value");
        delivery.setSubsidyBalanceOrderId("subsidy_balance_order_id_value");
        delivery.setUserReceived(true);
        delivery.setCommissionPercentage(new BigDecimal("20.5"));
        delivery.setPromos(buildPromos());
        delivery.setPrices(buildPrices());
        delivery.setPostOutletId(839275L);
        delivery.setPostOutlet(buildOutlet());
        delivery.setIgnoreSlaCheck(false);
        delivery.setTariffId(1947L);
        delivery.setFeatures(Set.of(DeliveryFeature.EXPRESS_DELIVERY, DeliveryFeature.ON_DEMAND));
        delivery.setMarketBranded(true);
        delivery.setMarketPartner(false);
        delivery.setMarketPostTerm(true);
        delivery.setLiftType(LiftType.ELEVATOR);
        delivery.setVerificationCode("123-456");
        delivery.setLeaveAtTheDoor(true);
        delivery.setBusinessRecipient(buildBusinessRecipient());
        delivery.setUnloadEnabled(false);
        delivery.setTryingAvailable(true);
        return delivery;
    }

    private static List<ShopOrderItem> buildOrderItems() {
        ShopOrderItem offerItem1 = new ShopOrderItem() {{
            setId(1L);
            setFeedId(1234L);
            setOfferId("item1");
            setBundleId("ItemBundleId");
            setCategoryId(3456);
            setFeedCategoryId("Камеры");
            setOfferName("OfferName");
            setCount(5);
            setPrice(new BigDecimal("4567"));
            setDelivery(true);
            setKind2ParametersString("Ширина: 20 м, Высота: 20 м");
            setBuyerPrice(new BigDecimal("4551"));
            setVat(VatType.VAT_10);
            setExternalFeedId(4301L);
            setFee(new BigDecimal("2099"));
            setSupplierId(8942L);
            setSku("192045601");
            setShopSku("192045601");
            setWarehouseId(5037);
            setPartnerWarehouseId("5037");
            setPromos(buildPromos());
            setSubsidy(new BigDecimal("250"));
            setInstances(buildInstances());
            setPrescriptionGuids(Set.of("94c37fed-474f-4b30-bb17-ac4e27fb3845"));
        }};
        ShopOrderItem offerItem2 = new ShopOrderItem() {{
            setFeedId(1L);
            setOfferId("item2");
            setFeedCategoryId("Камеры");
            setOfferName("OfferName");
            setPrice(new BigDecimal("4567"));
            setCount(5);
            setDelivery(true);
        }};
        return List.of(offerItem1, offerItem2);
    }

    private static Set<ItemPromo> buildPromos() {
        return Set.of(
                new ItemPromo(
                        PromoDefinition.builder()
                                .type(PromoType.MARKET_COUPON)
                                .marketPromoId("MKNJCBLAxmZEq")
                                .shopPromoId("KMzOyzzzMRqxoi")
                                .anaplanId("NDEsdfr129dfv")
                                .bundleId("KMSqrnhf21Pwe")
                                .bundleReturnRestrict(false)
                                .promoCode("MDHQOD")
                                .coinId(810L)
                                .reason("MainReason")
                                .partnerPromo(false)
                                .isPickupPromocode(false)
                                .sourceType("myPromo")
                                .build(),
                        new BigDecimal("48"),
                        new BigDecimal("48"),
                        new BigDecimal("299.9"),
                        new BigDecimal("38.1"),
                        new BigDecimal("42.3"),
                        new BigDecimal("51"),
                        new BigDecimal("68"),
                        new BigDecimal("73"),
                        8942L,
                        false,
                        "semanticDesctiption1",
                        "detailsGroup1",
                        List.of("flag1", "flag2"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                )
        );
    }

    private static List<OrderItemInstance> buildInstances() {
        return List.of(
                new OrderItemInstance() {{
                    setCis("cis1");
                    setCisFull("fullCis1");
                    setUit("id1");
                    setBalanceOrderId("balance1");
                    setSn("201 203576");
                }},
                new OrderItemInstance() {{
                    setCis("cis2");
                    setCisFull("fullCis2");
                    setUit("id2");
                    setBalanceOrderId("balance2");
                    setSn("314 57492061");
                }}
        );
    }

    private static Region buildRegion() {
        return new Region(213, "г. Москва", RegionType.CITY,
                new Region(3, "Центральный федеральный округ", RegionType.COUNTRY_DISTRICT, null)
        );
    }

    private static Courier buildCourier() {
        return new Courier() {{
            setFullName("Иванов Иван Иванович");
            setPhone("+7 111 222-33-44");
            setPhoneExtension("5678");
            setVehicleNumber("д103ом");
            setVehicleDescription("черная mazda 6");
        }};
    }

    private static DeliveryDates buildDeliveryDates() {
        //1650028893086 = 2022-04-15 16:21:33
        //1650091438308 = 2022-04-16 09:43:58
        //1649970000900 = 2022-04-15 00:00:00
        //1650056400963 = 2022-04-16 00:00:00
        return new DeliveryDates(getDate("15-04-2022 16:21:33"),
                getDate("16-04-2022 09:43:58"),
                LocalTime.of(16, 21, 33),
                LocalTime.of(9, 43, 58)
        );
    }

    private static RawDeliveryIntervalsCollection buildDeliveryIntervals() {
        return new RawDeliveryIntervalsCollection(List.of(
                new RawDeliveryIntervalAndDate(getDate("15-04-2022 00:00:00"),
                        List.of(new RawDeliveryInterval(getDate("15-04-2022 00:00:00"),
                                        LocalTime.of(16, 21, 33),
                                        LocalTime.of(18, 40, 0)),
                                new RawDeliveryInterval(getDate("15-04-2022 00:00:00"),
                                        LocalTime.of(18, 40, 0),
                                        LocalTime.of(22, 0, 0)))),
                new RawDeliveryIntervalAndDate(getDate("16-04-2022 00:00:00"),
                        List.of(new RawDeliveryInterval(getDate("16-04-2022 00:00:00"),
                                        LocalTime.of(8, 0, 0),
                                        LocalTime.of(9, 0, 0)),
                                new RawDeliveryInterval(getDate("16-04-2022 00:00:00"),
                                        LocalTime.of(9, 0, 0),
                                        LocalTime.of(9, 43, 0))))
        )
        );
    }

    private static Address buildAddress() {
        AddressImpl address = new AddressImpl();
        address.setCountry("Русь");
        address.setPostcode("131488");
        address.setCity("Питер");
        address.setDistrict("value_district");
        address.setSubway("Петровско-Разумовская");
        address.setStreet("Победы");
        address.setKm("90");
        address.setHouse("13");
        address.setBuilding("23А");
        address.setEstate("value_estate");
        address.setBlock("666");
        address.setEntrance("value_entrance");
        address.setEntryPhone("value_entryphone");
        address.setFloor("8");
        address.setApartment("value_apartment");
        address.setGps("45.3475872345 39.1234752245");
        address.setNotes("value_notes");
        address.setRecipient("value_recipient");
        address.setPhone("value_phone");
        address.setScheduleString("ПН-ПТ 9:00-18:00");
        address.setType(AddressType.BUYER);
        address.setLanguage(AddressLanguage.RUS);
        address.setPreciseRegionId(213L);
        address.setRecipientPerson(new RecipientPerson("Петров", "Петр", "Петрович"));
        address.setOutletName("value_outlet_name");
        address.setOutletPhones(new String[]{"8 9191919191", "8 7575757575"});
        address.setRecipientEmail("test_recipient@yandex.ru");
        address.setAddressSource(AddressSource.PERS_ADDRESS);
        address.setYandexMapPermalink("value_yandex_permalink");
        address.setBusinessRecipient(buildBusinessRecipient());
        return address;
    }

    private static BusinessRecipient buildBusinessRecipient() {
        return new BusinessRecipient() {{
            setName("organization312");
            setInn("82348575");
            setKpp("514353");
        }};
    }

    private static ShopOutlet buildOutlet() {
        ShopOutlet shopOutlet = new ShopOutlet();
        shopOutlet.setScheduleString("schedule-string");
        shopOutlet.setPostcode("postcode");
        shopOutlet.setCity("city");
        shopOutlet.setStreet("street");
        shopOutlet.setHouse("house");
        shopOutlet.setBlock("block");
        return shopOutlet;
    }

    private static List<Parcel> buildParcels() {
        Parcel parcel = new Parcel();
        parcel.setId(52L);
        parcel.setOrderId(1035782L);
        parcel.setDeliveryId(183456L);
        parcel.setShipmentId(812345L);
        parcel.setWeight(400L);
        parcel.setWidth(120L);
        parcel.setHeight(80L);
        parcel.setDepth(100L);
        parcel.setStatus(ParcelStatus.READY_TO_SHIP);
        parcel.setLabelURL("label_url_value");
        parcel.setParcelItems(buildParcelItems());
        parcel.setFromDate(LocalDate.of(2022, 4, 15));
        parcel.setToDate(LocalDate.of(2022, 4, 16));
        parcel.setTariffType(TariffType.REGISTERED);
        parcel.setBoxes(buildParcelBoxes());
        parcel.setCreationDate(LocalDateTime.of(2022, 4, 15, 5, 20, 33)
                .toInstant(ZoneOffset.ofHours(3)));
        parcel.setShipmentDate(LocalDate.of(2022, 4, 16));
        parcel.setShipmentTime(LocalDateTime.of(2022, 4, 16, 16, 41, 33));
        parcel.setPackagingTime(LocalDateTime.of(2022, 4, 15, 5, 20, 33)
                .toInstant(ZoneOffset.ofHours(3)));
        parcel.setDeliveredAt(LocalDateTime.of(2022, 4, 16, 16, 41, 33)
                .toInstant(ZoneOffset.ofHours(3)));
        parcel.setCancellationRequest(buildCancellationRequest());
        parcel.setDelayedShipmentDate(LocalDate.of(2022, 4, 16));
        parcel.setDeliveryDeadlineStatus(DeliveryDeadlineStatus.PROGRESS);
        parcel.setShipmentDateTimeBySupplier(LocalDateTime.of(2022, 4, 15, 3, 10, 20));
        parcel.setReceptionDateTimeByWarehouse(LocalDateTime.of(2022, 4, 16, 23, 20, 10));
        return List.of(parcel);
    }

    private static List<ParcelItem> buildParcelItems() {
        LocalDateTime time = LocalDateTime.of(2022, 4, 15, 13, 40);
        Instant timeInstant = time.toInstant(ZoneOffset.ofHours(3));
        ParcelItem parcelItem = new ParcelItem();
        parcelItem.setDeliveryId(183456L);
        parcelItem.setParcelId(52L);
        parcelItem.setItemId(223L);
        parcelItem.setCount(2);
        parcelItem.setSupplierStartDateTime(timeInstant);
        parcelItem.setSupplierShipmentDateTime(timeInstant);
        parcelItem.setShipmentDateTimeBySupplier(time);
        parcelItem.setReceptionDateTimeByWarehouse(time);
        return List.of(parcelItem);
    }

    private static List<ParcelBox> buildParcelBoxes() {
        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(1482L);
        parcelBox.setExternalId("23u234rysd");
        parcelBox.setFulfilmentId("473865");
        parcelBox.setWeight(400L);
        parcelBox.setWidth(120L);
        parcelBox.setHeight(80L);
        parcelBox.setDepth(100L);
        parcelBox.setItems(List.of(new ParcelBoxItem() {{
            setId(401234L);
            setParcelBoxId(1482L);
            setItemId(35027L);
            setCount(1);
        }}));
        parcelBox.setParcelId(52L);
        return List.of(parcelBox);
    }

    private static CancellationRequest buildCancellationRequest() {
        return new CancellationRequest(OrderSubstatus.DELIVERED_USER_RECEIVED,
                "notes_value", CancellationRequestStatus.CONFIRMED) {{
            setSubstatusText("substatus_value");
        }};
    }

    private static ItemPrices buildPrices() {
        ItemPrices itemPrices = new ItemPrices();
        itemPrices.setClientBuyerPrice(new BigDecimal("11"));
        itemPrices.setBuyerPriceBeforeDiscount(new BigDecimal("12"));
        itemPrices.setBuyerPriceNominal(new BigDecimal("13"));
        itemPrices.setBuyerDiscount(new BigDecimal("14"));
        itemPrices.setBuyerSubsidy(new BigDecimal("15"));
        itemPrices.setSubsidy(new BigDecimal("16"));
        itemPrices.setReportPrice(new BigDecimal("17"));
        itemPrices.setOldMin(new BigDecimal("18"));
        itemPrices.setFeedPrice(new BigDecimal("19"));
        itemPrices.setPartnerPrice(new BigDecimal("20"));
        itemPrices.setOldDiscountOldMin(new BigDecimal("21"));
        itemPrices.setReportPromoType(ReportPromoType.PROMO_CODE);
        itemPrices.setPriceWithoutVat(new BigDecimal("22"));
        return itemPrices;
    }

    private static Date getDate(String stringDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        LocalDateTime dt = LocalDateTime.parse(stringDate, formatter);
        ZoneOffset offset = ZoneId.systemDefault().getRules().getOffset(dt);
        return Date.from(dt.toInstant(offset));
    }
}
