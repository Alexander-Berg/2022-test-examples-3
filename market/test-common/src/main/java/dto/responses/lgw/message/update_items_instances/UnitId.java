package dto.responses.lgw.message.update_items_instances;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UnitId {

    @JsonProperty("vendorId")
    private Integer vendorId;

    @JsonProperty("article")
    private String article;
}
