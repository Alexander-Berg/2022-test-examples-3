package dto.responses.logplatform.admin.station_list;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EventLogItem {

    @JsonProperty("dt")
    private String dt;

    @JsonProperty("unixtime")
    private String unixtime;

    @JsonProperty("thread_id")
    private String threadId;

    @JsonProperty("t")
    private String t;

    @JsonProperty("host")
    private String host;

    @JsonProperty("link")
    private String link;

    @JsonProperty("evstamp")
    private String evstamp;

    @JsonProperty("source")
    private String source;

    @JsonProperty("event")
    private String event;

    @JsonProperty("_ts")
    private String ts;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("handler")
    private String handler;

    @JsonProperty("user_id")
    private String userId;
}
