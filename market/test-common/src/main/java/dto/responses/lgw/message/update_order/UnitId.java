package dto.responses.lgw.message.update_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UnitId {

    @JsonProperty("vendorId")
    private int vendorId;

    @JsonProperty("article")
    private String article;
}
