package dto.responses.lom.admin.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RecipientAddress {

    @JsonProperty("text")
    private String text;
}
