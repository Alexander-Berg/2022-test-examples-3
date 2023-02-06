package ru.yandex.market.crm.core.test.utils.report;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author apershukov
 */
public class Offer extends ReportEntity {

    public static class OfferPrices {

        public static class Discount {

            @JsonProperty("oldMin")
            private String oldMin;

            @JsonProperty("percent")
            private int percent;

            public Discount(String oldMin, int percent) {
                this.oldMin = oldMin;
                this.percent = percent;
            }

            public Discount() {
            }

            public String getOldMin() {
                return oldMin;
            }

            public Discount setOldMin(String oldMin) {
                this.oldMin = oldMin;
                return this;
            }

            public int getPercent() {
                return percent;
            }

            public Discount setPercent(int percent) {
                this.percent = percent;
                return this;
            }
        }

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("value")
        private String value;

        @JsonProperty("discount")
        private Discount discount;

        public String getCurrency() {
            return currency;
        }

        public OfferPrices setCurrency(String currency) {
            this.currency = currency;
            return this;
        }

        public String getValue() {
            return value;
        }

        public OfferPrices setValue(String value) {
            this.value = value;
            return this;
        }

        public Discount getDiscount() {
            return discount;
        }

        public OfferPrices setDiscount(Discount discount) {
            this.discount = discount;
            return this;
        }
    }

    public static class Delivery {

        public static class Region {

            @JsonProperty("id")
            private long id;

            public Region(long id) {
                this.id = id;
            }

            public Region() {
            }

            public long getId() {
                return id;
            }

            public void setId(long id) {
                this.id = id;
            }
        }

        @JsonProperty("shopPriorityRegion")
        private Region shopPriorityRegion;

        @JsonProperty("inStock")
        private boolean inStock;

        public Region getShopPriorityRegion() {
            return shopPriorityRegion;
        }

        public Delivery setShopPriorityRegion(Region shopPriorityRegion) {
            this.shopPriorityRegion = shopPriorityRegion;
            return this;
        }

        public boolean isInStock() {
            return inStock;
        }

        public Delivery setInStock(boolean inStock) {
            this.inStock = inStock;
            return this;
        }
    }

    public static class Promo {

        @JsonProperty("type")
        private String type;

        @JsonProperty("key")
        private String key;

        @JsonProperty("description")
        private String description;

        public String getType() {
            return type;
        }

        public Promo setType(String type) {
            this.type = type;
            return this;
        }

        public String getKey() {
            return key;
        }

        public Promo setKey(String key) {
            this.key = key;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Promo setDescription(String description) {
            this.description = description;
            return this;
        }
    }

    public static class Model {

        @JsonProperty("id")
        private Long id;

        public Model(Long id) {
            this.id = id;
        }

        public Model() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }

    @JsonProperty("prices")
    private OfferPrices prices;

    @JsonProperty("delivery")
    private Delivery delivery;

    @JsonProperty("promo")
    private Promo promo;

    @JsonProperty("model")
    private Model model;

    public OfferPrices getPrices() {
        return prices;
    }

    public Offer setPrices(OfferPrices prices) {
        this.prices = prices;
        return this;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public Offer setDelivery(Delivery delivery) {
        this.delivery = delivery;
        return this;
    }

    public Promo getPromo() {
        return promo;
    }

    public Offer setPromo(Promo promo) {
        this.promo = promo;
        return this;
    }

    public Model getModel() {
        return model;
    }

    public Offer setModel(Model model) {
        this.model = model;
        return this;
    }
}
