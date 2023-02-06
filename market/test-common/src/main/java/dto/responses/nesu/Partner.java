package dto.responses.nesu;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Partner {

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("partnerType")
    private String partnerType;

    @JsonProperty("logoUrl")
    private String logoUrl;
}
