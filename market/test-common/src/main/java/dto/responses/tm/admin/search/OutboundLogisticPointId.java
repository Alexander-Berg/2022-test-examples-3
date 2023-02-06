package dto.responses.tm.admin.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OutboundLogisticPointId {

    @JsonProperty("openNewTab")
    private Boolean openNewTab;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("id")
    private String id;

    @JsonProperty("slug")
    private String slug;
}
