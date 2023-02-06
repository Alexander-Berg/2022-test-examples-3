package dto.requests.tm;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RefreshTransportationRequest {

    private String jobName;
}
