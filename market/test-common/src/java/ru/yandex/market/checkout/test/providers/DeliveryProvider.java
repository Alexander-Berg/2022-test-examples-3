package ru.yandex.market.checkout.test.providers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryCustomizer;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.ExtraCharge;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryInterval;
import ru.yandex.market.checkout.checkouter.delivery.RawDeliveryIntervalsCollection;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.order.ItemPrices;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.time.TestableClock;
import ru.yandex.market.checkout.common.util.DeliveryOptionPartnerType;
import ru.yandex.market.checkout.common.util.StreamUtils;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.DeliveryPromo;
import ru.yandex.market.common.report.model.DeliveryPromoType;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.common.report.model.PickupOption;

import static ru.yandex.market.checkout.checkouter.delivery.Delivery.SELF_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature.ON_DEMAND;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.SHOP;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CARD_ON_DELIVERY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CASH_ON_DELIVERY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX;

public abstract class DeliveryProvider {

    public static final DeliveryType DELIVERY_TYPE = DeliveryType.DELIVERY;
    public static final Long OUTLET_SHOP_ID = 774L;
    public static final Long MARKET_OUTLET_ID = 419585L;
    public static final String SHOP_OUTLET_CODE = "20697";

    public static final Long FREE_MARKET_OUTLET_ID = 419584L;
    public static final String FREE_SHOP_OUTLET_CODE = "20696";

    public static final Long SHOP_OUTLET_POSTCODE_ID = 20698L;
    public static final Long FF_DELIVERY_SERVICE_ID = 9999L;
    public static final Long FF_FREE_DELIVERY_SERVICE_ID = 9998L;
    /**
     * Код отделения почты
     */
    public static final Long POST_OUTLET_ID = 30696L;
    public static final Long ANOTHER_POST_OUTLET_ID = 30697L;
    /**
     * СД с настройкой по умолчанию на самопривоз
     */
    public static final Long MOCK_DELIVERY_SERVICE_ID = 100501L;
    /**
     * СД с настройкой по умолчанию на забор
     */
    public static final Long MOCK_INTAKE_DELIVERY_SERVICE_ID = 100502L;
    /**
     * Еще одна СД с настройкой на самопривоз
     */
    public static final Long ANOTHER_MOCK_DELIVERY_SERVICE_ID = 100503L;
    /**
     * СД с настройкой по умолчанию на самопривоз в постаматы
     */
    public static final Long MOCK_POST_TERM_DELIVERY_SERVICE_ID = 100504L;
    /**
     * СД с настройкой по умолчанию на забор и без настроек на забор
     */
    public static final Long MOCK_ONLY_SELF_EXPORT_DELIVERY_SERVICE_ID = 123L;
    /**
     * СЦ
     */
    public static final Long MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID = 200501L;

    /**
     * СЦ с захардкожеными настройками для запуска второго СЦ
     */
    public static final Long MOCK_SORTING_CENTER_HARDCODED = 300501L;

    /**
     * Еще один склад
     */
    public static final Long ANOTHER_WAREHOUSE_ID = 3005012L;

    /**
     * ид склада партнера подключенного по схеме DropOff
     */
    public static final Long DROPOFF_PARTNER_WAREHOUSE_ID = 400504L;

    /**
     * Почта России (одна из СД МарДо)
     */
    public static final long RUSPOSTPICKUP_DELIVERY_SERVICE_ID = 123L;
    public static final long REGION_ID = 213L;

    /**
     * Почта России МарДо с типом POST
     */
    public static final long RUSPOST_DELIVERY_SERVICE_ID = 100501123L;

    /**
     * Дата и время на которую возможны заборные отгрузки у SHOP_ID_WITH_SORTING_CENTER
     */
    public static final Instant INTAKE_AVAILABLE_DATE =
            LocalDateTime.parse("2027-07-06T10:15:30").atZone(ZoneId.systemDefault()).toInstant();

    public static final String DELIVERY_HASH = "test_hash";

    @Nonnull
    public static Delivery getSelfDelivery() {
        return shopSelfDelivery()
                .serviceId(SELF_DELIVERY_SERVICE_ID)
                .build();
    }

