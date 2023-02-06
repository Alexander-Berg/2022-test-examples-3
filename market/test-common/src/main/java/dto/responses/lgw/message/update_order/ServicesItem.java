package dto.responses.lgw.message.update_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ServicesItem {

    @JsonProperty("code")
    private String code;

    @JsonProperty("cost")
    private int cost;

    @JsonProperty("isOptional")
    private boolean isOptional;

    @JsonProperty("taxes")
    private List<TaxesItem> taxes;
}
