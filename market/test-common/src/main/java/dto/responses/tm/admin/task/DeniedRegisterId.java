package dto.responses.tm.admin.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DeniedRegisterId {

    @JsonProperty("openNewTab")
    private Boolean openNewTab;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("slug")
    private String slug;
}
