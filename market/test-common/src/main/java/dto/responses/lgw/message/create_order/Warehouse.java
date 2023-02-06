package dto.responses.lgw.message.create_order;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Warehouse {

    @JsonProperty("schedule")
    private List<ScheduleItem> schedule;

    @JsonProperty("incorporation")
    private String incorporation;

    @JsonProperty("address")
    private Address address;

    @JsonProperty("warehouseId")
    private WarehouseId warehouseId;

    @JsonProperty("instruction")
    private String instruction;

    @JsonProperty("contact")
    private Contact contact;

    @JsonProperty("phones")
    private List<PhonesItem> phones;
}
