package dto.responses.idxapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Urls {

    @JsonProperty("datacamp")
    private String datacamp;

    @JsonProperty("index_trace")
    private String indexTrace;

    @JsonProperty("report")
    private Report report;

    @JsonProperty("front")
    private Front front;

    @JsonProperty("idxapi")
    private Idxapi idxapi;

    @JsonProperty("mbo")
    private Mbo mbo;
}
