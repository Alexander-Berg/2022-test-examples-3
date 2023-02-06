package dto.responses.tm.admin.movement;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LgwFilter {

    @JsonProperty("openNewTab")
    private Boolean openNewTab;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("url")
    private String url;
}
