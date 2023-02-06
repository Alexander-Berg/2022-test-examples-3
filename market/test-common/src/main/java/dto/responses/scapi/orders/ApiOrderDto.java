package dto.responses.scapi.orders;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ApiOrderDto {

    @JsonProperty("canBindZone")
    private boolean canBindZone;

    @JsonProperty("fromCourier")
    private boolean fromCourier;

    @JsonProperty("palletizationRequired")
    private boolean palletizationRequired;

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("multiPlace")
    private boolean multiPlace;

    @JsonProperty("availableCells")
    private List<AvailableCellsItem> availableCells;

    @JsonProperty("availableLots")
    private List<Object> availableLots;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("warehouse")
    private Warehouse warehouse;

    @JsonProperty("status")
    private String status;
}
