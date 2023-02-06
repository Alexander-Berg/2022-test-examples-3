package dto.responses.tm.transportations;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Partner {

    @JsonProperty("ogrn")
    private String ogrn;

    @JsonProperty("legalName")
    private String legalName;

    @JsonProperty("legalType")
    private String legalType;

    @JsonProperty("name")
    private String name;

    @JsonProperty("inn")
    private String inn;

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("legalAddress")
    private String legalAddress;

    @JsonProperty("marketId")
    private Integer marketId;
}
