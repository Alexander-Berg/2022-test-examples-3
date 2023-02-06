package dto.responses.lgw.message.update_items_instances;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ItemsInstancesItem {

    @JsonProperty("instances")
    private List<InstancesItem> instances;

    @JsonProperty("unitId")
    private UnitId unitId;
}
