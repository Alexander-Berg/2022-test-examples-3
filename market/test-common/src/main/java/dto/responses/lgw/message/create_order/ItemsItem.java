package dto.responses.lgw.message.create_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ItemsItem {

    @JsonProperty("cargoType")
    private Integer cargoType;

    @JsonProperty("cargoTypes")
    private List<Integer> cargoTypes;

    @JsonProperty("boxCount")
    private Integer boxCount;

    @JsonProperty("unitOperationType")
    private String unitOperationType;

    @JsonProperty("price")
    private Integer price;

    @JsonProperty("name")
    private String name;

    @JsonProperty("count")
    private Integer count;

    @JsonProperty("unitId")
    private UnitId unitId;

    @JsonProperty("tax")
    private Tax tax;

    @JsonProperty("removableIfAbsent")
    private Boolean removableIfAbsent;

    @JsonProperty("korobyte")
    private Korobyte korobyte;

    @JsonProperty("article")
    private String article;
}
