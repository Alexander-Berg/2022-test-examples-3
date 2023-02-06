package dto.responses.inbounds;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ScInboundsAcceptResponse {

    @JsonProperty("externalId")
    private String externalId;

    @JsonProperty("id")
    private int id;

    @JsonProperty("informationListCode")
    private String informationListCode;

    @JsonProperty("type")
    private String type;

    @JsonProperty("status")
    private String status;

    @JsonProperty("info")
    private Info info;
}
