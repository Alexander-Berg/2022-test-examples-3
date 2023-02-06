package dto.responses.logplatform.admin.request_get;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PlaceInfo {

    @JsonProperty("destination_node_id")
    private String destinationNodeId;

    @JsonProperty("reservation_id")
    private String reservationId;

    @JsonProperty("place_barcode")
    private String placeBarcode;

    @JsonProperty("description")
    private String description;

    @JsonProperty("resource_id")
    private String resourceId;

    @JsonProperty("transfer_id")
    private String transferId;

    @JsonProperty("internal_place_id")
    private String internalPlaceId;

    @JsonProperty("physical")
    private Physical physical;

    @JsonProperty("current_info")
    private CurrentInfo currentInfo;

    @JsonProperty("items")
    private List<Object> items;
}
