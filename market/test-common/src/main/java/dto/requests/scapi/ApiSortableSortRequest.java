package dto.requests.scapi;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiSortableSortRequest {

    @JsonProperty("destinationExternalId")
    private String destinationExternalId;

    @Nullable
    @JsonProperty("parentDestinationExternalId")
    private String parentDestinationExternalId;

    @Nullable
    @JsonProperty("ignoreTodayRouteOnKeep")
    private Boolean ignoreTodayRouteOnKeep;

    @JsonProperty("sortableExternalId")
    private String sortableExternalId;

    @Nullable
    @JsonProperty("placeExternalId")
    private String placeExternalId;
}
