package ru.yandex.market.checkout.test.providers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.checkouter.delivery.AvailableDeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.util.DeliveryOptionPartnerType;
import ru.yandex.market.common.report.model.ActualDelivery;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;
import ru.yandex.market.common.report.model.DeliveryOffer;
import ru.yandex.market.common.report.model.DeliveryPromo;
import ru.yandex.market.common.report.model.DeliveryPromoType;
import ru.yandex.market.common.report.model.DeliveryTimeInterval;
import ru.yandex.market.common.report.model.DeliveryTypeDistribution;
import ru.yandex.market.common.report.model.LocalDeliveryOption;
import ru.yandex.market.common.report.model.OfferProblem;
import ru.yandex.market.common.report.model.PickupOption;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.RUSPOST_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryResultProvider.getActualDeliveryResult;
import static ru.yandex.market.checkout.test.providers.LocalDeliveryOptionProvider.RED_DELIVERY_SERVICE_ID;

/**
 * @author mmetlov
 */
public abstract class ActualDeliveryProvider {

    public static final BigDecimal FREE_DELIVERY_REMAINDER = BigDecimal.valueOf(1000L);
    public static final BigDecimal DELIVERY_PRICE = BigDecimal.valueOf(100);
    public static final BigDecimal PICKUP_PRICE = BigDecimal.valueOf(100);
    public static final BigDecimal POST_PRICE = BigDecimal.valueOf(101);
    public static final Long PICKUP_OUTLET_ID = 12312301L;
    public static final Long POST_TERM_OUTLET_ID = 12312305L;

    public static ActualDeliveryBuilder builder() {
        return new ActualDeliveryBuilder();
    }

    public static ActualDelivery defaultActualDelivery() {
        ActualDelivery actualDelivery = new ActualDelivery();
        actualDelivery.setFreeDeliveryRemainder(FREE_DELIVERY_REMAINDER);
        actualDelivery.setResults(singletonList(getActualDeliveryResult()));
        return actualDelivery;
    }

    public static class ActualDeliveryBuilder {

        public static final Integer DEFAULT_INTAKE_SHIPMENT_DAYS = (int) Math.ceil(
                DateUtil.diffInDays(DateUtil.convertDotDateFormat("07.07.2037"), DateUtil.now())
        );
        private List<ActualDeliveryOption> deliveryOptions = new ArrayList<>();
        private List<PickupOption> pickupOptions = new ArrayList<>();
        private List<PickupOption> postOptions = new ArrayList<>();
        private List<OfferProblem> offerProblems = new ArrayList<>();
        private List<String> commonProblems = new ArrayList<>();
        private List<DeliveryOffer> offers = new ArrayList<>();
        private BigDecimal weight;
        private List<BigDecimal> dimensions = new ArrayList<>();
        private Boolean isLargeSize;
        private LocalDeliveryOption lastOption;
        private DeliveryTypeDistribution bucketAll;
        private DeliveryTypeDistribution bucketActive;
        private DeliveryTypeDistribution carrierAll;
        private DeliveryTypeDistribution carrierActive;
        private List<AvailableDeliveryType> availableDeliveryTypes = new ArrayList<>();
        private boolean isFree = false;
        private Boolean isExternalLogistics;

        public ActualDeliveryBuilder withFreeDelivery() {
            isFree = true;
            return this;
        }

        public ActualDeliveryBuilder withIsExternalLogistics(Boolean isExternalLogistics) {
            this.isExternalLogistics = isExternalLogistics;
            return this;
        }

        public ActualDeliveryBuilder addWeight(BigDecimal weight) {
            this.weight = weight;
            return this;
        }

        public ActualDeliveryBuilder addLargeSize(Boolean isLargeSize) {
            this.isLargeSize = isLargeSize;
            return this;
        }

        public ActualDeliveryBuilder addDimensions(List<BigDecimal> dimensions) {
            this.dimensions = dimensions;
            return this;
        }

        public ActualDeliveryBuilder addBucketActive(DeliveryTypeDistribution distribution) {
            this.bucketActive = distribution;
            return this;
        }

        public ActualDeliveryBuilder addBucketAll(DeliveryTypeDistribution distribution) {
            this.bucketAll = distribution;
            return this;
        }

        public ActualDeliveryBuilder addCarriersActive(DeliveryTypeDistribution distribution) {
            this.carrierActive = distribution;
            return this;
        }

        public ActualDeliveryBuilder addCarriersAll(DeliveryTypeDistribution distribution) {
            this.carrierAll = distribution;
            return this;
        }

