package dto.requests.partner;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PartnerBox {
    private Long fulfilmentId;
    private Integer weight;
    private Integer width;
    private Integer height;
    private Integer depth;
    private List<PartnerItem> items;
}
