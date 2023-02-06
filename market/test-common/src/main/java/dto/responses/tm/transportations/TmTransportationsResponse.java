package dto.responses.tm.transportations;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TmTransportationsResponse {

    @JsonProperty("inbound")
    private Inbound inbound;

    @JsonProperty("outbound")
    private Outbound outbound;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("movement")
    private Movement movement;

    @JsonProperty("tags")
    private List<Object> tags;
}
