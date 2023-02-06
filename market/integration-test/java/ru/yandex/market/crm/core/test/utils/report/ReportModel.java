package ru.yandex.market.crm.core.test.utils.report;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author apershukov
 */
public class ReportModel extends ReportEntity {

    public static class ModelPrices {

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("min")
        private String min;

        @JsonProperty("max")
        private String max;

        @JsonProperty("avg")
        private String avg;

        public String getCurrency() {
            return currency;
        }

        public ModelPrices setCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public String getMin() {
            return min;
        }

        public ModelPrices setMin(String min) {
            this.min = min;
            return this;
        }

        public String getMax() {
            return max;
        }

        public ModelPrices setMax(String max) {
            this.max = max;
            return this;
        }

        public String getAvg() {
            return avg;
        }

        public ModelPrices setAvg(String avg) {
            this.avg = avg;
            return this;
        }
    }

    @JsonProperty("offers")
    private ModelOffers offers;

    @JsonProperty("prices")
    private ModelPrices prices;

    public ModelOffers getOffers() {
        return offers;
    }

    public void setOffers(ModelOffers offers) {
        this.offers = offers;
    }

    public ReportModel setOffers(Offer... offers) {
        setOffers(new ModelOffers(offers.length, List.of(offers)));
        return this;
    }

    public ModelPrices getPrices() {
        return prices;
    }

    public ReportModel setPrices(ModelPrices prices) {
        this.prices = prices;
        return this;
    }
}