        public ActualDeliveryBuilder addOffers(List<DeliveryOffer> offers) {
            this.offers = offers;
            return this;
        }

        public ActualDeliveryBuilder addDelivery(ActualDeliveryOption option) {
            deliveryOptions.add(option);
            lastOption = option;
            return this;
        }

        public ActualDeliveryBuilder addDelivery(Long deliveryServiceId) {
            return addDelivery(deliveryServiceId, DEFAULT_INTAKE_SHIPMENT_DAYS);
        }

        public ActualDeliveryBuilder addDelivery(
                Long deliveryServiceId, int dayFrom, int dayTo, DeliveryTimeInterval interval) {
            ActualDeliveryOption option = new ActualDeliveryOption();
            option.setDeliveryServiceId(deliveryServiceId);
            option.setDayFrom(dayFrom);
            option.setDayTo(dayTo);
            option.setTimeIntervals(List.of(interval));
            option.setCost(BigDecimal.ZERO);
            option.setCurrency(Currency.RUR);
            return addDelivery(option);
        }

        public ActualDeliveryBuilder addDelivery(Long deliveryServiceId, Integer shipmentDay) {
            return addDelivery(deliveryServiceId, shipmentDay, null,
                    Duration.ofHours(23), null, null);
        }

        public ActualDeliveryBuilder addDeliveryWithPartnerType(Long deliveryServiceId,
                                                                DeliveryOptionPartnerType partnerType) {
            return addDelivery(deliveryServiceId, DEFAULT_INTAKE_SHIPMENT_DAYS, null,
                    Duration.ofHours(23), null, null,
                    0, 2, partnerType, null);
        }

        public ActualDeliveryBuilder addDelivery(Long deliveryServiceId, Integer shipmentDay,
                                                 BigDecimal deliveryPrice) {
            ActualDeliveryOption deliveryOption = new ActualDeliveryOption();
            deliveryOption.setDeliveryServiceId(deliveryServiceId);
            deliveryOption.setDayFrom(0);
            deliveryOption.setDayTo(2);
            deliveryOption.setPaymentMethods(new HashSet<>(Arrays.asList(PaymentMethod.YANDEX.name(),
                    PaymentMethod.CARD_ON_DELIVERY.name(),
                    PaymentMethod.CASH_ON_DELIVERY.name()))
            );
            deliveryOption.setPrice(deliveryPrice);
            deliveryOption.setCurrency(Currency.RUR);
            deliveryOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
            deliveryOption.setShipmentDay(shipmentDay);
            deliveryOption.setPackagingTime(Duration.ofHours(23));
            deliveryOption.setPriceForShop(BigDecimal.valueOf(123L));
            deliveryOption.setPriceForShopCurrency(Currency.RUR);
            deliveryOption.setSupplierShipmentDateTime(null);

            deliveryOptions.add(deliveryOption);
            lastOption = deliveryOption;
            return this;
        }

        public ActualDeliveryBuilder addDelivery(Long deliveryServiceId, Integer shipmentDay,
                                                 Instant supplierShipmentDateTime,
                                                 Duration packagingTime,
                                                 ZonedDateTime shipmentDateTimeBySupplier,
                                                 ZonedDateTime receptionDateTimeByWarehouse) {
            return addDelivery(deliveryServiceId, shipmentDay, supplierShipmentDateTime, packagingTime,
                    shipmentDateTimeBySupplier, receptionDateTimeByWarehouse, 0, 2, null);
        }

        @SuppressWarnings("checkstyle:ParameterNumber")
        public ActualDeliveryBuilder addDelivery(Long deliveryServiceId, Integer shipmentDay,
                                                 Instant supplierShipmentDateTime,
                                                 Duration packagingTime,
                                                 ZonedDateTime shipmentDateTimeBySupplier,
                                                 ZonedDateTime receptionDateTimeByWarehouse,
                                                 int dayFrom,
                                                 int dayTo,
                                                 Consumer<ActualDeliveryOption> optionEnricher
        ) {
            return addDelivery(deliveryServiceId, shipmentDay, supplierShipmentDateTime, packagingTime,
                    shipmentDateTimeBySupplier, receptionDateTimeByWarehouse, dayFrom, dayTo,
                    DeliveryOptionPartnerType.MARKET_DELIVERY, optionEnricher
            );
        }

