package dto.requests.tpl.pvz;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderStatusUpdateDto {
    String id;
    String status;
}
