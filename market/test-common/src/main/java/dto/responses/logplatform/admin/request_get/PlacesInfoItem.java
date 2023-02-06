package dto.responses.logplatform.admin.request_get;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PlacesInfoItem {

    @JsonProperty("is_actual_place")
    private boolean isActualPlace;

    @JsonProperty("rejected_chains")
    private List<Object> rejectedChains;

    @JsonProperty("place_info")
    private PlaceInfo placeInfo;

    @JsonProperty("status_history")
    private List<String> statusHistory;

    @JsonProperty("place_code")
    private String placeCode;

    @JsonProperty("internal_place_id")
    private String internalPlaceId;

    @JsonProperty("trace_simple")
    private List<TraceSimpleItem> traceSimple;

    @JsonProperty("events_chain")
    private List<EventsChainItem> eventsChain;

    @JsonProperty("barcode")
    private String barcode;
}
