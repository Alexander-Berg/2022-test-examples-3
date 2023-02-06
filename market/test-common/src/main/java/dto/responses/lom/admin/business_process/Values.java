package dto.responses.lom.admin.business_process;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Values {

    @JsonProperty("created")
    private String created;

    @JsonProperty("statuses")
    private List<String> statuses;

    @JsonProperty("queueTypes")
    private List<String> queueTypes;

    @JsonProperty("relatedEntities")
    private List<RelatedEntitiesItem> relatedEntities;

    @JsonProperty("updated")
    private String updated;
}
