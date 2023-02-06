package ru.yandex.market.notifier.util.providers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.CollectionUtils;

import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryCustomizer;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.order.ItemPrices;
import ru.yandex.market.checkout.checkouter.order.VatType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.util.StreamUtils;

import static ru.yandex.market.checkout.checkouter.delivery.Delivery.SELF_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.SHOP;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType.YANDEX_MARKET;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CARD_ON_DELIVERY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CASH_ON_DELIVERY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX;

public abstract class DeliveryProvider {

    public static final DeliveryType DELIVERY_TYPE = DeliveryType.DELIVERY;
    public static final Long MARKET_OUTLET_ID = 419585L;
    public static final String SHOP_OUTLET_CODE = "20697";

    public static final Long FREE_MARKET_OUTLET_ID = 419584L;

    public static final Long FF_DELIVERY_SERVICE_ID = 9999L;
    public static final Long FF_FREE_DELIVERY_SERVICE_ID = 9998L;
    /**
     * СД с настройкой по умолчанию на самопривоз
     */
    public static final Long MOCK_DELIVERY_SERVICE_ID = 100501L;
    /**
     * СЦ с захардкожеными настройками для запуска второго СЦ
     */
    public static final Long MOCK_SORTING_CENTER_HARDCODED = 300501L;

    /**
     * Почта России (одна из СД МарДо)
     */
    public static final long RUSPOSTPICKUP_DELIVERY_SERVICE_ID = 123L;
    public static final long REGION_ID = 213L;

    @Nonnull
    public static Delivery getSelfDelivery() {
        return shopSelfDelivery()
                .serviceId(SELF_DELIVERY_SERVICE_ID)
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
    public static Delivery getShopDelivery() {
        return shopSelfDelivery().build();
    }

    @Nonnull
    public static Delivery getShopDeliveryWithPickupType() {
        return shopSelfPickupDeliveryByMarketOutletId()
                .build();
    }

    @Nonnull
    public static Delivery getYandexMarketDelivery(boolean free) {
        return yandexDelivery().courier(free)
                .build();
    }

    @Nonnull
    public static DeliveryBuilder yandexDelivery() {
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

    @Nonnull
    public static DeliveryBuilder yandexPickupDelivery() {
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
    public static DeliveryBuilder shopSelfDelivery() {
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
    public static DeliveryBuilder shopSelfPickupDeliveryByOutletCode() {
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
    public static DeliveryBuilder shopSelfPickupDeliveryByMarketOutletId() {
        return shopSelfDelivery()
                .pickup()
                .serviceId(SELF_DELIVERY_SERVICE_ID)
                .outletId(MARKET_OUTLET_ID);
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

        public DeliveryBuilder outletCode(String outletCode) {
            this.outletCode = outletCode;
            return this;
        }

        public DeliveryBuilder postCode(Long postCode) {
            postCodes.add(postCode);
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

        public DeliveryBuilder buyerPrice(BigDecimal buyerPrice) {
            this.buyerPrice = buyerPrice;
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

        public DeliveryBuilder somePrice() {
            return price(BigDecimal.TEN)
                    .buyerPrice(BigDecimal.TEN);
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
        }
    }
}
