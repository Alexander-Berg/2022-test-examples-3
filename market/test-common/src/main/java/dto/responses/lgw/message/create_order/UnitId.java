package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UnitId {

    @JsonProperty("vendorId")
    private Integer vendorId;

    @JsonProperty("article")
    private String article;
}