    @Nonnull
    public static Delivery getPostalDelivery() {
        return shopSelfDelivery()
                .post()
                .build();
    }

    @Nonnull
    public static Delivery getEmptyDelivery() {
        return getEmptyDelivery(REGION_ID);
    }

    @Nonnull
    public static Delivery getEmptyDelivery(long regionId) {
        return new Delivery(regionId);
    }

    @Nonnull
    public static Delivery getEmptyDeliveryWithAddress() {
        Delivery delivery = getEmptyDelivery();
        delivery.setBuyerAddress(AddressProvider.getAddress());
        return delivery;
    }

    @Nonnull
    public static Delivery getShopDelivery() {
        return shopSelfDelivery().build();
    }

    @Deprecated
    public static Delivery getGlobalDelivery() {
        return shopSelfDelivery()
                .address(AddressProvider.getEnglishAddress())
                .build();
    }

    @Nonnull
    public static Delivery getShopDeliveryWithPickupType() {
        return shopSelfPickupDeliveryByMarketOutletId()
                .build();
    }

    @Nonnull
    public static Delivery getShopDeliveryWithPostType() {
        return shopSelfDelivery()
                .post()
                .build();
    }

    @Nonnull
    public static Delivery getYandexMarketPickupDelivery() {
        return yandexPickupDelivery().build();
    }

    @Nonnull
    public static Delivery getYandexMarketDelivery(boolean free) {
        return yandexDelivery().courier(free)
                .build();
    }

    @Nonnull
    public static DeliveryProvider.DeliveryBuilder yandexDelivery() {
        return deliveryBuilder()
                .defaultPaymentOptions()
                .somePrice()
                .today()
                .someAddress()
                .someRecipient()
                .somePrices()
                .yandex()
                .courier(false);
    }

    public static Delivery getOnDemandDelivery() {
        return yandexDelivery()
                .features(Set.of(ON_DEMAND))
                .build();
    }

    @Nonnull
    public static DeliveryProvider.DeliveryBuilder yandexPickupDelivery() {
        return deliveryBuilder()
                .defaultPaymentOptions()
                .somePrice()
                .somePrices()
                .today()
                .someAddress()
                .someRecipient()
                .yandex()
                .pickup()
                .outletId(MARKET_OUTLET_ID);
    }

    @Nonnull
    public static DeliveryProvider.DeliveryBuilder shopSelfDelivery() {
        return deliveryBuilder()
                .defaultPaymentOptions()
                .shop()
                .somePrice()
                .somePrices()
                .today()
                .someAddress()
                .someRecipient();
    }

    /**
     * Должен использоваться только для ответов пуш-апи.
     *
     * @return Пикап по оутлет коду от магазина.
     */
    @Nonnull
    public static DeliveryProvider.DeliveryBuilder shopSelfPickupDeliveryByOutletCode() {
        return shopSelfDelivery()
                .pickup()
                .serviceId(SELF_DELIVERY_SERVICE_ID)
                .outletCode(SHOP_OUTLET_CODE);
    }

    /**
     * Должен использоваться только для ответов репорта и выбранной опции.
     *
     * @return Пикап по оутлету репорта.
     */
    @Nonnull
    public static DeliveryProvider.DeliveryBuilder shopSelfPickupDeliveryByMarketOutletId() {
        return shopSelfDelivery()
                .pickup()
                .serviceId(SELF_DELIVERY_SERVICE_ID)
                .outletId(MARKET_OUTLET_ID);
    }

    @Nonnull
    public static DeliveryProvider.DeliveryBuilder shopSelfPostDelivery() {
        return shopSelfDelivery()
                .post()
                .serviceId(SELF_DELIVERY_SERVICE_ID)
                .nextDays(1, 14)
                .postCode(111222L);
    }

    @Nonnull
    public static DeliveryProvider.DeliveryBuilder shopFreePickupDeliveryByMarketOutletId() {
        return shopSelfPickupDeliveryByMarketOutletId()
                .serviceName("Доставка бесплатная")
                .serviceId(SELF_DELIVERY_SERVICE_ID)
                .outletId(FREE_MARKET_OUTLET_ID);
    }

