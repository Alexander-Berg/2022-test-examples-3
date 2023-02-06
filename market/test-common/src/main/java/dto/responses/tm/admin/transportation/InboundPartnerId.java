package dto.responses.tm.admin.transportation;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InboundPartnerId {

    @JsonProperty("openNewTab")
    private boolean openNewTab;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("id")
    private String id;

    @JsonProperty("slug")
    private String slug;

}
