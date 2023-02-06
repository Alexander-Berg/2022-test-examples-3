package ru.yandex.market.pers.basket.controller.v2;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.model.Price;
import ru.yandex.market.pers.basket.model.SecondaryReference;
import ru.yandex.market.pers.list.BasketClientParams;
import ru.yandex.market.pers.list.model.v2.enums.ReferenceType;

/**
 * @author ifilippov5
 */
public class BasketV2PostTestRequest extends BasketV2TestRequest {

    private BasketRefItem item = new BasketRefItem();

    public BasketRefItem getItem() {
        return item;
    }

    public void setItem(BasketRefItem item) {
        this.item = item;
    }

    public BasketV2PostTestRequest clone() {
        BasketV2PostTestRequest request = new BasketV2PostTestRequest();
        request.setReqIdHeader(this.getReqIdHeader());
        request.setPlatformHeader(this.getPlatformHeader());
        request.setUserIdType(this.getUserIdType());
        request.setUserAnyId(this.getUserAnyId());
        request.setRgb(this.getRgb());
        request.setItem(this.getItem().clone());
        return request;
    }

    public static class BasketRefItem {

        @JsonProperty("id")
        private Long basketItemId;

        @JsonProperty(BasketClientParams.REFERENCE_TYPE)
        private ReferenceType referenceType = ReferenceType.SKU;

        @JsonProperty(BasketClientParams.REFERENCE_ID)
        private String referenceId = "refId";

        @JsonProperty(BasketClientParams.TITLE)
        private String title = "title";

        @JsonProperty(BasketClientParams.IMAGE_BASE_URL)
        private String imageBaseUrl = "https://avatars.mds.yandex.net/get-mpic/466729/img_id4470756757588870758/orig";

        @JsonProperty(BasketClientParams.PRICE)
        private Price price;

        @JsonProperty("added_at")
        private String addedAt;

        @JsonProperty(BasketClientParams.SECONDARY_REFERENCES)
        private List<SecondaryReference> secondaryReferences = Collections.emptyList();

        public Long getBasketItemId() {
            return basketItemId;
        }

        public void setBasketItemId(Long basketItemId) {
            this.basketItemId = basketItemId;
        }

        public ReferenceType getReferenceType() {
            return referenceType;
        }

        public void setReferenceType(ReferenceType referenceType) {
            this.referenceType = referenceType;
        }

        public String getReferenceId() {
            return referenceId;
        }

        public void setReferenceId(String referenceId) {
            this.referenceId = referenceId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getImageBaseUrl() {
            return imageBaseUrl;
        }

        public void setImageBaseUrl(String imageBaseUrl) {
            this.imageBaseUrl = imageBaseUrl;
        }

        public Price getPrice() {
            return price;
        }

        public void setPrice(Price price) {
            this.price = price;
        }

        public String getAddedAt() {
            return addedAt;
        }

        public void setAddedAt(String addedAt) {
            this.addedAt = addedAt;
        }

        public List<SecondaryReference> getSecondaryReferences() {
            return secondaryReferences;
        }

        public void setSecondaryReferences(List<SecondaryReference> secondaryReferences) {
            this.secondaryReferences = secondaryReferences;
        }

        public BasketRefItem clone() {
            BasketRefItem item = new BasketRefItem();
            item.setReferenceType(this.getReferenceType());
            item.setReferenceId(this.getReferenceId());
            item.setTitle(this.getTitle());
            item.setImageBaseUrl(this.getImageBaseUrl());
            item.setPrice(this.getPrice());
            item.setAddedAt(this.getAddedAt());
            item.setSecondaryReferences(this.getSecondaryReferences());
            return item;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BasketRefItem)) {
                return false;
            }
            BasketRefItem item = (BasketRefItem) obj;
            return referenceType == item.getReferenceType()
                && Objects.equals(referenceId, item.getReferenceId()) && Objects.equals(title, item.getTitle())
                && Objects.equals(imageBaseUrl, item.getImageBaseUrl()) && Objects.equals(price, item.getPrice())
                && Objects.equals(addedAt, item.getAddedAt()) && Objects.equals(secondaryReferences, item.getSecondaryReferences());
        }

        @Override
        public String toString() {
            String sb = "[" +
                referenceType +
                "," +
                referenceId +
                "," +
                title +
                "," +
                imageBaseUrl +
                "," +
                price +
                "," +
                addedAt +
                "," +
                secondaryReferences +
                "]";
            return sb;
        }

        public static BasketRefItem from(BasketReferenceItem item) {
            BasketRefItem result = new BasketRefItem();
            result.setBasketItemId(item.getBasketItemId());
            result.setReferenceType(item.getReferenceType());
            result.setReferenceId(item.getReferenceId());
            result.setAddedAt(item.getAddedAt());
            result.setImageBaseUrl(item.getImageBaseUrl());
            result.setPrice(item.getPrice());
            result.setSecondaryReferences(item.getSecondaryReferences());
            result.setTitle(item.getTitle());
            return result;
        }

    }

}