    @Nonnull
    public static DeliveryProvider.DeliveryBuilder shopFreePickupDeliveryByOutletCode() {
        return shopSelfPickupDeliveryByOutletCode()
                .serviceName("Доставка бесплатная")
                .serviceId(SELF_DELIVERY_SERVICE_ID)
                .outletCode(FREE_SHOP_OUTLET_CODE);
    }

    @Nonnull
    public static DeliveryProvider.DeliveryBuilder digitalDelivery() {
        return deliveryBuilder()
                .today()
                .paymentOption(YANDEX)
                .shop()
                .price(BigDecimal.ZERO)
                .buyerPrice(BigDecimal.ZERO)
                .someRecipient()
                .type(DeliveryType.DIGITAL)
                .serviceName("Digital delivery");
    }

    @Nonnull
    public static DeliveryDates getDeliveryDates() {
        return getDeliveryDates(LocalDate.now());
    }

    @Nonnull
    public static DeliveryDates getDeliveryDates(LocalDateTime from, LocalDateTime to) {
        return new DeliveryDates(
                Date.from(from.atZone(ZoneId.systemDefault()).toInstant()),
                Date.from(to.atZone(ZoneId.systemDefault()).toInstant())
        );
    }

    @Nonnull
    public static DeliveryDates getDeliveryDates(LocalDate from, LocalDate to) {
        return getDeliveryDates(from.atStartOfDay(), to.atStartOfDay());
    }

    @Nonnull
    public static DeliveryDates getDeliveryDates(LocalDate from) {
        return getDeliveryDates(from, from.plusDays(1));
    }

    @Nonnull
    @Deprecated
    public static Delivery getByDelivery(@Nonnull Delivery delivery, @Nonnull Date shipmentDate) {
        ShopOutlet shopOutlet = new ShopOutlet();
        shopOutlet.setShipmentDate(shipmentDate);
        shopOutlet.setInletId(222L);
        shopOutlet.setRegionId(213L);

        Delivery deliveryRequest = DeliveryProvider.getEmptyDelivery();
        deliveryRequest.setType(DeliveryType.PICKUP);
        deliveryRequest.setDeliveryPartnerType(delivery.getDeliveryPartnerType());
        deliveryRequest.setServiceName("Russian post");
        deliveryRequest.setPrice(new BigDecimal("1.11"));
        deliveryRequest.setPrice(new BigDecimal("1.11"));
        deliveryRequest.setDeliveryDates(new DeliveryDates(shipmentDate, shipmentDate));
        deliveryRequest.setOutletId(111L);
        deliveryRequest.setOutletCode("111");
        deliveryRequest.setOutlet(shopOutlet);
        deliveryRequest.setDeliveryServiceId(delivery.getDeliveryServiceId());
        deliveryRequest.setHash(delivery.getHash());
        return deliveryRequest;
    }

    @Nonnull
    public static DeliveryBuilder deliveryBuilder() {
        return new DeliveryBuilder();
    }

    @Nonnull
    @SuppressWarnings("deprecation")
    public static DeliveryBuilder createFrom(@Nonnull Delivery delivery) {
        return new DeliveryBuilder()
                .id(delivery.getId())
                .hash(delivery.getHash())
                .optionId(delivery.getDeliveryOptionId())
                .dates(delivery.getDeliveryDates())
                .validatedDates(delivery.getValidatedDeliveryDates())
                .regionId(delivery.getRegionId())
                .buyerAddress(delivery.getBuyerAddress())
                .shopAddress(delivery.getShopAddress())
                .outletId(delivery.getOutletId())
                .outletCode(delivery.getOutletCode())
                .outlets(delivery.getOutlets())
                .serviceId(delivery.getDeliveryServiceId())
                .serviceName(delivery.getServiceName())
                .partnerType(delivery.getDeliveryPartnerType())
                .type(delivery.getType())
                .vatType(delivery.getVat())
                .price(delivery.getPrice())
                .buyerPrice(delivery.getBuyerPrice())
                .paymentOptions(delivery.getPaymentOptions())
                .prices(delivery.getPrices())
                .features(delivery.getFeatures());
    }