        @SuppressWarnings("checkstyle:ParameterNumber")
        public ActualDeliveryBuilder addDelivery(Long deliveryServiceId, Integer shipmentDay,
                                                 Instant supplierShipmentDateTime,
                                                 Duration packagingTime,
                                                 ZonedDateTime shipmentDateTimeBySupplier,
                                                 ZonedDateTime receptionDateTimeByWarehouse,
                                                 int dayFrom,
                                                 int dayTo,
                                                 DeliveryOptionPartnerType partnerType,
                                                 Consumer<ActualDeliveryOption> optionEnricher) {
            ActualDeliveryOption deliveryOption = new ActualDeliveryOption();
            deliveryOption.setDeliveryServiceId(deliveryServiceId);
            deliveryOption.setDayFrom(dayFrom);
            deliveryOption.setDayTo(dayTo);
            deliveryOption.setPaymentMethods(new HashSet<>(Arrays.asList(
                    PaymentMethod.YANDEX.name(),
                    PaymentMethod.CARD_ON_DELIVERY.name(),
                    PaymentMethod.CASH_ON_DELIVERY.name()))
            );
            deliveryOption.setPrice(DELIVERY_PRICE);
            deliveryOption.setCurrency(Currency.RUR);
            deliveryOption.setPartnerType(partnerType.getReportName());
            deliveryOption.setShipmentDay(shipmentDay);
            deliveryOption.setPackagingTime(packagingTime);
            deliveryOption.setPriceForShop(BigDecimal.valueOf(123L));
            deliveryOption.setPriceForShopCurrency(Currency.RUR);
            deliveryOption.setSupplierShipmentDateTime(supplierShipmentDateTime);
            deliveryOption.setShipmentDateTimeBySupplier(shipmentDateTimeBySupplier);
            deliveryOption.setReceptionDateTimeByWarehouse(receptionDateTimeByWarehouse);
            deliveryOption.setTimeIntervals(singletonList(new DeliveryTimeInterval(
                    LocalTime.of(10, 00),
                    LocalTime.of(18, 00))));

            if (optionEnricher != null) {
                optionEnricher.accept(deliveryOption);
            }
            deliveryOption.setWideExpress(true);
            deliveryOption.setFastestExpress(true);
            deliveryOptions.add(deliveryOption);
            lastOption = deliveryOption;
            return this;
        }

        public ActualDeliveryBuilder addPickup(Long deliveryServiceId, Integer shipmentDay) {
            return addPickup(deliveryServiceId, shipmentDay, Collections.singletonList(PICKUP_OUTLET_ID));
        }

        public ActualDeliveryBuilder addPickup(Long deliveryServiceId, Integer shipmentDay,
                                               Instant supplierShipmentDateTime,
                                               Duration packagingTime,
                                               ZonedDateTime shipmentDateTimeBySupplier,
                                               ZonedDateTime receptionDateTimeByWarehouse) {
            return addPickup(deliveryServiceId, shipmentDay,
                    supplierShipmentDateTime, Collections.singletonList(PICKUP_OUTLET_ID),
                    packagingTime, shipmentDateTimeBySupplier, receptionDateTimeByWarehouse,
                    DeliveryOptionPartnerType.MARKET_DELIVERY);
        }

        public ActualDeliveryBuilder addPickup(Long deliveryServiceId) {
            return addPickup(deliveryServiceId, DEFAULT_INTAKE_SHIPMENT_DAYS,
                    Collections.singletonList(PICKUP_OUTLET_ID));
        }

        public ActualDeliveryBuilder addPostTerm(Long deliveryServiceId) {
            return addPickup(
                    deliveryServiceId, DEFAULT_INTAKE_SHIPMENT_DAYS,
                    Collections.singletonList(POST_TERM_OUTLET_ID)
            );
        }

        public ActualDeliveryBuilder addPickupWithPartnerType(Long deliveryServiceId,
                                                              DeliveryOptionPartnerType partnerType) {
            return addPickup(deliveryServiceId, DEFAULT_INTAKE_SHIPMENT_DAYS, null,
                    Collections.singletonList(PICKUP_OUTLET_ID), Duration.ofHours(23),
                    null, null, partnerType);
        }

        public ActualDeliveryBuilder addPickup(PickupOption pickupOption) {
            pickupOptions.add(pickupOption);
            lastOption = pickupOption;
            return this;
        }

        public ActualDeliveryBuilder addPickup(Long deliveryServiceId,
                                               Integer shipmentDay,
                                               List<Long> outletIds) {
            return addPickup(deliveryServiceId, shipmentDay, null, outletIds,
                    Duration.ofHours(23), null, null,
                    DeliveryOptionPartnerType.MARKET_DELIVERY);
        }

