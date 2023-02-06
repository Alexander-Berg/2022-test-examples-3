package dto.responses.lgw.message.update_order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TaxesItem {

    @JsonProperty("type")
    private String type;

    @JsonProperty("value")
    private int value;
}
