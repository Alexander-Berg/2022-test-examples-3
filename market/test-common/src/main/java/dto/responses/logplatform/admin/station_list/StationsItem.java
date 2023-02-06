package dto.responses.logplatform.admin.station_list;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StationsItem {

    @JsonProperty("station_name")
    private String stationName;

    @JsonProperty("location_ll")
    private LocationLl locationLl;

    @JsonProperty("requests_count")
    private int requestsCount;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("deprecated")
    private boolean deprecated;

    @JsonProperty("station_id")
    private String stationId;

    @JsonProperty("enabled_in_platform")
    private boolean enabledInPlatform;

    @JsonProperty("physical_limits")
    private PhysicalLimits physicalLimits;

    @JsonProperty("station_full_name")
    private String stationFullName;

    @JsonProperty("timetable_output")
    private TimetableOutput timetableOutput;

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("capacity")
    private int capacity;

    @JsonProperty("revision")
    private int revision;

    @JsonProperty("need_synchronization")
    private boolean needSynchronization;

    @JsonProperty("orders_count")
    private int ordersCount;

    @JsonProperty("requests_count_by_day")
    private Map<LocalDate, Integer> requestsCountByDay;

    @JsonProperty("location_details")
    private LocationDetails locationDetails;

    @JsonProperty("request_ids")
    private Map<LocalDate, List<String>> requestIds;

    @JsonProperty("location")
    private List<Double> location;

    @JsonProperty("orders")
    private List<OrdersItem> orders;

    @JsonProperty("operator_station_id")
    private String operatorStationId;

    @JsonProperty("timetable_input")
    private TimetableInput timetableInput;

    @JsonProperty("excluded_input_intervals")
    private List<Object> excludedInputIntervals;
}
