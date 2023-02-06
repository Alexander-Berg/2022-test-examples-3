package dto.responses.lom.admin.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Credentials {

    @JsonProperty("text")
    private String text;
}
