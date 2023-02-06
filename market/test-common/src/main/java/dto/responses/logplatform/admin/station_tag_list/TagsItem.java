package dto.responses.logplatform.admin.station_tag_list;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TagsItem {

    @JsonProperty("carriage_instant")
    private Long carriageInstant;

    @JsonProperty("comments")
    private String comments;

    @JsonProperty("dropoff_reservations")
    private List<DropoffReservationsItem> dropoffReservations;

    @JsonProperty("operator_id")
    private String operatorId;

    @JsonProperty("tag_name")
    private String tagName;

    @JsonProperty("pickup_interval")
    private String pickupInterval;

    @JsonProperty("object_id")
    private String objectId;

    @JsonProperty("parent_tag_id")
    private String parentTagId;

    @JsonProperty("tag_id")
    private String tagId;

    @JsonProperty("operator_shipment_tag")
    private OperatorShipmentTag operatorShipmentTag;

    @JsonProperty("return_station_id")
    private String returnStationId;

    @JsonProperty("class_name")
    private String className;

    @JsonProperty("status")
    private String status;

    @JsonProperty("patronymic")
    private String patronymic;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("operator_registration_id")
    private String operatorRegistrationId;

    @JsonProperty("time_margin_notify")
    private TimeMarginNotify timeMarginNotify;

    @JsonProperty("days_planned_count")
    private int daysPlannedCount;

    @JsonProperty("pickup_volume_tag")
    private PickupVolumeTag pickupVolumeTag;

    @JsonProperty("dropoff_promise")
    private List<DropoffPromiseItem> dropoffPromise;

    @JsonProperty("duration")
    private String duration;
}
