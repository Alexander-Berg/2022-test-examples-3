package dto.responses.lom.admin.order;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Values {

    @JsonProperty("credentials")
    private Credentials credentials;

    @JsonProperty("created")
    private String created;

    @JsonProperty("deliveryDateMax")
    private String deliveryDateMax;

    @JsonProperty("deliveryType")
    private String deliveryType;

    @JsonProperty("platformClient")
    private String platformClient;

    @JsonProperty("senderId")
    private SenderId senderId;

    @JsonProperty("deliveryDateMin")
    private String deliveryDateMin;

    @JsonProperty("recipient")
    private Recipient recipient;

    @JsonProperty("recipientAddress")
    private RecipientAddress recipientAddress;

    @JsonProperty("barcode")
    private String barcode;

    @JsonProperty("updated")
    private String updated;

    @JsonProperty("actions")
    private Actions actions;

    @JsonProperty("status")
    private String status;

    @JsonProperty("orderTags")
    private OrderTags tags;
}
