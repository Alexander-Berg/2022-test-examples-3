package dto.responses.lgw.message.create_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Delivery {

    @JsonProperty("deliveryId")
    private DeliveryId deliveryId;

    @JsonProperty("docs")
    private List<DocsItem> docs;

    @JsonProperty("contract")
    private String contract;

    @JsonProperty("name")
    private String name;

    @JsonProperty("phones")
    private List<PhonesItem> phones;

    @JsonProperty("priority")
    private Integer priority;
}
