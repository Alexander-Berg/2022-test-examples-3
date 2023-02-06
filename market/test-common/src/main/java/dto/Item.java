package dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
public class Item {

    private long feedId;
    private String offerId;
    private float buyerPrice;
    private int count;
    private Long shopId;
    private Integer warehouseId;
    private String showInfo;
    private String wareMd5;

    private Long weight;
    private Long width;
    private Long height;
    private Long depth;

}
