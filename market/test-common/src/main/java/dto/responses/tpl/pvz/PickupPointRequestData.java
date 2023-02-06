package dto.responses.tpl.pvz;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class PickupPointRequestData {
    Long id;
    Long pvzMarketId;
    String name;
    Long uid;
    Integer timeOffset;
    Integer storagePeriod;
}