    @Nonnull
    public static <T extends Delivery> T buildShopDeliveryResponse(Supplier<T> supplier) {
        return shopSelfDelivery()
                .buildResponse(supplier);
    }

    @Deprecated
    public static <T extends Delivery> T buildPostpaidDeliveryResponse(Supplier<T> supplier) {
        T response = buildShopDeliveryResponse(supplier);
        Set<PaymentMethod> paymentMethods = new HashSet<>();
        paymentMethods.add(CASH_ON_DELIVERY);
        paymentMethods.add(CARD_ON_DELIVERY);
        response.setPaymentOptions(paymentMethods);
        return response;
    }

    public static <T extends Delivery> T buildDeliveryResponseWithIntervals(Supplier<T> supplier) {
        RawDeliveryIntervalsCollection rawDeliveryIntervals = new RawDeliveryIntervalsCollection();
        rawDeliveryIntervals.add(new RawDeliveryInterval(DateUtil.getToday(), LocalTime.of(10, 0), LocalTime.of(12,
                0)));
        rawDeliveryIntervals.add(new RawDeliveryInterval(DateUtil.getToday(), LocalTime.of(16, 0), LocalTime.of(18,
                0)));

        T response = buildShopDeliveryResponse(supplier);
        response.setRawDeliveryIntervals(rawDeliveryIntervals);

        return response;
    }

    @Nonnull
    public static <T extends Delivery> T createYandexDeliveryResponse(boolean free, @Nonnull Supplier<T> supplier) {
        return yandexDelivery()
                .courier(free)
                .nextDays(2)
                .buildResponse(supplier);
    }

    @Nonnull
    public static <T extends Delivery> T buildPickupDeliveryResponseWithOutletCode(Supplier<T> supplier) {
        return shopSelfPickupDeliveryByOutletCode()
                .buildResponse(supplier);
    }

    @Nonnull
    public static <T extends Delivery> T buildShopPostDeliveryResponse(Supplier<T> supplier) {
        return shopSelfPostDelivery()
                .buildResponse(supplier);
    }

    @Nonnull
    public static <T extends Delivery> T buildDigitalDeliveryResponse(Supplier<T> supplier) {
        return digitalDelivery()
                .buildResponse(supplier);
    }

    @Nonnull
    public static List<DeliveryProvider.DeliveryBuilder> defaultDeliveryList(@Nonnull OrderAcceptMethod acceptMethod) {
        if (acceptMethod == OrderAcceptMethod.PUSH_API) {
            return List.of(
                    //shop courier delivery (99 self delivery)
                    shopSelfDelivery(),
                    //shop pickup delivery (99 self delivery)
                    //Пуш апи пока не может ответить маркетным id оутлета - костыль
                    shopSelfPickupDeliveryByOutletCode(),
                    //shop pickup delivery (99 self delivery)
                    shopSelfPostDelivery(),
                    //Free shop pickup delivery
                    //Пуш апи пока не может ответить маркетным id оутлета - костыль
                    shopFreePickupDeliveryByOutletCode()
            );
        } else {
            return List.of(
                    //shop courier delivery (99 self delivery)
                    shopSelfDelivery(),
                    //shop pickup delivery (99 self delivery)
                    //Пуш апи пока не может ответить маркетным id оутлета - костыль
                    shopSelfPickupDeliveryByMarketOutletId(),
                    //shop pickup delivery (99 self delivery)
                    shopSelfPostDelivery(),
                    //Free shop pickup delivery
                    //Пуш апи пока не может ответить маркетным id оутлета - костыль
                    shopFreePickupDeliveryByMarketOutletId(),
                    //yandex free courier delivery (9998)
                    yandexDelivery()
                            .courier(true),
                    //yandex courier delivery (9999)
                    yandexDelivery()
                            .courier(false),
                    //yandex pickup delivery (100501)
                    yandexPickupDelivery()
            );
        }
    }

    public static class DeliveryBuilder {

