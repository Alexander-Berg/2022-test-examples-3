package dto.responses.tm.admin.transportation;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StartrekTicket {

    @JsonProperty("openNewTab")
    private boolean openNewTab;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("url")
    private String url;

}
