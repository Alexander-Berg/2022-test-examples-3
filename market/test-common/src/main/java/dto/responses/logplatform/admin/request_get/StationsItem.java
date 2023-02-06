package dto.responses.logplatform.admin.request_get;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StationsItem {

    @JsonProperty("station_name")
    private String stationName;

    @JsonProperty("contact_name")
    private String contactName;

    @JsonProperty("address")
    private String address;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("station_id")
    private String stationId;

    @JsonProperty("description")
    private String description;

    @JsonProperty("operator_station_id")
    private String operatorStationId;
}
