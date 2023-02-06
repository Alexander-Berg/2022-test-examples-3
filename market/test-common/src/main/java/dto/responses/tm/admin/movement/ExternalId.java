package dto.responses.tm.admin.movement;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ExternalId {

    @JsonProperty("openNewTab")
    private Boolean openNewTab;

    @JsonProperty("displayName")
    private Object displayName;

    @JsonProperty("url")
    private String url;
}
