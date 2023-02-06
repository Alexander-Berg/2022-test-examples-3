package dto.responses.logplatform.admin.request_get;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ResourcePhysicalFeature {

    @JsonProperty("resource_code")
    private String resourceCode;

    @JsonProperty("start_reservation_code")
    private String startReservationCode;

    @JsonProperty("physical_dims")
    private PhysicalDims physicalDims;

    @JsonProperty("destination_node_code")
    private String destinationNodeCode;

    @JsonProperty("description")
    private String description;

    @JsonProperty("weight")
    private int weight;

    @JsonProperty("place_code")
    private String placeCode;

    @JsonProperty("d_x")
    private int dX;

    @JsonProperty("barcode")
    private String barcode;

    @JsonProperty("items")
    private List<ItemsItem> items;

    @JsonProperty("d_z")
    private int dZ;

    @JsonProperty("d_y")
    private int dY;
}
