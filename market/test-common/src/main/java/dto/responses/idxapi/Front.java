package dto.responses.idxapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Front {

    @JsonProperty("market")
    private String market;

    @JsonProperty("pokupka")
    private String pokupka;
}
