package dto.requests.tm;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetTransportationsByTagRequest {

    private String tagCode;

    private String value;
}