        private String id;
        private String hash;
        private String optionId;
        private Instant shipmentDate;
        private DeliveryDates dates;
        private DeliveryDates validatedDates;
        private Long regionId;
        private Address address;
        private Address buyerAddress;
        private Address shopAddress;
        private Recipient recipient;
        private Long outletId;
        private Set<Long> outletIds = new HashSet<>();
        private String outletCode;
        private Set<String> outletCodes = new HashSet<>();
        private Set<Long> postCodes = new HashSet<>();
        private Set<ShopOutlet> outlets = new HashSet<>();
        private Long serviceId;
        private DeliveryPartnerType partnerType;
        private DeliveryType type;
        private VatType vatType;
        private String serviceName;
        private BigDecimal price;
        private BigDecimal discount;
        private BigDecimal buyerPrice;
        private Boolean leaveAtTheDoor;
        private List<DeliveryCustomizer> customizers;
        private Set<PaymentMethod> paymentOptions = new HashSet<>();
        private ItemPrices prices = new ItemPrices();
        private Set<DeliveryFeature> features = new HashSet<>();
        private boolean marketBranded;
        private boolean marketPartner;
        private boolean marketPostTerm;
        private Boolean isTryingAvailable;
        private Boolean estimated;
        private ExtraCharge extraCharge;

        private DeliveryBuilder() {
            regionId(REGION_ID);
            partnerType(YANDEX_MARKET);
            type(DELIVERY_TYPE);
            serviceName("Доставка");
            serviceId(SELF_DELIVERY_SERVICE_ID);
            vatType(VatType.VAT_10);
        }

        @Nonnull
        public DeliveryType getType() {
            return Objects.requireNonNull(type);
        }

        @Nonnull
        public DeliveryPartnerType getPartnerType() {
            return Objects.requireNonNull(partnerType);
        }

        public DeliveryBuilder someId() {
            return id(UUID.randomUUID().toString());
        }

        public DeliveryBuilder emptyId() {
            return id(null);
        }

        public DeliveryBuilder id(String id) {
            this.id = id;
            return this;
        }

        public DeliveryBuilder hash(String hash) {
            this.hash = hash;
            return this;
        }

        public DeliveryBuilder optionId(String optionId) {
            this.optionId = optionId;
            return this;
        }

        public DeliveryBuilder dates(DeliveryDates dates) {
            this.dates = dates;
            return this;
        }

        public DeliveryBuilder shipmentDate(Instant shipmentDate) {
            this.shipmentDate = shipmentDate;
            return this;
        }

        public DeliveryBuilder today() {
            return dates(getDeliveryDates());
        }

        public DeliveryBuilder nextDays(int days) {
            var day = LocalDate.now();
            return dates(getDeliveryDates(day, day.plusDays(days)));
        }

        public DeliveryBuilder nextDays(int daysFrom, int daysTo) {
            var day = LocalDate.now();
            return dates(getDeliveryDates(day.plusDays(daysFrom), day.plusDays(daysTo)));
        }

        public DeliveryBuilder validatedDates(DeliveryDates validatedDates) {
            this.validatedDates = validatedDates;
            return this;
        }

        public DeliveryBuilder regionId(Long regionId) {
            this.regionId = regionId;
            return this;
        }

        public DeliveryBuilder address(Address address) {
            this.address = address;
            return this;
        }

        public DeliveryBuilder buyerAddress(Address buyerAddress) {
            this.buyerAddress = buyerAddress;
            return this;
        }

        public DeliveryBuilder shopAddress(Address shopAddress) {
            this.shopAddress = shopAddress;
            return this;
        }

        public DeliveryBuilder recipient(Recipient recipient) {
            this.recipient = recipient;
            return this;
        }

        public DeliveryBuilder outletId(Long outletId) {
            this.outletId = outletId;
            return this;
        }

        public DeliveryBuilder outletIds(@Nullable Collection<Long> outletIds) {
            this.outletIds = outletIds == null ? null : new HashSet<>(outletIds);
            return this;
        }

        public DeliveryBuilder outletCode(String outletCode) {
            this.outletCode = outletCode;
            return this;
        }

        public DeliveryBuilder outletCodes(Collection<String> outletCodes) {
            this.outletCodes = outletCodes == null ? new HashSet<>() : new HashSet<>(outletCodes);
            return this;
        }

        public DeliveryBuilder postCode(Long postCode) {
            postCodes.add(postCode);
            return this;
        }

