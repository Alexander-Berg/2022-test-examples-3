package dto.requests.wms;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChangeShipmentDateRequest {

    private String orderKey;
    private LocalDate shipDate;
}
