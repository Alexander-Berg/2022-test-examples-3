package dto.responses.tm.transportation_unit;

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
    private Object name;

    @JsonProperty("inn")
    private String inn;

    @JsonProperty("id")
    private Object id;

    @JsonProperty("type")
    private Object type;

    @JsonProperty("legalAddress")
    private String legalAddress;

    @JsonProperty("marketId")
    private Integer marketId;
}