        @SuppressWarnings("checkstyle:ParameterNumber")
        public ActualDeliveryBuilder addPickup(Long deliveryServiceId, Integer shipmentDay,
                                               Instant supplierShipmentDateTime,
                                               List<Long> outletIds,
                                               Duration packagingTime,
                                               ZonedDateTime shipmentDateTimeBySupplier,
                                               ZonedDateTime receptionDateTimeByWarehouse,
                                               DeliveryOptionPartnerType partnerType) {
            PickupOption pickupOption = new PickupOption();
            pickupOption.setDeliveryServiceId(deliveryServiceId);
            pickupOption.setDayFrom(0);
            pickupOption.setDayTo(2);
            pickupOption.setPaymentMethods(
                    Set.of(
                            PaymentMethod.YANDEX.name(),
                            PaymentMethod.CARD_ON_DELIVERY.name(),
                            PaymentMethod.CASH_ON_DELIVERY.name()
                    )
            );
            pickupOption.setPrice(PICKUP_PRICE);
            pickupOption.setCurrency(Currency.RUR);
            pickupOption.setOutletIds(outletIds);
            pickupOption.setShipmentDay(shipmentDay);
            pickupOption.setPackagingTime(packagingTime);
            pickupOption.setSupplierShipmentDateTime(supplierShipmentDateTime);
            pickupOption.setShipmentDateTimeBySupplier(shipmentDateTimeBySupplier);
            pickupOption.setReceptionDateTimeByWarehouse(receptionDateTimeByWarehouse);
            pickupOption.setPartnerType(partnerType.getReportName());

            return addPickup(pickupOption);
        }

        public ActualDeliveryBuilder addRedPost(Integer shipmentDay) {
            PickupOption postOption = new PickupOption();
            postOption.setDeliveryServiceId(RED_DELIVERY_SERVICE_ID);
            postOption.setDayFrom(3);
            postOption.setDayTo(6);
            postOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
            postOption.setPrice(POST_PRICE);
            postOption.setCurrency(Currency.RUR);
            postOption.setPartnerType(DeliveryOptionPartnerType.RED_MARKET.getReportName());
            postOption.setShipmentDay(shipmentDay);
            postOption.setOutletIds(Collections.singletonList(DeliveryProvider.POST_OUTLET_ID));
            postOption.setPostCodes(Collections.singletonList(111222L));

            postOptions.add(postOption);
            lastOption = postOption;
            return this;
        }

        public ActualDeliveryBuilder addPost(Integer shipmentDay) {
            return addPost(shipmentDay, RUSPOST_DELIVERY_SERVICE_ID);
        }

        public ActualDeliveryBuilder addPost(Integer shipmentDay, long deliveryServiceId) {
            PickupOption postOption = new PickupOption();
            postOption.setDeliveryServiceId(deliveryServiceId);
            postOption.setDayFrom(0);
            postOption.setDayTo(2);
            postOption.setPaymentMethods(Collections.singleton(PaymentMethod.YANDEX.name()));
            postOption.setPrice(POST_PRICE);
            postOption.setCurrency(Currency.RUR);
            postOption.setPartnerType(DeliveryOptionPartnerType.MARKET_DELIVERY.getReportName());
            postOption.setShipmentDay(shipmentDay);
            postOption.setOutletIds(Collections.singletonList(DeliveryProvider.POST_OUTLET_ID));
            postOption.setPackagingTime(Duration.ofHours(23));
            postOption.setPostCodes(Collections.singletonList(111222L));

            postOptions.add(postOption);
            lastOption = postOption;
            return this;
        }

        public ActualDeliveryBuilder addPost(PickupOption postOption) {
            postOptions.add(postOption);
            lastOption = postOption;
            return this;
        }

        public ActualDeliveryBuilder addOption(DeliveryType deliveryType, LocalDeliveryOption option) {
            switch (deliveryType) {
                case DELIVERY:
                    return addDelivery((ActualDeliveryOption) option);
                case PICKUP:
                    return addPickup((PickupOption) option);
                case POST:
                    return addPost((PickupOption) option);
                default:
                    throw new UnsupportedOperationException("Unsupported delivery type");
            }
        }

        protected void checkLastOption() {
            if (lastOption == null) {
                throw new IllegalStateException("You didn't add delivery option yet");
            }
        }

