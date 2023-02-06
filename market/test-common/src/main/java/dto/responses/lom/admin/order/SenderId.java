package dto.responses.lom.admin.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SenderId {

    @JsonProperty("openNewTab")
    private Boolean openNewTab;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("id")
    private String id;

    @JsonProperty("slug")
    private String slug;
}