        public DeliveryBuilder postCodes(Collection<Long> postCodes) {
            this.postCodes = postCodes == null ? new HashSet<>() : new HashSet<>(postCodes);
            return this;
        }

        public DeliveryBuilder outlets(Collection<ShopOutlet> outlets) {
            this.outlets = outlets == null ? new HashSet<>() : new HashSet<>(outlets);
            return this;
        }

        public DeliveryBuilder serviceId(Long deliveryServiceId) {
            this.serviceId = deliveryServiceId;
            return this;
        }

        public DeliveryBuilder marketBranded(boolean marketBranded) {
            this.marketBranded = marketBranded;
            return this;
        }

        public DeliveryBuilder marketPartner(boolean marketPartner) {
            this.marketPartner = marketPartner;
            return this;
        }

        public DeliveryBuilder marketPostTerm(boolean marketPostTerm) {
            this.marketPostTerm = marketPostTerm;
            return this;
        }

        public DeliveryBuilder partnerType(DeliveryPartnerType deliveryPartnerType) {
            this.partnerType = deliveryPartnerType;
            return this;
        }

        public DeliveryBuilder type(DeliveryType type) {
            this.type = type;
            return this;
        }

        public DeliveryBuilder vatType(VatType vatType) {
            this.vatType = vatType;
            return this;
        }

        public DeliveryBuilder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public DeliveryBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public DeliveryBuilder discount(BigDecimal discount) {
            this.discount = discount;
            return this;
        }

        public DeliveryBuilder buyerPrice(BigDecimal buyerPrice) {
            this.buyerPrice = buyerPrice;
            return this;
        }

        public DeliveryBuilder leaveAtTheDoor(Boolean leaveAtTheDoor) {
            this.leaveAtTheDoor = leaveAtTheDoor;
            return this;
        }

        public DeliveryBuilder customizers(List<DeliveryCustomizer> customizers) {
            this.customizers = customizers;
            return this;
        }

        public DeliveryBuilder paymentOption(PaymentMethod paymentOption) {
            paymentOptions.add(paymentOption);
            return this;
        }

        public DeliveryBuilder paymentOptions(Collection<PaymentMethod> paymentOptions) {
            this.paymentOptions = new HashSet<>(paymentOptions);
            return this;
        }

        public DeliveryBuilder prices(ItemPrices prices) {
            this.prices = prices;
            return this;
        }

        public DeliveryBuilder isTryingAvailable(Boolean isTryingAvailable) {
            this.isTryingAvailable = isTryingAvailable;
            return this;
        }

        public DeliveryBuilder shop() {
            return partnerType(SHOP);
        }

        public DeliveryBuilder yandex() {
            return partnerType(YANDEX_MARKET);
        }

        public DeliveryBuilder pickup() {
            return type(DeliveryType.PICKUP)
                    .serviceId(MOCK_DELIVERY_SERVICE_ID)
                    .serviceName("Самовывоз");
        }

        public DeliveryBuilder pickup(boolean marketBranded) {
            return pickup()
                    .marketBranded(marketBranded);
        }

        public DeliveryBuilder somePrice() {
            return price(BigDecimal.TEN)
                    .buyerPrice(BigDecimal.TEN);
        }

        public DeliveryBuilder free() {
            return price(BigDecimal.ZERO)
                    .buyerPrice(BigDecimal.ZERO);
        }

        public DeliveryBuilder someAddress() {
            return shopAddress(AddressProvider.getAddress())
                    .buyerAddress(AddressProvider.getAddress());
        }

        public DeliveryBuilder someRecipient() {
            return recipient(RecipientProvider.getDefaultRecipient());
        }

        public DeliveryBuilder courier(boolean free) {
            serviceId(free ? FF_FREE_DELIVERY_SERVICE_ID : FF_DELIVERY_SERVICE_ID)
                    .serviceName("Доставка")
                    .type(DeliveryType.DELIVERY);

            if (free) {
                buyerPrice(new BigDecimal("0"))
                        .price(new BigDecimal("0"))
                        .nextDays(3, 5);
            } else {
                buyerPrice(new BigDecimal("100"))
                        .price(new BigDecimal("100"))
                        .nextDays(2);
            }
            return this;
        }

