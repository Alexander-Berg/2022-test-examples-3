package ru.yandex.market.pharmatestshop.domain.cart.item;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {

    private Long feedId;
    private String offerId;
    private String feedCategoryId;
    private String offerName;
    private Long subsidy;
    private Integer count;
    private String params;
    private Long fulfilmentShopId;
    private String sku;
    private Long warehouseId;
    private String partnerWarehouseId;
    private List<String> guids;


}
