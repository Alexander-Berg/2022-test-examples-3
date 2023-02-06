package dto.responses.lgw.message.update_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Order {

    @JsonProperty("cargoType")
    private int cargoType;

    @JsonProperty("orderId")
    private OrderId orderId;

    @JsonProperty("amountPrepaid")
    private int amountPrepaid;

    @JsonProperty("deliveryInterval")
    private String deliveryInterval;

    @JsonProperty("shipmentPointCode")
    private String shipmentPointCode;

    @JsonProperty("warehouseFrom")
    private WarehouseFrom warehouseFrom;

    @JsonProperty("total")
    private int total;

    @JsonProperty("tariff")
    private String tariff;

    @JsonProperty("shipmentDate")
    private String shipmentDate;

    @JsonProperty("deliveryDate")
    private String deliveryDate;

    @JsonProperty("cargoCost")
    private int cargoCost;

    @JsonProperty("assessedCost")
    private int assessedCost;

    @JsonProperty("deliveryType")
    private int deliveryType;

    @JsonProperty("services")
    private List<ServicesItem> services;

    @JsonProperty("warehouse")
    private Warehouse warehouse;

    @JsonProperty("korobyte")
    private Korobyte korobyte;

    @JsonProperty("places")
    private List<PlacesItem> places;

    @JsonProperty("sender")
    private Sender sender;

    @JsonProperty("locationTo")
    private LocationTo locationTo;

    @JsonProperty("recipient")
    private Recipient recipient;

    @JsonProperty("paymentMethod")
    private int paymentMethod;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("items")
    private List<ItemsItem> items;

    @JsonProperty("locationFrom")
    private LocationFrom locationFrom;

    @JsonProperty("deliveryCost")
    private int deliveryCost;
}
