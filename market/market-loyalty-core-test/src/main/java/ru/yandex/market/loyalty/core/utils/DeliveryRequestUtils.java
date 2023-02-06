package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;

import ru.yandex.market.loyalty.api.model.delivery.DeliveryFeature;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryRequest;
import ru.yandex.market.loyalty.api.model.delivery.DeliveryType;
import ru.yandex.market.loyalty.api.model.delivery.GeoPoint;

import static java.math.BigDecimal.ZERO;

public class DeliveryRequestUtils {
    public static final BigDecimal DEFAULT_DELIVERY_PRICE = BigDecimal.valueOf(249);
    public static final long DEFAULT_DELIVERY_REGION = 213L;
    public static final BigDecimal DEFAULT_DELIVERY_PRICE_HALF = BigDecimal.valueOf(249 / 2);

    public static final String COURIER_DELIVERY_ID = "12321321";
    public static final String COURIER_SPECIAL_ADDRESS_DELIVERY_ID = "32141252";
    public static final String PICKUP_DELIVERY_ID = "126498";
    public static final String POST_DELIVERY_ID = "4644896";

    public static final GeoPoint FREE_DELIVERY_ADDRESS_POINT = new GeoPoint(37.642474, 55.735520);

    public static final Function<Builder, Builder> SELECTED = b -> b.setSelected(true);
    public static final Function<Builder, Builder> ZERO_PRICE = withPrice(ZERO);

    public static Function<Builder, Builder> withRegion(Long region) {
        return b -> b.setRegion(region);
    }

    public static Function<Builder, Builder> withPrice(BigDecimal price) {
        return b -> b.setPrice(price);
    }
    public static Function<Builder, Builder> withMarketBrandedPickup(boolean isMarketBrandedPickup) {
        return b -> b.setMarketBrandedPickup(isMarketBrandedPickup);
    }

    public static Function<Builder, Builder> withId(String id) {
        return b -> b.setId(id);
    }

    public static Function<Builder, Builder> withGeoPoint(Double longitude, Double latitude) {
        return b -> b.setGeoPoint(new GeoPoint(longitude, latitude));
    }

    public static Function<Builder, Builder> withFreeDeliveryAddress() {
        return withGeoPoint(37.642474d, 55.735520d);
    }

    public static Function<Builder, Builder> withFeatures(Set<DeliveryFeature> features) {
        return b -> b.setFeatures(features);
    }

    public static DeliveryRequest courierDelivery() {
        return courierDelivery(Function.identity());
    }

    public static DeliveryRequest nullRegion() {
        Builder builder = builder()
                .setId(COURIER_DELIVERY_ID)
                .setPrice(DEFAULT_DELIVERY_PRICE)
                .setRegion(null)
                .setGeoPoint(new GeoPoint(123123.123, 123213.123))
                .setSelected(false)
                .setType(DeliveryType.COURIER);
        return builder.build();
    }

    public static DeliveryRequest courierDeliveryToSpecialAddress() {
        return courierDeliveryToSpecialAddress(DEFAULT_DELIVERY_PRICE);
    }

    public static DeliveryRequest courierDeliveryToSpecialAddressAlreadyFree() {
        return courierDeliveryToSpecialAddress(ZERO);
    }

    public static DeliveryRequest courierDeliveryToSpecialAddress(BigDecimal price) {
        Builder builder = builder()
                .setId(COURIER_SPECIAL_ADDRESS_DELIVERY_ID)
                .setPrice(price)
                .setRegion(213L)
                .setGeoPoint(FREE_DELIVERY_ADDRESS_POINT)
                .setSelected(false)
                .setType(DeliveryType.COURIER);
        return builder.build();
    }

    public static DeliveryRequest postDeliveryToSpecialAddress() {
        Builder builder = builder()
                .setId(POST_DELIVERY_ID)
                .setPrice(DEFAULT_DELIVERY_PRICE)
                .setRegion(213L)
                .setGeoPoint(FREE_DELIVERY_ADDRESS_POINT)
                .setSelected(false)
                .setType(DeliveryType.POST);
        return builder.build();
    }

    @SafeVarargs
    public static DeliveryRequest courierDelivery(Function<Builder, Builder>... customizations) {
        Builder builder = builder()
                .setId(COURIER_DELIVERY_ID)
                .setPrice(DEFAULT_DELIVERY_PRICE)
                .setRegion(DEFAULT_DELIVERY_REGION)
                .setGeoPoint(new GeoPoint(123123.123, 123213.123))
                .setSelected(false)
                .setType(DeliveryType.COURIER);
        Arrays.asList(customizations).forEach(c -> c.apply(builder));
        return builder.build();
    }

