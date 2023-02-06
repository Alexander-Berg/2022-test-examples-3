package dto.responses.tracker;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import ru.yandex.market.delivery.tracker.domain.entity.DeliveryTrackCheckpoint;

@Data
public class TracksItem {

    @JsonProperty("trackCode")
    private String trackCode;

    @JsonProperty("deliveryServiceId")
    private Integer deliveryServiceId;

    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("deliveryTrackStatus")
    private String deliveryTrackStatus;

    @JsonProperty("deliveryType")
    private String deliveryType;

    @JsonProperty("checkpoints")
    private List<DeliveryTrackCheckpoint> checkpoints;

    @JsonProperty("entityId")
    private String entityId;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("estimatedArrivalDate")
    private Object estimatedArrivalDate;

    @JsonProperty("globalOrder")
    private Boolean globalOrder;
}
