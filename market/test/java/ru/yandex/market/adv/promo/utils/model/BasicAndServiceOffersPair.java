package ru.yandex.market.adv.promo.utils.model;

import java.util.Objects;

import javax.annotation.Nullable;

import Market.DataCamp.DataCampOffer;

public class BasicAndServiceOffersPair {
    private final DataCampOffer.Offer basicOffer;
    private final DataCampOffer.Offer serviceOffer;

    public BasicAndServiceOffersPair(DataCampOffer.Offer basicOffer, DataCampOffer.Offer serviceOffer) {
        this.basicOffer = Objects.requireNonNull(basicOffer, "basicOffer");
        this.serviceOffer = serviceOffer;
    }

    public DataCampOffer.Offer getBasicOffer() {
        return basicOffer;
    }

    @Nullable
    public DataCampOffer.Offer getServiceOffer() {
        return serviceOffer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BasicAndServiceOffersPair that = (BasicAndServiceOffersPair) o;
        return Objects.equals(basicOffer, that.basicOffer) && Objects.equals(serviceOffer, that.serviceOffer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(basicOffer, serviceOffer);
    }
}
