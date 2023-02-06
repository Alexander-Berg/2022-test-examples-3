package dto.responses.logplatform.admin.station_list;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StationListResponse {

    @JsonProperty("__host")
    private String host;

    @JsonProperty("stations")
    private List<StationsItem> stations;

    @JsonProperty("has_more")
    private boolean hasMore;

    @JsonProperty("__event_log")
    private List<EventLogItem> eventLog;
}
