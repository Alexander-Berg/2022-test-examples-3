package dto.responses.lom.admin.business_process;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BusinessProcessesResponse {

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("items")
    private List<BusinessProcess> items;
}
