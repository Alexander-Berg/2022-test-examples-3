package dto.responses.idxapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Mbo {

    @JsonProperty("msku")
    private String msku;

    @JsonProperty("model")
    private String model;
}
