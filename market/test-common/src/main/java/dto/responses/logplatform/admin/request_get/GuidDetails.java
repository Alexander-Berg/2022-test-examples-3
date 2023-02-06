package dto.responses.logplatform.admin.request_get;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GuidDetails {

    @JsonProperty("features")
    private List<FeaturesItem> features;

    @JsonProperty("total_cost")
    private int totalCost;

    @JsonProperty("resource_code")
    private String resourceCode;

    @JsonProperty("place_barcodes")
    private List<String> placeBarcodes;

    @JsonProperty("resource_id")
    private String resourceId;

    @JsonProperty("total_assessed_cost")
    private int totalAssessedCost;

    @JsonProperty("incorrect_place_ids")
    private List<Object> incorrectPlaceIds;
}
