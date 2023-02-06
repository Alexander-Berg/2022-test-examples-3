package steps.orderSteps;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import com.google.common.collect.Lists;
import steps.ParcelSteps;
import steps.shopOutletSteps.ShopOutletSteps;
import steps.utils.DateUtils;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.CheckpointStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackStatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOption;
import ru.yandex.market.checkout.checkouter.pay.PaymentOptionHiddenReason;
import ru.yandex.market.checkout.checkouter.pay.legacy.PaymentSubMethod;
import ru.yandex.market.checkout.checkouter.shop.MarketplaceFeature;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;

class DeliverySteps {
    private static final String ID = "123";
    private static final String HASH = "hash";
    private static final String SHOP_DELIVERY_ID = "345";
    private static final String DELIVERY_OPTION_ID = "543";
    private static final DeliveryType TYPE = DeliveryType.DELIVERY;
    private static final String SERVICE_NAME = "serviceName";
    private static final BigDecimal PRICE = BigDecimal.valueOf(123);
    private static final BigDecimal BUYER_PRICE = BigDecimal.valueOf(123);
    private static final DeliveryDates DELIVERY_DATES = new DeliveryDates();
    private static final DeliveryDates VALIDATED_DELIVERY_DATES = new DeliveryDates();
    private static final Long REGION_ID = 213L;
    private static final Long OUTLET_ID = 333L;
    private static final Set<Long> OUTLET_IDS = Set.of(1L);
    private static final List<PaymentOption> HIDDEN_PAYMENT_OPTIONS = new ArrayList<>();
    private static final boolean VALIDATED = true;
    private static final Long DELIVERY_SERVICE_ID = 987L;
    private static final DeliveryPartnerType DELIVERY_PARTNER_TYPE = DeliveryPartnerType.YANDEX_MARKET;
    private static final Recipient RECIPIENT = RecipientSteps.getRecipient();

    private DeliverySteps() {
    }

    static Delivery getDelivery(int numberOfParcels) {
        Delivery delivery = new Delivery();

        delivery.setId(ID);
        delivery.setHash(HASH);
        delivery.setShopDeliveryId(SHOP_DELIVERY_ID);
        delivery.setDeliveryOptionId(DELIVERY_OPTION_ID);
        delivery.setType(TYPE);
        delivery.setServiceName(SERVICE_NAME);
        delivery.setPrice(PRICE);
        delivery.setBuyerPrice(BUYER_PRICE);
        delivery.setDeliveryDates(DELIVERY_DATES);
        delivery.setValidatedDeliveryDates(VALIDATED_DELIVERY_DATES);
        delivery.setRegionId(REGION_ID);
        delivery.setOutletId(OUTLET_ID);
        Parcel parcel = ParcelSteps.getParcel();
        parcel.setTracks(TrackSteps.getTracksList());

        List<Parcel> parcels = new ArrayList<>();
        IntStream.range(0, numberOfParcels).forEach(i -> parcels.add(new Parcel(parcel)));
        delivery.setParcels(parcels);

        delivery.setHiddenPaymentOptions(HIDDEN_PAYMENT_OPTIONS);
        delivery.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        delivery.setValidated(VALIDATED);
        delivery.setHiddenPaymentOptions(HiddenDeliveryOptionsSteps.getHiddenDeliveryOptions());
        delivery.setValidFeatures(ValidFeaturesSteps.getValidFeature());
        delivery.setOutletId(OUTLET_ID);
        delivery.setDeliveryPartnerType(DELIVERY_PARTNER_TYPE);
        delivery.setOutlets(ShopOutletSteps.getShopOutletsList());
        delivery.setShopAddress(AddressSteps.getAddress());
        delivery.setBuyerAddress(AddressSteps.getAddress());
        delivery.setOutletIds(OUTLET_IDS);
        delivery.setOutlet(ShopOutletSteps.getShopOutlet());
        delivery.setValidationErrors(ValidationErrorStep.getValidationErrors());
        delivery.setRecipient(RECIPIENT);

        return delivery;
    }

