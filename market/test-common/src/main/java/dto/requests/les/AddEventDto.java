package dto.requests.les;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AddEventDto {
    private EventDto event;
    private String queue;
}
