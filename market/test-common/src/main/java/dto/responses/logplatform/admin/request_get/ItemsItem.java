package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ItemsItem {

    @JsonProperty("billing_details")
    private BillingDetails billingDetails;

    @JsonProperty("refused_count")
    private int refusedCount;

    @JsonProperty("internal_item_id")
    private String internalItemId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("count")
    private int count;

    @JsonProperty("non_physical_item")
    private boolean nonPhysicalItem;

    @JsonProperty("resource_id")
    private String resourceId;

    @JsonProperty("internal_place_id")
    private String internalPlaceId;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("article")
    private String article;
}