        /**
         * Сделать последнюю добавленную доставку бесплатной по причине промо, указанного параметром.
         *
         * @param deliveryPromoType причина, по которой последняя добавленная опция будет бесплатной
         * @return
         * @throws IllegalStateException в случае если не было добавлено опший доставки
         */
        public ActualDeliveryBuilder withDiscount(DeliveryPromoType deliveryPromoType) {
            return withDiscount(deliveryPromoType, BigDecimal.ZERO);
        }

        public ActualDeliveryBuilder withDiscount(DeliveryPromoType deliveryPromoType, BigDecimal newCost) {
            checkLastOption();
            if (newCost.compareTo(lastOption.getCost()) >= 0) {
                throw new IllegalArgumentException("New cost is greater than previous (new :" + newCost + ", " +
                        "previous: " + lastOption.getCost() + ")");
            }
            if (deliveryPromoType != null) {
                DeliveryPromo discount = new DeliveryPromo();
                discount.setOldPrice(lastOption.getCost());
                lastOption.setCost(newCost);
                discount.setDiscountType(deliveryPromoType);
                lastOption.setDiscount(discount);
            }
            return this;
        }

        public ActualDeliveryBuilder withPaymentMethods(Set<PaymentMethod> methods) {
            checkLastOption();
            lastOption.setPaymentMethods(methods.stream().map(PaymentMethod::name).collect(Collectors.toSet()));
            return this;
        }

        public ActualDeliveryBuilder withDayFrom(Integer dayFrom) {
            checkLastOption();
            lastOption.setDayFrom(dayFrom);
            return this;
        }

        public ActualDeliveryBuilder withDayTo(Integer dayTo) {
            checkLastOption();
            lastOption.setDayTo(dayTo);
            return this;
        }

        public ActualDeliveryBuilder withTariffId(Long tariffId) {
            checkLastOption();
            lastOption.setTariffId(tariffId);
            return this;
        }

        public ActualDeliveryBuilder addOfferProblem(OfferProblem problem) {
            offerProblems.add(problem);
            return this;
        }

        public ActualDeliveryBuilder addCommonProblem(String problem) {
            commonProblems.add(problem);
            return this;
        }

        public ActualDeliveryBuilder addAvailableDeliveryType(AvailableDeliveryType type) {
            availableDeliveryTypes.add(type);
            return this;
        }

        public ActualDelivery build() {
            ActualDeliveryResult actualDeliveryResult = getActualDeliveryResult();
            actualDeliveryResult.setDelivery(deliveryOptions);
            actualDeliveryResult.setPickup(pickupOptions);
            actualDeliveryResult.setPost(postOptions);
            actualDeliveryResult.setAvailableDeliveryMethods(
                    availableDeliveryTypes.stream()
                            .map(AvailableDeliveryType::getCode)
                            .collect(Collectors.toList())
            );
            ActualDelivery actualDelivery = defaultActualDelivery();
            if (!offers.isEmpty()) {
                actualDeliveryResult.setOffers(offers);
            }
            if (bucketActive != null) {
                actualDeliveryResult.setBucketActive(bucketActive);
            }
            if (bucketAll != null) {
                actualDeliveryResult.setBucketAll(bucketAll);
            }
            if (carrierActive != null) {
                actualDeliveryResult.setCarrierActive(carrierActive);
            }
            if (carrierAll != null) {
                actualDeliveryResult.setCarrierAll(carrierAll);
            }
            if (!commonProblems.isEmpty()) {
                actualDelivery.setCommonProblems(commonProblems);
            }
            if (weight != null) {
                actualDeliveryResult.setWeight(weight);
            }

            actualDeliveryResult.setLargeSize(isLargeSize);

            if (CollectionUtils.isNonEmpty(dimensions)) {
                actualDeliveryResult.setDimensions(dimensions);
            }

            if (!offerProblems.isEmpty()) {
                actualDelivery.setOfferProblems(offerProblems);
                actualDelivery.setResults(emptyList());
                return actualDelivery;
            }

            actualDelivery.setResults(Collections.singletonList(actualDeliveryResult));
            if (isFree) {
                actualDelivery.setFreeDeliveryRemainder(BigDecimal.ZERO);
                deliveryOptions.forEach(option -> option.setCost(BigDecimal.ZERO));
                pickupOptions.forEach(option -> option.setCost(BigDecimal.ZERO));
                postOptions.forEach(option -> option.setCost(BigDecimal.ZERO));
            }

            if (isExternalLogistics != null) {
                deliveryOptions.forEach(option -> option.setIsExternalLogistics(isExternalLogistics));
                pickupOptions.forEach(option -> option.setIsExternalLogistics(isExternalLogistics));
            }
            return actualDelivery;
        }
    }
}
