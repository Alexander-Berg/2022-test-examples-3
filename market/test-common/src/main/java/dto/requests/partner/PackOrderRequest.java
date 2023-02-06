package dto.requests.partner;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PackOrderRequest {

    private List<PartnerBox> boxes;
}
