package dto.responses.nesu;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServicesItem {

    @JsonProperty("code")
    private String code;

    @JsonProperty("cost")
    private Integer cost;

    @JsonProperty("name")
    private String name;

    @JsonProperty("enabledByDefault")
    private Boolean enabledByDefault;

    @JsonProperty("customerPay")
    private Boolean customerPay;
}
