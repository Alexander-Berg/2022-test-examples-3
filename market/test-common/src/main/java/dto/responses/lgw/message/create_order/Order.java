package dto.responses.lgw.message.create_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Order {

    @JsonProperty("cargoType")
    private Integer cargoType;

    @JsonProperty("orderId")
    private OrderId orderId;

    @JsonProperty("amountPrepaid")
    private Integer amountPrepaid;

    @JsonProperty("documentData")
    private DocumentData documentData;

    @JsonProperty("warehouseFrom")
    private WarehouseFrom warehouseFrom;

    @JsonProperty("total")
    private Integer total;

    @JsonProperty("tariff")
    private String tariff;

    @JsonProperty("shipmentDate")
    private String shipmentDate;

    @JsonProperty("delivery")
    private Delivery delivery;

    @JsonProperty("cargoCost")
    private Integer cargoCost;

    @JsonProperty("assessedCost")
    private Integer assessedCost;

    @JsonProperty("deliveryType")
    private Integer deliveryType;

    @JsonProperty("externalId")
    private ExternalId externalId;

    @JsonProperty("services")
    private List<ServicesItem> services;

    @JsonProperty("warehouse")
    private Warehouse warehouse;

    @JsonProperty("korobyte")
    private Korobyte korobyte;

    @JsonProperty("places")
    private List<Object> places;

    @JsonProperty("pickupPointCode")
    private String pickupPointCode;

    @JsonProperty("sender")
    private Sender sender;

    @JsonProperty("locationTo")
    private LocationTo locationTo;

    @JsonProperty("maxAbsentItemsPricePercent")
    private Integer maxAbsentItemsPricePercent;

    @JsonProperty("recipient")
    private Recipient recipient;

    @JsonProperty("paymentMethod")
    private Integer paymentMethod;

    @JsonProperty("items")
    private List<ItemsItem> items;

    @JsonProperty("locationFrom")
    private LocationFrom locationFrom;

    @JsonProperty("deliveryCost")
    private Integer deliveryCost;
}
