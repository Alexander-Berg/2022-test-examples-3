package ru.yandex.market.checkout.util.loyalty.response;

import java.util.Map;
import java.util.Objects;

import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.loyalty.api.model.ItemPromoResponse;
import ru.yandex.market.loyalty.api.model.PromoType;
import ru.yandex.market.loyalty.api.model.bundle.OrderBundle;

public class OrderBundleResponse {

    private final OrderBundle bundle;
    private final Map<FeedOfferId, ItemPromoResponse> promos;
    private final PromoType promoType;
    private final String anaplanId;

    OrderBundleResponse(OrderBundle bundle, Map<FeedOfferId, ItemPromoResponse> promos, PromoType promoType,
                        String anaplanId) {
        this.bundle = bundle;
        this.promos = promos;
        this.promoType = promoType;
        this.anaplanId = anaplanId;
    }

    public OrderBundle getBundle() {
        return bundle;
    }

    public ItemPromoResponse promoFor(FeedOfferId feedOfferId) {
        return promos.get(feedOfferId);
    }

    public PromoType getPromoType() {
        return promoType;
    }

    public String getAnaplanId() {
        return anaplanId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderBundleResponse that = (OrderBundleResponse) o;
        return Objects.equals(bundle, that.bundle);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bundle);
    }

    @Override
    public String toString() {
        return "OrderBundleResponse{" +
                "bundle=" + bundle +
                ", promos=" + promos +
                ", promoType=" + promoType +
                ", anaplanId='" + anaplanId + '\'' +
                '}';
    }
}
