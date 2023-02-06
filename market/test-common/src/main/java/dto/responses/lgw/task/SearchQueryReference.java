package dto.responses.lgw.task;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchQueryReference {

    @JsonProperty("openNewTab")
    private Boolean openNewTab;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("url")
    private String url;
}
