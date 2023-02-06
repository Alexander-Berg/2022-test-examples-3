package dto.responses.logplatform.admin.station_tag_list;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ObjectsItem {

    @JsonProperty("object_id")
    private String objectId;

    @JsonProperty("tags")
    private List<TagsItem> tags;
}
