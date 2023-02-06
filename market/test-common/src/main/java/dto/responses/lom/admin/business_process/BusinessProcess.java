package dto.responses.lom.admin.business_process;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BusinessProcess {

    @JsonProperty("values")
    private Values values;

    @JsonProperty("id")
    private Long id;
}
