package dto.responses.idxapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PromosItem {

    @JsonProperty("sourceReference")
    private String sourceReference;

    @JsonProperty("promoState")
    private String promoState;

    @JsonProperty("promoKey")
    private String promoKey;

    @JsonProperty("promoType")
    private String promoType;
}
