package dto.responses.logplatform.admin.request_get;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Model {

    @JsonProperty("request")
    private Request request;

    @JsonProperty("resources")
    private Map<String, GuidDetails> resources;

    @JsonProperty("created_at")
    private int createdAt;

    @JsonProperty("places_info")
    private List<PlacesInfoItem> placesInfo;

    @JsonProperty("external_requests")
    private List<ExternalRequestsItem> externalRequests;

    @JsonProperty("revision")
    private int revision;

    @JsonProperty("particular_items_refuse")
    private boolean particularItemsRefuse;

    @JsonProperty("is_compilation")
    private boolean isCompilation;

    @JsonProperty("employer")
    private String employer;

    @JsonProperty("request_code")
    private String requestCode;

    @JsonProperty("items")
    private List<ItemsItem> items;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("status")
    private String status;
}