        public DeliveryBuilder post() {
            return type(DeliveryType.POST)
                    .serviceId(RUSPOSTPICKUP_DELIVERY_SERVICE_ID)
                    .serviceName("Почтомат");
        }

        public DeliveryBuilder defaultPaymentOptions() {
            paymentOptions = new HashSet<>(Arrays.asList(CASH_ON_DELIVERY, CARD_ON_DELIVERY, YANDEX));
            return this;
        }

        public DeliveryBuilder somePrices() {
            return prices(ItemPricesProvider.getDefaultItemPrices());
        }

        public DeliveryBuilder features(Set<DeliveryFeature> features) {
            this.features = features;
            return this;
        }

        public DeliveryBuilder estimated(Boolean estimated) {
            this.estimated = estimated;
            return this;
        }

        public DeliveryBuilder extraCharge(ExtraCharge extraCharge) {
            this.extraCharge = extraCharge;
            return this;
        }

        public Delivery build() {
            Delivery delivery = new Delivery();
            setParams(delivery);
            return delivery;
        }

        public <T extends Delivery> T buildResponse(Supplier<T> provider) {
            T delivery = provider.get();
            setParams(delivery);
            return delivery;
        }

        public LocalDeliveryOption buildLocalDeliveryOption() {
            switch (type) {
                case DELIVERY:
                    return buildActualDeliveryOption();
                case POST:
                case PICKUP:
                    return buildPickupOption();
                default:
                    throw new UnsupportedOperationException("Type is not supported");
            }
        }

        public ActualDeliveryOption buildActualDeliveryOption() {
            return buildActualDeliveryOption(TestableClock.getInstance());
        }

