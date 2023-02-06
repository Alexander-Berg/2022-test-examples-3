package dto.responses.lgw.message.create_order;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DocumentData {

    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private Integer version;
}
