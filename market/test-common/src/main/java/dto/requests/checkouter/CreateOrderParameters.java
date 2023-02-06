package dto.requests.checkouter;

import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dto.requests.checkouter.checkout.Buyer;
import dto.requests.report.OfferItem;
import lombok.Builder;
import lombok.Getter;
import toolkit.Pair;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;

@Builder
@Getter
public class CreateOrderParameters {
    private final Long regionId;
    private final List<OfferItem> items;

    private final Address address;
    private final PaymentMethod paymentMethod;
    private final PaymentType paymentType;
    private final EnumSet<RearrFactor> experiment;
    private final Long forceDeliveryId;
    private final OrderComment comment;
    private Predicate<Delivery> deliveryPredicate;
    private final DeliveryType deliveryType;
    private final Buyer buyer;
    private Pair<LocalTime, LocalTime> deliveryInterval;
    private final List<Long> outletId;
    private final Color rgb;
    private final Long businessBuyerBalanceId;
    private final Long type;
    private final Long availableForBusiness;

    @SuppressWarnings("checkstyle:ParameterNumber")
    private CreateOrderParameters(
        @Nonnull Long regionId,
        @Nonnull List<OfferItem> items,
        @Nullable Address address,
        @Nullable PaymentMethod paymentMethod,
        @Nullable PaymentType paymentType,
        @Nullable EnumSet<RearrFactor> experiment,
        @Nullable Long forceDeliveryId,
        @Nullable OrderComment comment,
        @Nullable Predicate<Delivery> deliveryPredicate,
        @Nonnull DeliveryType deliveryType,
        @Nullable Buyer buyer,
        @Nullable Pair<LocalTime, LocalTime> deliveryInterval,
        @Nullable List<Long> outletId,
        @Nullable Color rgb,
        @Nullable Long businessBuyerBalanceId,
        @Nullable Long type,
        @Nullable Long availableForBusiness
    ) {
        this.regionId = regionId;
        this.items = items;
        this.deliveryType = deliveryType;
        this.address = address != null ? address : Address.ADDRESS;
        this.paymentMethod = paymentMethod != null ? paymentMethod : PaymentMethod.CASH_ON_DELIVERY;
        this.paymentType = paymentType != null ? paymentType : PaymentType.POSTPAID;
        this.experiment = experiment != null ? experiment : EnumSet.noneOf(RearrFactor.class);
        this.forceDeliveryId = forceDeliveryId;
        this.comment = comment;
        this.deliveryPredicate = deliveryPredicate != null ? deliveryPredicate : delivery -> true;
        this.buyer = buyer != null ? buyer : new Buyer("+77777777777", "2af28f3ab6cc3b47864976b2dac27b46");
        this.buyer.setBusinessBalanceId(businessBuyerBalanceId);
        this.businessBuyerBalanceId = businessBuyerBalanceId;
        this.buyer.setType(type);
        this.type = type;
        this.deliveryInterval = deliveryInterval;
        this.outletId = outletId;
        this.rgb = rgb != null ? rgb : Color.BLUE;
        this.availableForBusiness = availableForBusiness != null ? availableForBusiness : 0L;
        if (outletId != null) {
            this.deliveryPredicate = this.deliveryPredicate.and(delivery ->
                delivery.getOutlets() != null &&
                    delivery.getOutlets().stream().map(ShopOutlet::getId).anyMatch(outletId::contains));
        }
    }


    private static CreateOrderParameters.CreateOrderParametersBuilder builder() {
        return new CreateOrderParametersBuilder();
    }

    public static CreateOrderParameters.CreateOrderParametersBuilder newBuilder(
        Long regionId,
        List<OfferItem> items,
        DeliveryType deliveryType
    ) {
        return CreateOrderParameters.builder()
            .regionId(regionId)
            .deliveryType(deliveryType)
            .items(items);
    }

    public static CreateOrderParameters.CreateOrderParametersBuilder newBuilder(
        Long regionId,
        OfferItem item,
        DeliveryType deliveryType
    ) {
        return CreateOrderParameters.builder()
            .regionId(regionId)
            .deliveryType(deliveryType)
            .items(Collections.singletonList(item));
    }


    public String getComment() {
        return comment != null ? comment.getValue() : "";
    }

    public boolean getFake() {
        return paymentType.equals(PaymentType.PREPAID);
    }

    public void setDeliveryInterval(Pair<LocalTime, LocalTime> deliveryInterval) {
        this.deliveryInterval = deliveryInterval;
    }

    public void setDeliveryPredicate(Predicate<Delivery> deliveryPredicate) {
        this.deliveryPredicate = deliveryPredicate;
    }
}

