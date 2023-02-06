package dto.requests.les;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

import ru.yandex.market.logistics.les.base.EventPayload;

@AllArgsConstructor
public class EventDto {
    @JsonProperty("source")
    String source;

    @JsonProperty("event_id")
    String eventId;

    @JsonProperty("timestamp")
    Long timestamp;

    @JsonProperty("event_type")
    String eventType;

    @JsonProperty("payload")
    EventPayload payload;

    @JsonProperty("description")
    String description;
}