    private static class HiddenDeliveryOptionsSteps {
        private static final PaymentMethod PAYMENT_METHOD = PaymentMethod.BANK_CARD;
        private static final List<PaymentSubMethod> PAYMENT_SUB_METHODS = List.of(PaymentSubMethod.YA_MONEY);
        private static final PaymentOptionHiddenReason HIDDEN_REASON = PaymentOptionHiddenReason.POST;

        static List<PaymentOption> getHiddenDeliveryOptions() {
            PaymentOption paymentOption = new PaymentOption();

            paymentOption.setPaymentMethod(PAYMENT_METHOD);
            paymentOption.setPaymentSubMethods(PAYMENT_SUB_METHODS);
            paymentOption.setHiddenReason(HIDDEN_REASON);

            return List.of(paymentOption);
        }
    }

    private static class ValidFeaturesSteps {
        private static final MarketplaceFeature MARKETPLACE_FEATURE = MarketplaceFeature.PLAINCPA;

        static Set<MarketplaceFeature> getValidFeature() {
            return Set.of(MARKETPLACE_FEATURE);
        }
    }

    public static class TrackSteps {
        private static final Long ID = 123L;
        private static final Long ORDER_ID = 123L;
        private static final Long DELIVERY_ID = 123L;
        private static final String TRACK_CODE = "track code";
        private static final Long DELIVERY_SERVICE_ID = 123L;
        private static final Long TRACKER_ID = 222L;
        private static final TrackStatus STATUS = TrackStatus.STARTED;

        static List<Track> getTracksList() {
            Track track = new Track();

            track.setId(ID);
            track.setOrderId(ORDER_ID);
            track.setDeliveryId(DELIVERY_ID);
            track.setTrackCode(TRACK_CODE);
            track.setDeliveryServiceId(DELIVERY_SERVICE_ID);
            track.setTrackerId(TRACKER_ID);
            track.setStatus(STATUS);
            track.setCheckpoints(CheckpointSteps.getCheckpointList());

            return Lists.newArrayList(track);
        }

        private static class CheckpointSteps {
            private static final long TRACKER_CHECKPOINT_ID = 123L;
            private static final String COUNTRY = "Россия";
            private static final String CITY = "Москва";
            private static final String LOCATION = "location";
            private static final String MESSAGE = "message";
            private static final CheckpointStatus CHECKPOINT_STATUS = CheckpointStatus.IN_TRANSIT;
            private static final String ZIP_CODE = "630060";
            private static final Integer DELIVERY_CHECKPOINT_STATUS = 3;
            private static final String TRANSLATE_MESSAGE = "translated_message";
            private static final Long TRACK_ID = 123L;
            private static final Long ID = 123L;

            static List<TrackCheckpoint> getCheckpointList() {
                TrackCheckpoint checkpoint = new TrackCheckpoint(
                    TRACKER_CHECKPOINT_ID,
                    COUNTRY,
                    CITY,
                    LOCATION,
                    MESSAGE,
                    CHECKPOINT_STATUS,
                    ZIP_CODE,
                    DateUtils.getDate(),
                    DELIVERY_CHECKPOINT_STATUS
                );

                checkpoint.setTranslatedMessage(TRANSLATE_MESSAGE);
                checkpoint.setTrackId(TRACK_ID);
                checkpoint.setTranslatedCity(CITY);
                checkpoint.setTranslatedLocation(LOCATION);
                checkpoint.setId(ID);
                checkpoint.setTranslatedCountry(COUNTRY);

                return List.of(checkpoint);
            }
        }
    }

    private static class ValidationErrorStep {
        static List<ValidationResult> getValidationErrors() {
            return List.of(new ValidationResult("code", ValidationResult.Severity.WARNING));
        }
    }
}
