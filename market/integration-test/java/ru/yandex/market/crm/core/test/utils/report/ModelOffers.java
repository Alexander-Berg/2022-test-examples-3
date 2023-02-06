package ru.yandex.market.crm.core.test.utils.report;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author apershukov
 */
class ModelOffers {

    @JsonProperty("count")
    private int count;

    @JsonProperty("items")
    private List<?> offers = List.of();

    public ModelOffers() {
    }

    ModelOffers(int count, List<?> offers) {
        this.count = count;
        this.offers = offers;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<?> getOffers() {
        return offers;
    }

    public void setOffers(List<?> offers) {
        this.offers = offers;
    }
}