        public ActualDeliveryOption buildActualDeliveryOption(Clock clock) {
            ActualDeliveryOption actualDeliveryOption = new ActualDeliveryOption();
            actualDeliveryOption.setPrice(price);
            actualDeliveryOption.setSupplierPrice(price);
            if (discount != null) {
                var promo = new DeliveryPromo();
                promo.setDiscountType(DeliveryPromoType.YANDEX_PLUS);
                promo.setOldPrice(price.add(discount));
                actualDeliveryOption.setDiscount(promo);
            }
            actualDeliveryOption.setDeliveryServiceId(serviceId);
            actualDeliveryOption.setDayFrom(Period.between(LocalDate.now(clock),
                    LocalDate.ofInstant(dates.getFromDate().toInstant(), clock.getZone())).getDays());
            actualDeliveryOption.setDayTo(Period.between(LocalDate.now(clock),
                    LocalDate.ofInstant(dates.getToDate().toInstant(), clock.getZone())).getDays());
            actualDeliveryOption.setPaymentMethods(paymentOptions.stream()
                    .map(PaymentMethod::name)
                    .collect(Collectors.toSet()));
            actualDeliveryOption.setCurrency(Currency.RUR);
            actualDeliveryOption.setPartnerType(partnerType == SHOP ?
                    DeliveryOptionPartnerType.REGULAR.getReportName() :
                    DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
            actualDeliveryOption.setShipmentDay(actualDeliveryOption.getDayFrom());
            actualDeliveryOption.setLeaveAtTheDoor(leaveAtTheDoor);
            actualDeliveryOption.setSupplierShipmentDateTime(shipmentDate);
            actualDeliveryOption.setCustomizers(toReportCustomizers(customizers));
            actualDeliveryOption.setIsDeferredCourier(features != null
                    && features.contains(DeliveryFeature.DEFERRED_COURIER));
            actualDeliveryOption.setTryingAvailable(isTryingAvailable);
            actualDeliveryOption.setEstimated(estimated);
            return actualDeliveryOption;
        }

        public PickupOption buildPickupOption() {
            return buildPickupOption(TestableClock.getInstance());
        }

        public PickupOption buildPickupOption(Clock clock) {
            PickupOption pickupOption = new PickupOption();
            pickupOption.setPrice(price);
            pickupOption.setSupplierPrice(price);
            if (discount != null) {
                var promo = new DeliveryPromo();
                promo.setDiscountType(DeliveryPromoType.YANDEX_PLUS);
                promo.setOldPrice(price.add(discount));
                pickupOption.setDiscount(promo);
            }
            pickupOption.setDeliveryServiceId(serviceId);
            pickupOption.setDayFrom(Period.between(LocalDate.now(clock),
                    LocalDate.ofInstant(dates.getFromDate().toInstant(), clock.getZone())).getDays());
            pickupOption.setDayTo(Period.between(LocalDate.now(clock),
                    LocalDate.ofInstant(dates.getToDate().toInstant(), clock.getZone())).getDays());
            pickupOption.setPaymentMethods(paymentOptions.stream()
                    .map(PaymentMethod::name)
                    .collect(Collectors.toSet()));
            pickupOption.setCurrency(Currency.RUR);
            pickupOption.setPartnerType(partnerType == SHOP ?
                    DeliveryOptionPartnerType.REGULAR.getReportName() :
                    DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
            pickupOption.setShipmentDay(pickupOption.getDayFrom());
            pickupOption.setOutletIds(Stream.concat(Stream.ofNullable(outletId), StreamUtils.stream(outletIds))
                    .collect(Collectors.toUnmodifiableList()));
            if (CollectionUtils.isNotEmpty(postCodes)) {
                pickupOption.setPostCodes(new ArrayList<>(postCodes));
            }
            pickupOption.setTryingAvailable(isTryingAvailable);
            return pickupOption;
        }

        private List<ru.yandex.market.common.report.model.DeliveryCustomizer> toReportCustomizers(
                List<DeliveryCustomizer> checkouterCustomizers
        ) {
            if (checkouterCustomizers == null) {
                return null;
            }
            return checkouterCustomizers.stream()
                    .map(cdc -> new ru.yandex.market.common.report.model.DeliveryCustomizer(
                            cdc.getKey(),
                            cdc.getName(),
                            cdc.getType())
                    )
                    .collect(Collectors.toList());
        }

        @SuppressWarnings("deprecation")
        private void setParams(Delivery delivery) {
            delivery.setId(id);
            delivery.setHash(hash);
            delivery.setDeliveryOptionId(optionId);
            delivery.setDeliveryDates(dates);
            delivery.setValidatedDeliveryDates(validatedDates);
            delivery.setRegionId(regionId);
            delivery.setAddressForJson(address);
            delivery.setBuyerAddressForJson(buyerAddress);
            delivery.setShopAddressForJson(shopAddress);
            delivery.setRecipient(recipient);
            delivery.setOutletId(outletId);
            delivery.setOutletIds(Stream.concat(
                    Stream.ofNullable(outletId),
                    StreamUtils.stream(outletIds)
            ).collect(Collectors.toUnmodifiableSet()));

            delivery.setOutletCode(outletCode);
            delivery.setOutletCodes(Stream.concat(
                    Stream.ofNullable(outletCode),
                    StreamUtils.stream(outletCodes)
            ).collect(Collectors.toUnmodifiableSet()));
            if (CollectionUtils.isNotEmpty(outlets)) {
                delivery.setOutlets(new ArrayList<>(outlets));
            }
            if (CollectionUtils.isNotEmpty(postCodes)) {
                delivery.setPostCodes(new ArrayList<>(postCodes));
            }
            delivery.setDeliveryServiceId(serviceId);
            delivery.setDeliveryPartnerType(partnerType);
            delivery.setType(type);
            delivery.setVat(vatType);
            delivery.setServiceName(serviceName);
            delivery.setPrice(price);
            delivery.setSupplierPrice(price);
            if (discount != null) {
                delivery.setSupplierDiscount(discount);
            }
            delivery.setPrices(prices);
            delivery.setBuyerPrice(buyerPrice);
            delivery.setPaymentOptions(paymentOptions);
            delivery.setFeatures(features);
            delivery.setLeaveAtTheDoor(leaveAtTheDoor);
            delivery.setCustomizers(customizers);
            delivery.setMarketBranded(marketBranded);
            delivery.setMarketPartner(marketPartner);
            delivery.setMarketPostTerm(marketPostTerm);
            delivery.setTryingAvailable(isTryingAvailable);
            delivery.setEstimated(estimated);
            delivery.setExtraCharge(extraCharge);
        }
    }
}
