package dto.responses.tm.admin.transportation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Item {

    @JsonProperty("values")
    private Values values;

    @JsonProperty("id")
    private int id;

    @JsonProperty("title")
    private String title;

}
