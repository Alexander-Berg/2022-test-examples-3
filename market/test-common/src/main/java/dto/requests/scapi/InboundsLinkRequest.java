package dto.requests.scapi;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InboundsLinkRequest {

    private String sortableId;

    private String type;
}
