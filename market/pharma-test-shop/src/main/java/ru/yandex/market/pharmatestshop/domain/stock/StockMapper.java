package ru.yandex.market.pharmatestshop.domain.stock;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import ru.yandex.market.pharmatestshop.domain.stock.item.SkuItem;


@Component
public class StockMapper {
    private final ru.yandex.market.pharmatestshop.domain.stock.Stock stock;

    public StockMapper(ru.yandex.market.pharmatestshop.domain.stock.Stock stock) {
        this.stock = stock;
    }

    public ru.yandex.market.pharmatestshop.domain.stock.Stock map(StockDto stockDto) {

        if (stockDto.skus == null) {
            throw new IllegalArgumentException("No stocks in request");
        }
        stock.skus = new ArrayList<>();
        for (var skuDto : stockDto.skus) {
            SkuItem skuItem = SkuItem.builder()
                    .type("FIT")
                    .count(10)
                    .updatedAt(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss")) +
                            OffsetDateTime.now(ZoneId.systemDefault()).getOffset())
                    .build();
            List<SkuItem> skuItemList = new ArrayList<>();
            skuItemList.add(skuItem);
            Sku sku = Sku.builder()
                    .sku(skuDto)
                    .warehouseId(String.valueOf(stockDto.warehouseId))
                    .items(skuItemList)
                    .build();

            stock.skus.add(sku);
            System.out.println(stock);
        }
        return stock;
    }
}
