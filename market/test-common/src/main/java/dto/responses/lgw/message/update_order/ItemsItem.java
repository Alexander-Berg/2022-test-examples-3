package dto.responses.lgw.message.update_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ItemsItem {

    @JsonProperty("cargoTypes")
    private List<Integer> cargoTypes;

    @JsonProperty("price")
    private int price;

    @JsonProperty("supplier")
    private Supplier supplier;

    @JsonProperty("name")
    private String name;

    @JsonProperty("count")
    private int count;

    @JsonProperty("taxes")
    private List<TaxesItem> taxes;

    @JsonProperty("unitId")
    private UnitId unitId;

    @JsonProperty("categoryName")
    private String categoryName;

    @JsonProperty("korobyte")
    private Korobyte korobyte;

    @JsonProperty("article")
    private String article;
}
