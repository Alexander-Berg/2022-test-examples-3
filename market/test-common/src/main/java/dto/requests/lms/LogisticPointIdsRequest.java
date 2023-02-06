package dto.requests.lms;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LogisticPointIdsRequest {

    private List<Long> ids;
}
