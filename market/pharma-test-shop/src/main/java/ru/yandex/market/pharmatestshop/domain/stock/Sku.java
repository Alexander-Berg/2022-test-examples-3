package ru.yandex.market.pharmatestshop.domain.stock;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ru.yandex.market.pharmatestshop.domain.stock.item.SkuItem;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sku {
    String sku;// Один из списка в stocksDto.skus
    String warehouseId;// long
    List<SkuItem> items;
}
