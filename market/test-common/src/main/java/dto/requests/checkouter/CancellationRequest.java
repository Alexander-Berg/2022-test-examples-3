package dto.requests.checkouter;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CancellationRequest {
    private String notes;
    private String substatus;
    private String substatusText;
}