    @SafeVarargs
    public static DeliveryRequest postDelivery(Function<Builder, Builder>... customizations) {
        Builder builder = builder()
                .setId(POST_DELIVERY_ID)
                .setPrice(DEFAULT_DELIVERY_PRICE)
                .setRegion(213L)
                .setGeoPoint(new GeoPoint(123123.123, 123213.123))
                .setSelected(false)
                .setType(DeliveryType.POST);
        Arrays.asList(customizations).forEach(c -> c.apply(builder));
        return builder.build();
    }

    @SafeVarargs
    public static DeliveryRequest pickupDelivery(Function<Builder, Builder>... customizations) {
        Builder builder = builder()
                .setId(PICKUP_DELIVERY_ID)
                .setPrice(DEFAULT_DELIVERY_PRICE)
                .setRegion(213L)
                .setGeoPoint(new GeoPoint(123123.123, 123213.123))
                .setSelected(false)
                .setType(DeliveryType.PICKUP);
        Arrays.asList(customizations).forEach(c -> c.apply(builder));
        return builder.build();
    }

    @SafeVarargs
    public static DeliveryRequest freePickupDelivery(Function<Builder, Builder>... customizations) {
        Builder builder = builder()
                .setId(PICKUP_DELIVERY_ID)
                .setPrice(ZERO)
                .setRegion(213L)
                .setSelected(false)
                .setType(DeliveryType.PICKUP);
        Arrays.asList(customizations).forEach(c -> c.apply(builder));
        return builder.build();
    }

    @SafeVarargs
    public static DeliveryRequest marketBrandedPickupDelivery(Function<Builder, Builder>... customizations) {
        Builder builder = builder()
                .setId(PICKUP_DELIVERY_ID)
                .setPrice(ZERO)
                .setRegion(213L)
                .setGeoPoint(new GeoPoint(123123.123, 123213.123))
                .setSelected(true)
                .setType(DeliveryType.PICKUP)
                .setMarketBrandedPickup(true);
        Arrays.asList(customizations).forEach(c -> c.apply(builder));
        return builder.build();
    }

    @SafeVarargs
    public static DeliveryRequest marketBrandedPickupDeliveryNotSelected(Function<Builder, Builder>... customizations) {
        Builder builder = builder()
                .setId(PICKUP_DELIVERY_ID)
                .setPrice(ZERO)
                .setRegion(213L)
                .setGeoPoint(new GeoPoint(123123.123, 123213.123))
                .setType(DeliveryType.PICKUP)
                .setMarketBrandedPickup(true);
        Arrays.asList(customizations).forEach(c -> c.apply(builder));
        return builder.build();
    }

    private static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private DeliveryType type;
        private BigDecimal price;
        private boolean selected;
        private GeoPoint geoPoint;
        private Long region;
        private Set<DeliveryFeature> features;
        private Boolean isMarketBrandedPickup;
        private String personalGpsId;

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setType(DeliveryType type) {
            this.type = type;
            return this;
        }

        public Builder setPrice(BigDecimal price) {
            this.price = price;
            return this;
        }

        public Builder setSelected(boolean selected) {
            this.selected = selected;
            return this;
        }

        public Builder setGeoPoint(GeoPoint geoPoint) {
            this.geoPoint = geoPoint;
            return this;
        }

        public Builder setRegion(Long region) {
            this.region = region;
            return this;
        }

        public Builder setFeatures(Set<DeliveryFeature> features) {
            this.features = features;
            return this;
        }

        public Builder setMarketBrandedPickup(Boolean isMarketBrandedPickup) {
            this.isMarketBrandedPickup = isMarketBrandedPickup;
            return this;
        }

        public Builder setPersonalGpsId(String personalGpsId) {
            this.personalGpsId = personalGpsId;
            return this;
        }

        public DeliveryRequest build() {
            return DeliveryRequest.Builder.create()
                    .setId(id)
                    .setType(type)
                    .setPrice(price)
                    .setSelected(selected)
                    .setGeoPoint(geoPoint)
                    .setRegion(region)
                    .setFeatures(features)
                    .setIsMarketBrandedPickup(isMarketBrandedPickup)
                    .setPersonalGpsId(personalGpsId)
                    .build();
        }
    }
}
