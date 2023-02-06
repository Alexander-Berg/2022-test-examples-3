package dto.requests.lms;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateScheduleDayRequest {

    private String scheduleType;
    private Long partnerRelationId;
    private Integer weekDay;
    private String timeInterval;
}
