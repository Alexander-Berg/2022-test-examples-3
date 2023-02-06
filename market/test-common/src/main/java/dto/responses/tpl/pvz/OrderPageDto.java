package dto.responses.tpl.pvz;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class OrderPageDto {
    private PvzOrderDto order;
}
