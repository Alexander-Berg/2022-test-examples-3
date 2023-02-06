package dto.responses.tm.transportation_unit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RegistersItem {

    @JsonProperty("externalId")
    private Object externalId;

    @JsonProperty("documentId")
    private Object documentId;

    @JsonProperty("comment")
    private Object comment;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("partnerId")
    private Object partnerId;

    @JsonProperty("type")
    private String type;

    @JsonProperty("status")
    private String status;
}
