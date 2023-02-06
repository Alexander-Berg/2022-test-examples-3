package dto.responses.tm.admin.movement;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Transport {

    @JsonProperty("openNewTab")
    private Boolean openNewTab;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("id")
    private String id;

    @JsonProperty("slug")
    private String slug;
}
